package clofi.runningplanet.crew.service;

import static clofi.runningplanet.crew.domain.ApprovalType.*;
import static clofi.runningplanet.crew.domain.Category.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import clofi.runningplanet.common.exception.ConflictException;
import clofi.runningplanet.common.exception.NotFoundException;
import clofi.runningplanet.common.service.S3StorageManagerUseCase;
import clofi.runningplanet.crew.domain.Approval;
import clofi.runningplanet.crew.domain.Crew;
import clofi.runningplanet.crew.domain.CrewApplication;
import clofi.runningplanet.crew.domain.CrewImage;
import clofi.runningplanet.crew.domain.CrewMember;
import clofi.runningplanet.crew.domain.Role;
import clofi.runningplanet.crew.domain.Tag;
import clofi.runningplanet.crew.dto.CrewLeaderDto;
import clofi.runningplanet.crew.dto.RuleDto;
import clofi.runningplanet.crew.dto.request.ApplyCrewReqDto;
import clofi.runningplanet.crew.dto.request.CreateCrewReqDto;
import clofi.runningplanet.crew.dto.request.ProceedApplyReqDto;
import clofi.runningplanet.crew.dto.request.UpdateCrewReqDto;
import clofi.runningplanet.crew.dto.response.ApplyCrewResDto;
import clofi.runningplanet.crew.dto.response.ApprovalMemberResDto;
import clofi.runningplanet.crew.dto.response.FindAllCrewResDto;
import clofi.runningplanet.crew.dto.response.FindCrewResDto;
import clofi.runningplanet.crew.dto.response.GetApplyCrewResDto;
import clofi.runningplanet.crew.repository.CrewApplicationRepository;
import clofi.runningplanet.crew.repository.CrewImageRepository;
import clofi.runningplanet.crew.repository.CrewMemberRepository;
import clofi.runningplanet.crew.repository.CrewRepository;
import clofi.runningplanet.crew.repository.TagRepository;
import clofi.runningplanet.member.domain.Gender;
import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class CrewServiceTest {

	@Mock
	private CrewRepository crewRepository;

	@Mock
	private TagRepository tagRepository;

	@Mock
	private CrewMemberRepository crewMemberRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private CrewApplicationRepository crewApplicationRepository;

	@Mock
	private CrewImageRepository crewImageRepository;

	@Mock
	private S3StorageManagerUseCase storageManagerUseCase;

	@InjectMocks
	private CrewService crewService;

	@DisplayName("크루 생성 성공")
	@Test
	void successCreateCrew() {
		// given
		Long leaderId = 1L;

		CreateCrewReqDto reqDto = getCreateCrewReqDto();

		Crew crew = createCrew();
		Member leader = createLeader();
		MockMultipartFile imageFile = createImage();

		given(crewRepository.save(any(Crew.class))).willReturn(crew);
		given(tagRepository.saveAll(anyList())).willReturn(Collections.emptyList());
		given(crewMemberRepository.save(any(CrewMember.class))).willReturn(
			new CrewMember(1L, crew, leader, Role.LEADER));
		given(memberRepository.findById(anyLong())).willReturn(Optional.of(leader));
		given(storageManagerUseCase.uploadImage(any(MockMultipartFile.class)))
			.willReturn("https://imagepath.com");
		given(crewImageRepository.save(any(CrewImage.class)))
			.willReturn(null);
		given(crewMemberRepository.existsByMemberId(anyLong()))
			.willReturn(false);

		// when
		Long result = crewService.createCrew(reqDto, imageFile, leaderId);

		// then
		assertThat(result).isEqualTo(1L);
	}

	@DisplayName("크루 생성시 등록되지 않은 사용자가 들어올 경우 예외 발생")
	@Test
	void failCreateCrewByNotFoundMember() {
		//given
		CreateCrewReqDto reqDto = getCreateCrewReqDto();

		Member leader = createLeader();
		MockMultipartFile imageFile = createImage();

		given(memberRepository.findById(anyLong())).willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.createCrew(reqDto, imageFile, leader.getId()))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("크루에 속해있는 사용자가 크루 생성시 예외 발생")
	@Test
	void test() {
		//given
		CreateCrewReqDto reqDto = getCreateCrewReqDto();

		Member leader = createLeader();
		MockMultipartFile imageFile = createImage();

		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.of(leader));
		given(crewMemberRepository.existsByMemberId(anyLong()))
			.willReturn(true);

		//when
		//then
		assertThatThrownBy(() -> crewService.createCrew(reqDto, imageFile, leader.getId()))
			.isInstanceOf(ConflictException.class);
	}

	@DisplayName("크루 목록 조회 성공")
	@Test
	void successFindAllCrew() {
		//given
		Crew crew1 = createCrew();
		Crew crew2 = createAutoCrew();

		Member leader = createLeader();
		Member leader2 = createMember();

		given(crewRepository.findAll())
			.willReturn(List.of(crew1, crew2));

		given(tagRepository.findAllByCrewId(anyLong()))
			.willReturn(List.of(
				new Tag(1L, crew1, "성실")
			))
			.willReturn(List.of(
				new Tag(2L, crew2, "최고")
			));
		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.of(leader))
			.willReturn(Optional.of(leader2));

		//when
		List<FindAllCrewResDto> result = crewService.findAllCrew();

		//then
		final FindAllCrewResDto firstFindAllCrewResDto = FindAllCrewResDto.of(crew1, List.of("성실"),
			new CrewLeaderDto(1L, "크루장"));

		final FindAllCrewResDto secondFindAllCrewResDto = FindAllCrewResDto.of(crew2, List.of("최고"),
			new CrewLeaderDto(2L, "사용자"));

		final List<FindAllCrewResDto> expect = List.of(firstFindAllCrewResDto, secondFindAllCrewResDto);

		assertThat(result).isEqualTo(expect);
	}

	@DisplayName("아무 크루도 없을 시 빈 리스트 반환")
	@Test
	void successEmptyCrew() {
		//given
		given(crewRepository.findAll())
			.willReturn(List.of());

		//when
		List<FindAllCrewResDto> result = crewService.findAllCrew();

		//then
		assertThat(result).isEmpty();
	}

	@DisplayName("크루장이 실제 사용자가 아닌 경우 예외 발생")
	@Test
	void failEmptyCrew() {
		//given
		Crew crew = createCrew();

		given(crewRepository.findAll())
			.willReturn(List.of(crew));

		given(tagRepository.findAllByCrewId(anyLong()))
			.willReturn(List.of(
				new Tag(1L, crew, "성실")
			));
		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.findAllCrew())
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("크루 상세 조회 성공")
	@Test
	void successFindCrew() {
		//given
		Long crewId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.of(crew));

		given(tagRepository.findAllByCrewId(anyLong()))
			.willReturn(List.of(
				new Tag(1L, null, "성실")
			));

		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.of(leader));

		//when
		FindCrewResDto result = crewService.findCrew(crewId);

		//then
		final FindCrewResDto findCrewResDto = FindCrewResDto.of(crew, new CrewLeaderDto(1L, "크루장"), List.of("성실"));

		assertThat(result).isEqualTo(findCrewResDto);
	}

	@DisplayName("상세 조회한 크루가 없는 경우 예외 발생")
	@Test
	void failFindCrewByNotFoundCrew() {
		//given
		Long crewId = 10L;

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.findCrew(crewId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("상세 조회한 크루장이 실제 사용자가 아닌 경우 예외 발생")
	@Test
	void failFindCrewByNotFoundLeader() {
		//given
		Long crewId = 1L;

		Crew crew = createCrew();

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.of(crew));

		given(tagRepository.findAllByCrewId(anyLong()))
			.willReturn(List.of(
				new Tag(1L, null, "성실")
			));

		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.findCrew(crewId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("크루 신청 성공 테스트 코드")
	@Test
	void successApplyCrew() {
		//given
		ApplyCrewReqDto reqDto = getApplyCrewReqDto();
		Long crewId = 1L;
		Long memberId = 2L;

		Crew crew = createCrew();
		Member member = createMember();

		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.of(member));
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.empty());
		given(crewApplicationRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.empty());
		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.of(crew));
		given(crewApplicationRepository.save(any(CrewApplication.class)))
			.willReturn(any());

		//when
		ApplyCrewResDto result = crewService.applyCrew(reqDto, crewId, memberId);

		//then
		ApplyCrewResDto applyCrewResDto = new ApplyCrewResDto(crewId, memberId, true);

		assertThat(result).isEqualTo(applyCrewResDto);
	}

	@DisplayName("크루 신청한 사용자가 가입된 사용자가 아닌 경우 예외 발생")
	@Test
	void failApplyCrewByNotFoundMember() {
		//given
		ApplyCrewReqDto reqDto = getApplyCrewReqDto();
		Long crewId = 1L;
		Long memberId = 1L;

		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.applyCrew(reqDto, crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("이미 크루가 있는 사용자가 크루에 다시 신청할 경우 예외 발생")
	@Test
	void failApplyCrewByExistCrew() {
		//given
		ApplyCrewReqDto reqDto = getApplyCrewReqDto();
		Long crewId = 1L;
		Long memberId = 2L;

		Member member = createMember();

		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.of(member));
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(new CrewMember(null, null, null, null)));

		//when
		//then
		assertThatThrownBy(() -> crewService.applyCrew(reqDto, crewId, memberId))
			.isInstanceOf(ConflictException.class);
	}

	@DisplayName("가입 신청한 크루가 없는 경우 예외 처리")
	@Test
	void failApplyCrewByNotFoundCrew() {
		//given
		ApplyCrewReqDto reqDto = getApplyCrewReqDto();
		Long crewId = 1L;
		Long memberId = 2L;

		Member member = createMember();

		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.of(member));
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.empty());
		given(crewApplicationRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.empty());
		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.applyCrew(reqDto, crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("크루 신청 목록 조회 성공")
	@Test
	void successGetApplyList() {
		//given
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();
		CrewMember crewMember = new CrewMember(1L, crew, leader, Role.LEADER);

		Member member1 = Member.builder()
			.id(2L)
			.nickname("닉네임1")
			.age(30)
			.gender(Gender.MALE)
			.profileImg("https://image-url1.com")
			.avgDistance(50)
			.totalDistance(2000)
			.runScore(80)
			.build();
		Member member2 = Member.builder()
			.id(3L)
			.nickname("닉네임2")
			.age(15)
			.gender(Gender.FEMALE)
			.profileImg("https://image-url2.com")
			.avgDistance(5)
			.totalDistance(20)
			.runScore(70)
			.build();

		CrewApplication crewApplication1 = new CrewApplication(1L, "크루 신청글", Approval.PENDING, crew, member1);
		CrewApplication crewApplication2 = new CrewApplication(2L, "크루 신청글", Approval.PENDING, crew, member2);

		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewMember));
		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewApplicationRepository.findAllByCrewId(anyLong()))
			.willReturn(List.of(crewApplication1, crewApplication2));

		//when
		ApprovalMemberResDto result = crewService.getApplyCrewList(crewId, memberId);

		//then
		GetApplyCrewResDto getApplyCrewResDto1 = new GetApplyCrewResDto(2L, "닉네임1", "크루 신청글", 80, Gender.MALE, 30,
			Approval.PENDING);
		GetApplyCrewResDto getApplyCrewResDto2 = new GetApplyCrewResDto(3L, "닉네임2", "크루 신청글", 70, Gender.FEMALE, 15,
			Approval.PENDING);
		ApprovalMemberResDto approvalMemberResDto = new ApprovalMemberResDto(
			List.of(getApplyCrewResDto1, getApplyCrewResDto2));

		assertThat(result).isEqualTo(approvalMemberResDto);
	}

	@DisplayName("크루에 신청한 사람이 없는 경우 빈 리스트 반환")
	@Test
	void successGetApplyEmptyList() {
		//given
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();
		CrewMember crewMember = new CrewMember(1L, crew, leader, Role.LEADER);

		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewMember));
		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewApplicationRepository.findAllByCrewId(anyLong()))
			.willReturn(Collections.emptyList());

		//when
		ApprovalMemberResDto result = crewService.getApplyCrewList(crewId, memberId);

		//then
		assertThat(result).isEqualTo(new ApprovalMemberResDto(Collections.emptyList()));
	}

	@DisplayName("인증된 사용자가 아닌 경우 예외 발생")
	@Test
	void failGetApplyListByNotFoundMember() {
		//given
		Long crewId = 1L;
		Long memberId = 1L;

		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.getApplyCrewList(crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("확인하려는 크루가 없는 경우 예외 발생")
	@Test
	void failGetApplyListByNotFoundCrew() {
		//given
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();
		CrewMember crewMember = new CrewMember(1L, crew, leader, Role.LEADER);

		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewMember));
		given(crewRepository.existsById(anyLong()))
			.willReturn(false);

		//when
		//then
		assertThatThrownBy(() -> crewService.getApplyCrewList(crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("크루 가입 승인 성공")
	@Test
	void successApproveCrew() {
		//given
		ProceedApplyReqDto reqDto = new ProceedApplyReqDto(2L, true);
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();
		Member member = createMember();

		CrewMember crewMember = new CrewMember(1L, crew, leader, Role.LEADER);
		CrewApplication crewApplication = new CrewApplication(1L, "크루 신청글", Approval.PENDING, crew, member);

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.of(crew));
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewMember))
			.willReturn(Optional.empty());
		given(crewApplicationRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.of(crewApplication));
		given(crewMemberRepository.countByCrewId(anyLong()))
			.willReturn(1);
		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.of(member));
		given(crewMemberRepository.save(any(CrewMember.class)))
			.willReturn(crewMember);

		//when
		//then
		assertDoesNotThrow(() -> crewService.proceedApplyCrew(reqDto, crewId, memberId));
	}

	@DisplayName("크루 가입 거절 성공")
	@Test
	void successRejectCrew() {
		//given
		ProceedApplyReqDto reqDto = new ProceedApplyReqDto(2L, false);
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();
		Member member = createMember();

		CrewMember crewMember = new CrewMember(1L, crew, leader, Role.LEADER);
		CrewApplication crewApplication = new CrewApplication(1L, "크루 신청글", Approval.PENDING, crew, member);

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.of(crew));
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewMember))
			.willReturn(Optional.empty());
		given(crewApplicationRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.of(crewApplication));

		//when
		//then
		assertDoesNotThrow(() -> crewService.proceedApplyCrew(reqDto, crewId, memberId));
	}

	@DisplayName("가입 승인/거절 하려는 크루가 없는 경우 예외 발생")
	@Test
	void failApproveCrewByNotFoundCrew() {
		//given
		ProceedApplyReqDto reqDto = new ProceedApplyReqDto(2L, false);
		Long crewId = 1L;
		Long memberId = 1L;

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.proceedApplyCrew(reqDto, crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("크루 소속이 아닌 경우 크루 가입 승인/거절 요청 시 예외 발생")
	@Test
	void failApproveCrewByNotInCrew() {
		//given
		ProceedApplyReqDto reqDto = new ProceedApplyReqDto(2L, false);
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = createCrew();

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.of(crew));
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.proceedApplyCrew(reqDto, crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("크루 가입 신청하지 않은 사용자를 승인/거절할 경우 예외 발생")
	@Test
	void failApproveCrewByNotApplyMember() {
		//given
		ProceedApplyReqDto reqDto = new ProceedApplyReqDto(2L, true);
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();

		CrewMember crewMember = new CrewMember(1L, crew, leader, Role.LEADER);

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.of(crew));
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewMember));
		given(crewApplicationRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.proceedApplyCrew(reqDto, crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("이미 크루가 있는 사용자를 승인/거절할 경우 예외 발생")
	@Test
	void failApproveCrewByInCrew() {
		//given
		ProceedApplyReqDto reqDto = new ProceedApplyReqDto(2L, true);
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();
		Member member = createMember();

		CrewMember crewMember = new CrewMember(1L, crew, leader, Role.LEADER);
		CrewApplication crewApplication = new CrewApplication(1L, "크루 신청글", Approval.PENDING, crew, member);

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.of(crew));
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewMember))
			.willReturn(Optional.of(CrewMember.createMember(crew, member)));
		given(crewApplicationRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.of(crewApplication));

		//when
		//then
		assertThatThrownBy(() -> crewService.proceedApplyCrew(reqDto, crewId, memberId))
			.isInstanceOf(ConflictException.class);
	}

	@DisplayName("회원이 아닌 사용자를 승인할 경우 예외 발생")
	@Test
	void failApproveCrewByNotFoundMember() {
		//given
		ProceedApplyReqDto reqDto = new ProceedApplyReqDto(2L, true);
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();
		Member member = createMember();

		CrewMember crewMember = new CrewMember(1L, crew, leader, Role.LEADER);
		CrewApplication crewApplication = new CrewApplication(1L, "크루 신청글", Approval.PENDING, crew, member);

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.of(crew));
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewMember))
			.willReturn(Optional.empty());
		given(crewApplicationRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.of(crewApplication));
		given(crewMemberRepository.countByCrewId(anyLong()))
			.willReturn(1);
		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.proceedApplyCrew(reqDto, crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("제한 인원수를 초과하여 승인할 경우 예외 발생")
	@Test
	void failApproveCrewByOverLimitMember() {
		//given
		ProceedApplyReqDto reqDto = new ProceedApplyReqDto(2L, true);
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();
		Member member = createMember();

		CrewMember crewMember = new CrewMember(1L, crew, leader, Role.LEADER);
		CrewApplication crewApplication = new CrewApplication(1L, "크루 신청글", Approval.PENDING, crew, member);

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.of(crew));
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewMember))
			.willReturn(Optional.empty());
		given(crewApplicationRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.of(crewApplication));
		given(crewMemberRepository.countByCrewId(anyLong()))
			.willReturn(10);

		//when
		//then
		assertThatThrownBy(() -> crewService.proceedApplyCrew(reqDto, crewId, memberId))
			.isInstanceOf(ConflictException.class);
	}

	@DisplayName("크루원 강퇴 성공")
	@Test
	void successRemoveCrewMember() {
		//given
		Long crewId = 1L;
		Long memberId = 2L;
		Long leaderId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();
		Member member = createMember();

		CrewMember crewLeader = new CrewMember(1L, crew, leader, Role.LEADER);
		CrewMember crewMember = new CrewMember(2L, crew, member, Role.MEMBER);

		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewLeader));
		given(memberRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewMemberRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.of(crewMember));
		doNothing()
			.when(crewMemberRepository)
			.deleteById(anyLong());

		//when
		//then
		assertDoesNotThrow(() -> crewService.removeCrewMember(crewId, memberId, leaderId));
	}

	@DisplayName("강퇴하려는 크루가 없는 경우에 크루원 강퇴할 경우 예외 발생")
	@Test
	void failRemoveCrewMemberByNotFoundCrew() {
		//given
		Long crewId = 1L;
		Long memberId = 2L;
		Long leaderId = 1L;

		given(crewRepository.existsById(anyLong()))
			.willReturn(false);

		//when
		//then
		assertThatThrownBy(() -> crewService.removeCrewMember(crewId, memberId, leaderId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("소속 크루가 아닌 크루원이 크루원을 강퇴할 경우 예외 발생")
	@Test
	void failRemoveCrewMemberByNotFoundCrewMember() {
		//given
		Long crewId = 1L;
		Long memberId = 2L;
		Long leaderId = 1L;

		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.removeCrewMember(crewId, memberId, leaderId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("강퇴하려는 크루원이 소속 크루가 아닌 경우 예외 발생")
	@Test
	void failRemoveCrewMemberByNotInCrew() {
		//given
		Long crewId = 1L;
		Long memberId = 2L;
		Long leaderId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();

		CrewMember crewLeader = new CrewMember(1L, crew, leader, Role.LEADER);

		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewLeader));
		given(memberRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewMemberRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.removeCrewMember(crewId, memberId, leaderId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("강퇴할 크루원이 회원이 아닌 경우 예외 발생")
	@Test
	void failRemoveCrewMemberByNotFoundMember() {
		//given
		Long crewId = 1L;
		Long memberId = 2L;
		Long leaderId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();

		CrewMember crewLeader = new CrewMember(1L, crew, leader, Role.LEADER);

		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewLeader));
		given(memberRepository.existsById(anyLong()))
			.willReturn(false);

		//when
		//then
		assertThatThrownBy(() -> crewService.removeCrewMember(crewId, memberId, leaderId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("크루멤버의 크루 탈퇴 성공")
	@Test
	void successLeaveCrew() {
		//given
		Long crewId = 1L;
		Long memberId = 2L;

		Crew crew = createCrew();
		Member leader = createLeader();

		CrewMember crewMember = new CrewMember(1L, crew, leader, Role.MEMBER);

		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(memberRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewMemberRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.of(crewMember));
		doNothing()
			.when(crewMemberRepository)
			.deleteById(anyLong());

		//when
		//then
		assertDoesNotThrow(() -> crewService.leaveCrew(crewId, memberId));
	}

	@DisplayName("탈퇴하려는 크루가 없는 경우 예외 발생")
	@Test
	void failLeaveCrewByNotFoundCrew() {
		//given
		Long crewId = 1L;
		Long memberId = 2L;

		given(crewRepository.existsById(anyLong()))
			.willReturn(false);

		//when
		//then
		assertThatThrownBy(() -> crewService.leaveCrew(crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("인증되지 않은 사용자가 크루를 탈퇴할 시 예외 발생")
	@Test
	void failLeaveCrewByNotFoundMember() {
		//given
		Long crewId = 1L;
		Long memberId = 2L;

		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(memberRepository.existsById(anyLong()))
			.willReturn(false);

		//when
		//then
		assertThatThrownBy(() -> crewService.leaveCrew(crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("소속 크루원이 아닌 경우 크루 탈퇴 시 예외 발생")
	@Test
	void failLeaveCrewByNotInCrew() {
		//given
		Long crewId = 1L;
		Long memberId = 2L;

		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(memberRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewMemberRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.leaveCrew(crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("크루리더가 조건 만족 시 크루 탈퇴 성공")
	@Test
	void successLeaderLeaveCrews() {
		//given
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();

		CrewMember crewMember = new CrewMember(1L, crew, leader, Role.LEADER);

		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(memberRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewMemberRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.of(crewMember));
		given(crewMemberRepository.countByCrewId(anyLong()))
			.willReturn(1);
		doNothing()
			.when(crewRepository)
			.deleteById(anyLong());
		doNothing()
			.when(crewMemberRepository)
			.deleteById(anyLong());

		//when
		//then
		assertDoesNotThrow(() -> crewService.leaveCrew(crewId, memberId));
	}

	@DisplayName("크루원이 2명 이상 존재할 경우 크루장 탈퇴 시 예외 발생")
	@ParameterizedTest
	@ValueSource(ints = {2, 3, 10, 29})
	void failLeaderLeaveCrewsByMemberCnt(int currentMemberCnt) {
		//given
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = createCrew();
		Member leader = createLeader();

		CrewMember crewMember = new CrewMember(1L, crew, leader, Role.LEADER);

		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(memberRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewMemberRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.of(crewMember));
		given(crewMemberRepository.countByCrewId(anyLong()))
			.willReturn(currentMemberCnt);

		//when
		//then
		assertThatThrownBy(() -> crewService.leaveCrew(crewId, memberId))
			.isInstanceOf(ConflictException.class);
	}

	@DisplayName("크루 신청 취소 성공")
	@Test
	void successCancelCrewApplication() {
		//given
		Long crewId = 1L;
		Long memberId = 2L;

		Crew crew = createCrew();
		Member member1 = createMember();

		CrewApplication crewApplication = new CrewApplication(1L, "크루 신청글", Approval.PENDING, crew, member1);

		ApplyCrewResDto expected = new ApplyCrewResDto(crewId, memberId, false);

		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(memberRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewApplicationRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.of(crewApplication));
		doNothing()
			.when(crewApplicationRepository)
			.deleteById(anyLong());

		//when
		ApplyCrewResDto result = crewService.cancelCrewApplication(crewId, memberId);

		//then
		assertThat(result).isEqualTo(expected);
	}

	@DisplayName("신청을 취소하려는 크루가 없을 경우 예외 발생")
	@Test
	void failCancelCrewApplicationByNotFoundCrew() {
		//given
		Long crewId = 1L;
		Long memberId = 2L;

		given(crewRepository.existsById(anyLong()))
			.willReturn(false);

		//when
		//then
		assertThatThrownBy(() -> crewService.cancelCrewApplication(crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("인증된 사용자가 아닌 경우 예외 발생")
	@Test
	void failCancelCrewApplicationByNotFoundMember() {
		//given
		Long crewId = 1L;
		Long memberId = 2L;

		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(memberRepository.existsById(anyLong()))
			.willReturn(false);

		//when
		//then
		assertThatThrownBy(() -> crewService.cancelCrewApplication(crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("크루에 신청하지 않은 사용자가 크루 신청 취소할 경우 예외 발생")
	@Test
	void failCancelCrewApplicationByNotInCrew() {
		//given
		Long crewId = 1L;
		Long memberId = 2L;

		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(memberRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewApplicationRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.cancelCrewApplication(crewId, memberId))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("크루 정보 수정 성공")
	@Test
	void successUpdateCrew() {
		//given
		Long crewId = 1L;
		Long memberId = 1L;
		UpdateCrewReqDto reqDto = new UpdateCrewReqDto(List.of("수정1", "수정2"), AUTO, "크루 소개 수정",
			new RuleDto(3, 10));
		MockMultipartFile image = createImage();

		Crew crew = createCrew();
		Member leader = createLeader();
		CrewMember crewMember = new CrewMember(1L, crew, leader, Role.LEADER);

		CrewImage crewImage = createCrewImage();

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.of(crew));
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewMember));
		given(memberRepository.existsById(anyLong()))
			.willReturn(true);
		doNothing()
			.when(tagRepository)
			.deleteAllByCrewId(anyLong());
		given(tagRepository.saveAll(anyList()))
			.willReturn(null);
		given(crewImageRepository.findByCrewId(anyLong()))
			.willReturn(Optional.of(crewImage));
		doNothing()
			.when(storageManagerUseCase)
			.deleteImages(anyString());
		given(storageManagerUseCase.uploadImage(any(MockMultipartFile.class)))
			.willReturn("https://update.com");

		//when
		//then
		assertDoesNotThrow(() -> crewService.updateCrew(reqDto, image, crewId, memberId));
	}

	private CrewImage createCrewImage() {
		Crew crew = createCrew();
		return new CrewImage(1L, "크루이미지", "https://test.com", crew);
	}

	private CreateCrewReqDto getCreateCrewReqDto() {
		RuleDto rule = new RuleDto(5, 100);
		return new CreateCrewReqDto("구름 크루", RUNNING, List.of("성실"), MANUAL, "구름 크루는 성실한 크루", rule
		);
	}

	private static ApplyCrewReqDto getApplyCrewReqDto() {
		return new ApplyCrewReqDto("크루 신청글");
	}

	private MockMultipartFile createImage() {
		return new MockMultipartFile("크루로고", "크루로고.png", MediaType.IMAGE_PNG_VALUE, "크루로고.png".getBytes());
	}

	private Crew createCrew() {
		return new Crew(1L, 1L, "구름 크루", 10, RUNNING, MANUAL, "구름 크루는 성실한 크루", 5, 100, 0, 0);
	}

	private Crew createAutoCrew() {
		return new Crew(2L, 2L, "클로피 크루", 8, RUNNING, AUTO, "클로피 크루는 최고의 크루", 7, 500, 1000, 3000);
	}

	private Member createLeader() {
		return new Member(1L, "크루장", Gender.MALE, 20, 70, "https://image-url.com", 0, 10, 30, 100);
	}

	private Member createMember() {
		return new Member(2L, "사용자", Gender.FEMALE, 30, 80, "https://image-url.com", 0, 0, 0, 0);
	}
}
