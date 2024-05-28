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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import clofi.runningplanet.common.exception.ConflictException;
import clofi.runningplanet.common.exception.NotFoundException;
import clofi.runningplanet.crew.domain.Approval;
import clofi.runningplanet.crew.domain.Crew;
import clofi.runningplanet.crew.domain.CrewApplication;
import clofi.runningplanet.crew.domain.CrewMember;
import clofi.runningplanet.crew.domain.Role;
import clofi.runningplanet.crew.domain.Tag;
import clofi.runningplanet.crew.dto.CrewLeaderDto;
import clofi.runningplanet.crew.dto.RuleDto;
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
import clofi.runningplanet.member.domain.Gender;
import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class CrewServiceTest {

	private static final Member MEMBER = Member.builder()
		.id(1L)
		.nickname("닉네임")
		.age(20)
		.gender(Gender.MALE)
		.profileImg("https://image-url.com")
		.avgDistance(10)
		.totalDistance(100)
		.runScore(50)
		.build();

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

	@InjectMocks
	private CrewService crewService;

	@DisplayName("크루 생성 성공")
	@Test
	void successCreateCrew() {
		// given
		Long leaderId = 1L;

		RuleDto rule = new RuleDto(5, 100);

		CreateCrewReqDto reqDto = new CreateCrewReqDto(
			"크루명",
			5,
			50,
			RUNNING,
			List.of("성실"),
			AUTO,
			"크루를 소개하는 글",
			rule
		);

		Crew crew = new Crew(
			1L,
			MEMBER.getId(),
			"크루명",
			5,
			50,
			RUNNING,
			AUTO,
			"크루를 소개하는 글",
			5,
			100,
			0,
			0
		);

		given(crewRepository.save(any(Crew.class))).willReturn(crew);
		given(tagRepository.saveAll(anyList())).willReturn(Collections.emptyList());
		given(crewMemberRepository.save(any(CrewMember.class))).willReturn(
			new CrewMember(1L, crew, MEMBER, Role.LEADER));
		given(memberRepository.findById(anyLong())).willReturn(Optional.of(MEMBER));
		given(crewMemberRepository.existsByMemberId(anyLong()))
			.willReturn(false);

		// when
		Long result = crewService.createCrew(reqDto, leaderId);

		// then
		assertThat(result).isEqualTo(1L);
	}

	@DisplayName("크루 생성시 등록되지 않은 사용자가 들어올 경우 예외 발생")
	@Test
	void failCreateCrewByNotFoundMember() {
		//given
		RuleDto rule = new RuleDto(5, 100);

		CreateCrewReqDto reqDto = new CreateCrewReqDto(
			"크루명",
			5,
			50,
			RUNNING,
			List.of("성실"),
			AUTO,
			"크루를 소개하는 글",
			rule
		);

		given(memberRepository.findById(anyLong())).willReturn(Optional.empty());

		//when
		//then
		assertThatThrownBy(() -> crewService.createCrew(reqDto, MEMBER.getId()))
			.isInstanceOf(NotFoundException.class);
	}

	@DisplayName("크루에 속해있는 사용자가 크루 생성시 예외 발생")
	@Test
	void test() {
		//given
		RuleDto rule = new RuleDto(5, 100);

		CreateCrewReqDto reqDto = new CreateCrewReqDto(
			"크루명",
			5,
			50,
			RUNNING,
			List.of("성실"),
			AUTO,
			"크루를 소개하는 글",
			rule
		);

		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.ofNullable(MEMBER));
		given(crewMemberRepository.existsByMemberId(anyLong()))
			.willReturn(true);

		//when
		//then
		assertThatThrownBy(() -> crewService.createCrew(reqDto, MEMBER.getId()))
			.isInstanceOf(ConflictException.class);
	}

	@DisplayName("크루 목록 조회 성공")
	@Test
	void successFindAllCrew() {
		//given
		Crew crew1 = new Crew(1L, 1L, "구름 크루", 10, 50,
			RUNNING, AUTO, "구름 크루는 성실한 크루", 5, 100,
			0, 0);
		Crew crew2 = new Crew(2L, 2L, "클로피 크루", 8, 90,
			RUNNING, MANUAL, "클로피 크루는 최고의 크루", 7, 500,
			1000, 3000);

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
			.willReturn(Optional.of(MEMBER));

		//when
		List<FindAllCrewResDto> result = crewService.findAllCrew();

		//then
		final FindAllCrewResDto firstFindAllCrewResDto = FindAllCrewResDto.of(crew1, List.of("성실"),
			new CrewLeaderDto(1L, "닉네임"));

		final FindAllCrewResDto secondFindAllCrewResDto = FindAllCrewResDto.of(crew2, List.of("최고"),
			new CrewLeaderDto(2L, "닉네임"));

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
		Crew crew1 = new Crew(1L, 1L, "구름 크루", 10, 50,
			RUNNING, AUTO, "구름 크루는 성실한 크루", 5, 100,
			0, 0);

		given(crewRepository.findAll())
			.willReturn(List.of(crew1));

		given(tagRepository.findAllByCrewId(anyLong()))
			.willReturn(List.of(
				new Tag(1L, crew1, "성실")
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

		Crew crew = new Crew(1L, 1L, "구름 크루", 10, 50,
			RUNNING, AUTO, "구름 크루는 성실한 크루", 5, 100,
			0, 0);

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.of(crew));

		given(tagRepository.findAllByCrewId(anyLong()))
			.willReturn(List.of(
				new Tag(1L, null, "성실")
			));

		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.of(MEMBER));

		//when
		FindCrewResDto result = crewService.findCrew(crewId);

		//then
		final FindCrewResDto findCrewResDto = FindCrewResDto.of(crew, new CrewLeaderDto(1L, "닉네임"), List.of("성실"));

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

		Crew crew = new Crew(1L, 1L, "구름 크루", 10, 50,
			RUNNING, AUTO, "구름 크루는 성실한 크루", 5, 100,
			0, 0);

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
		ApplyCrewReqDto reqDto = new ApplyCrewReqDto("크루 신청서");
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = new Crew(1L, 2L, "구름 크루", 10, 50,
			RUNNING, MANUAL, "구름 크루는 성실한 크루", 5, 100,
			0, 0);

		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.of(MEMBER));
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
		ApplyCrewReqDto reqDto = new ApplyCrewReqDto("크루 신청서");
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
		ApplyCrewReqDto reqDto = new ApplyCrewReqDto("크루 신청서");
		Long crewId = 1L;
		Long memberId = 1L;

		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.of(MEMBER));
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
		ApplyCrewReqDto reqDto = new ApplyCrewReqDto("크루 신청서");
		Long crewId = 1L;
		Long memberId = 1L;

		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.of(MEMBER));
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

		Crew crew = new Crew(1L, 1L, "구름 크루", 10, 50,
			RUNNING, MANUAL, "구름 크루는 성실한 크루", 5, 100,
			0, 0);
		CrewMember crewMember = new CrewMember(1L, crew, MEMBER, Role.LEADER);

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

		CrewApplication crewApplication1 = new CrewApplication(1L, "크루 신청글1", Approval.PENDING, crew, member1);
		CrewApplication crewApplication2 = new CrewApplication(2L, "크루 신청글2", Approval.PENDING, crew, member2);

		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewMember));
		given(crewRepository.existsById(anyLong()))
			.willReturn(true);
		given(crewApplicationRepository.findAllByCrewId(anyLong()))
			.willReturn(List.of(crewApplication1, crewApplication2));

		//when
		ApprovalMemberResDto result = crewService.getApplyCrewList(crewId, memberId);

		//then
		GetApplyCrewResDto getApplyCrewResDto1 = new GetApplyCrewResDto(2L, "닉네임1", "크루 신청글1", 80, Gender.MALE, 30,
			Approval.PENDING);
		GetApplyCrewResDto getApplyCrewResDto2 = new GetApplyCrewResDto(3L, "닉네임2", "크루 신청글2", 70, Gender.FEMALE, 15,
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

		Crew crew = new Crew(1L, 1L, "구름 크루", 10, 50,
			RUNNING, MANUAL, "구름 크루는 성실한 크루", 5, 100,
			0, 0);
		CrewMember crewMember = new CrewMember(1L, crew, MEMBER, Role.LEADER);

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

		Crew crew = new Crew(1L, 1L, "구름 크루", 10, 50,
			RUNNING, MANUAL, "구름 크루는 성실한 크루", 5, 100,
			0, 0);
		CrewMember crewMember = new CrewMember(1L, crew, MEMBER, Role.LEADER);

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

		Crew crew = new Crew(1L, 1L, "구름 크루", 10, 50,
			RUNNING, MANUAL, "구름 크루는 성실한 크루", 5, 100,
			0, 0);
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

		CrewMember crewMember = new CrewMember(1L, crew, MEMBER, Role.LEADER);
		CrewApplication crewApplication = new CrewApplication(1L, "크루 신청글", Approval.PENDING, crew, member1);

		given(crewRepository.findById(anyLong()))
			.willReturn(Optional.of(crew));
		given(crewMemberRepository.findByMemberId(anyLong()))
			.willReturn(Optional.of(crewMember))
			.willReturn(Optional.empty());
		given(crewApplicationRepository.findByCrewIdAndMemberId(anyLong(), anyLong()))
			.willReturn(Optional.of(crewApplication));
		given(crewMemberRepository.findByCrewId(anyLong()))
			.willReturn(List.of(new CrewMember(null, null, null, null), new CrewMember(null, null, null, null)));
		given(memberRepository.findById(anyLong()))
			.willReturn(Optional.of(member1));
		given(crewMemberRepository.save(any(CrewMember.class)))
			.willReturn(crewMember);

		//when
		//then
		assertDoesNotThrow(() -> crewService.proceedApplyCrew(reqDto, crewId, memberId));
	}

	@DisplayName("크루 가입 거절 성공")
	@Test
	void test1() {
		//given
		ProceedApplyReqDto reqDto = new ProceedApplyReqDto(2L, false);
		Long crewId = 1L;
		Long memberId = 1L;

		Crew crew = new Crew(1L, 1L, "구름 크루", 10, 50,
			RUNNING, MANUAL, "구름 크루는 성실한 크루", 5, 100,
			0, 0);
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

		CrewMember crewMember = new CrewMember(1L, crew, MEMBER, Role.LEADER);
		CrewApplication crewApplication = new CrewApplication(1L, "크루 신청글", Approval.PENDING, crew, member1);

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
}
