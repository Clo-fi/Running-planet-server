package clofi.runningplanet.crew.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import clofi.runningplanet.common.exception.ConflictException;
import clofi.runningplanet.common.exception.NotFoundException;
import clofi.runningplanet.crew.domain.Crew;
import clofi.runningplanet.crew.domain.CrewApplication;
import clofi.runningplanet.crew.domain.CrewMember;
import clofi.runningplanet.crew.domain.Tag;
import clofi.runningplanet.crew.dto.CrewLeaderDto;
import clofi.runningplanet.crew.dto.request.ApplyCrewReqDto;
import clofi.runningplanet.crew.dto.request.CreateCrewReqDto;
import clofi.runningplanet.crew.dto.request.ProceedApplyReqDto;
import clofi.runningplanet.crew.dto.response.ApplyCrewResDto;
import clofi.runningplanet.crew.dto.response.ApprovalMemberResDto;
import clofi.runningplanet.crew.dto.response.FindAllCrewResDto;
import clofi.runningplanet.crew.dto.response.FindCrewResDto;
import clofi.runningplanet.crew.dto.response.GetApplyCrewResDto;
import clofi.runningplanet.crew.repository.CrewApplicationRepository;
import clofi.runningplanet.crew.repository.CrewMemberRepository;
import clofi.runningplanet.crew.repository.CrewRepository;
import clofi.runningplanet.crew.repository.TagRepository;
import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CrewService {

	private final CrewRepository crewRepository;
	private final TagRepository tagRepository;
	private final MemberRepository memberRepository;
	private final CrewApplicationRepository crewApplicationRepository;
	private final CrewMemberRepository crewMemberRepository;

	@Transactional
	public Long createCrew(CreateCrewReqDto reqDto, Long memberId) {
		Member findMember = getMemberByMemberId(memberId);
		checkSubscribedCrew(memberId);

		Crew savedCrew = createAndSaveCrew(reqDto, memberId);
		saveTags(reqDto.tags(), savedCrew);

		createAndSaveCrewMember(savedCrew, findMember);
		return savedCrew.getId();
	}

	@Transactional(readOnly = true)
	public List<FindAllCrewResDto> findAllCrew() {
		List<Crew> crewList = crewRepository.findAll();

		return crewList.stream()
			.map(this::convertToFindAllCrewResDto)
			.toList();
	}

	@Transactional(readOnly = true)
	public FindCrewResDto findCrew(Long crewId) {
		Crew findCrew = getCrewByCrewId(crewId);

		List<String> tags = findTagsToStrings(findCrew.getId());
		CrewLeaderDto crewLeader = convertCrewLeaderDto(findCrew.getLeaderId());

		return FindCrewResDto.of(findCrew, crewLeader, tags);
	}

	@Transactional
	public ApplyCrewResDto applyCrew(ApplyCrewReqDto reqDto, Long crewId, Long memberId) {

		Member findMember = getMemberByMemberId(memberId);

		validateMemberNotInCrew(findMember.getId());
		validateDuplicateApply(crewId, findMember.getId());

		Crew findCrew = getCrewByCrewId(crewId);

		findCrew.checkRunScore(findMember.getRunScore());

		CrewApplication crewApplication = reqDto.toEntity(findCrew, findMember);
		crewApplicationRepository.save(crewApplication);
		return new ApplyCrewResDto(crewId, findMember.getId(), true);
	}

	@Transactional(readOnly = true)
	public ApprovalMemberResDto getApplyCrewList(Long crewId, Long memberId) {
		validateLeaderPrivilege(crewId, memberId);
		checkCrewExistById(crewId);

		List<CrewApplication> crewApplicationList = crewApplicationRepository.findAllByCrewId(crewId);
		List<GetApplyCrewResDto> getApplyCrewResDtos = makeGetApplyDtoList(crewApplicationList);
		return new ApprovalMemberResDto(getApplyCrewResDtos);
	}

	@Transactional
	public void proceedApplyCrew(ProceedApplyReqDto reqDto, Long crewId, Long memberId) {
		Crew findCrew = getCrewByCrewId(crewId);
		validateLeaderPrivilege(crewId, memberId);

		CrewApplication crewApplication = getCrewApplicationByCrewIdAndMemberId(crewId, reqDto.memberId());
		validateMemberNotInCrew(reqDto.memberId());

		if (reqDto.isApproval()) {
			processApproval(reqDto, findCrew, crewApplication);
		} else {
			crewApplication.reject();
		}
	}

	private void processApproval(ProceedApplyReqDto reqDto, Crew findCrew, CrewApplication crewApplication) {
		validateCrewMemberLimit(findCrew);

		Member applyMember = getMemberByMemberId(reqDto.memberId());
		crewApplication.approve();
		CrewMember crewMember = CrewMember.createMember(findCrew, applyMember);
		crewMemberRepository.save(crewMember);
	}

	private void validateLeaderPrivilege(Long crewId, Long memberId) {
		CrewMember findCrewMember = getCrewMemberByMemberId(memberId);
		findCrewMember.validateMembership(crewId);
		findCrewMember.checkLeaderPrivilege();
	}

	private void validateCrewMemberLimit(Crew crew) {
		int memberCnt = crewMemberRepository.countByCrewId(crew.getId());
		if (crew.checkReachedMemberLimit(memberCnt)) {
			throw new ConflictException("최대 인원수를 초과해서 크루원을 받을 수 없습니다.");
		}
	}

	private CrewApplication getCrewApplicationByCrewIdAndMemberId(Long crewId, Long memberId) {
		return crewApplicationRepository.findByCrewIdAndMemberId(crewId, memberId).orElseThrow(
			() -> new NotFoundException("크루에 신청한 사용자가 아닙니다.")
		);
	}

	private CrewMember getCrewMemberByMemberId(Long memberId) {
		return crewMemberRepository.findByMemberId(memberId).orElseThrow(
			() -> new NotFoundException("크루 소속이 아닙니다.")
		);
	}

	private void checkCrewExistById(Long crewId) {
		if (!crewRepository.existsById(crewId)) {
			throw new NotFoundException("크루가 존재하지 않습니다.");
		}
	}

	private List<GetApplyCrewResDto> makeGetApplyDtoList(List<CrewApplication> crewApplicationList) {
		return crewApplicationList.stream()
			.map(GetApplyCrewResDto::new)
			.toList();
	}

	private Crew getCrewByCrewId(Long crewId) {
		return crewRepository.findById(crewId).orElseThrow(
			() -> new NotFoundException("크루를 찾을 수 없습니다.")
		);
	}

	private void validateDuplicateApply(Long crewId, Long memberId) {
		Optional<CrewApplication> crewApplicationOpt = crewApplicationRepository.findByCrewIdAndMemberId(crewId,
			memberId);
		crewApplicationOpt.ifPresent(CrewApplication::checkDuplicateApply);
	}

	private void validateMemberNotInCrew(Long memberId) {
		crewMemberRepository.findByMemberId(memberId).ifPresent(crewMember -> {
			throw new ConflictException("이미 크루에 소속되어 있습니다.");
		});
	}

	private void createAndSaveCrewMember(Crew savedCrew, Member findMember) {
		CrewMember crewLeader = CrewMember.createLeader(savedCrew, findMember);
		crewMemberRepository.save(crewLeader);
	}

	private void checkSubscribedCrew(Long memberId) {
		if (crewMemberRepository.existsByMemberId(memberId)) {
			throw new ConflictException("이미 소속된 크루가 존재합니다.");
		}
	}

	private Member getMemberByMemberId(Long memberId) {
		return memberRepository.findById(memberId).orElseThrow(
			() -> new NotFoundException("존재하지 않는 회원입니다.")
		);
	}

	private Crew createAndSaveCrew(CreateCrewReqDto reqDto, Long leaderId) {
		Crew crew = reqDto.toEntity(leaderId);
		return crewRepository.save(crew);
	}

	private void saveTags(List<String> tagNames, Crew savedCrew) {
		if (!tagNames.isEmpty()) {
			List<Tag> tagList = tagNames.stream()
				.map(t -> new Tag(savedCrew, t))
				.toList();
			tagRepository.saveAll(tagList);
		}
	}

	private FindAllCrewResDto convertToFindAllCrewResDto(Crew crew) {
		List<String> tags = findTagsToStrings(crew.getId());
		CrewLeaderDto crewLeaderDto = convertCrewLeaderDto(crew.getLeaderId());
		return FindAllCrewResDto.of(crew, tags, crewLeaderDto);
	}

	private CrewLeaderDto convertCrewLeaderDto(Long leaderId) {
		Member crewLeader = getMemberByMemberId(leaderId);
		return new CrewLeaderDto(leaderId, crewLeader.getNickname());
	}

	private List<String> findTagsToStrings(Long crewId) {
		return tagRepository.findAllByCrewId(crewId).stream()
			.map(Tag::getContent)
			.toList();
	}
}
