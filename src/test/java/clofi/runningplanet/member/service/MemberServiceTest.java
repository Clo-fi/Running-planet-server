package clofi.runningplanet.member.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import clofi.runningplanet.common.FakeS3StorageManager;
import clofi.runningplanet.crew.domain.ApprovalType;
import clofi.runningplanet.crew.domain.Category;
import clofi.runningplanet.crew.domain.Crew;
import clofi.runningplanet.crew.domain.CrewMember;
import clofi.runningplanet.crew.domain.Role;
import clofi.runningplanet.crew.repository.CrewMemberRepository;
import clofi.runningplanet.crew.repository.CrewRepository;
import clofi.runningplanet.member.domain.Gender;
import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.member.dto.request.UpdateProfileRequest;
import clofi.runningplanet.member.dto.response.ProfileResponse;
import clofi.runningplanet.member.repository.MemberRepository;
import clofi.runningplanet.member.repository.SocialLoginRepository;

@SpringBootTest
class MemberServiceTest {
	@Autowired
	MemberService memberService;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	CrewMemberRepository crewMemberRepository;

	@Autowired
	CrewRepository crewRepository;
	@Autowired
	private SocialLoginRepository socialLoginRepository;

	@AfterEach
	void tearDown() {
		crewMemberRepository.deleteAllInBatch();
		crewRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@DisplayName("memberId로 조회할 수 있다.")
	@Test
	void getProfileWithCrew() {
		//given
		//크루 있는 경우
		Member member1 = Member.builder()
			.nickname("고구마1")
			.profileImg(
				"https://pbs.twimg.com/media/E86TJH1VkAQ0BGV.png")
			.age(34)
			.gender(Gender.MALE)
			.runScore(20)
			.avgPace(2400)
			.avgDistance(5000)
			.totalDistance(30000)
			.build();
		memberRepository.save(member1);

		Crew crew1 = new Crew(
			member1.getId(),"고구마크루" ,5,10, Category.RUNNING, ApprovalType.AUTO,"소개글",7,1);
		crewRepository.save(crew1);

		CrewMember crewMember1 = CrewMember.builder()
			.member(member1)
			.role(Role.LEADER)
			.crew(crew1)
			.build();
		crewMemberRepository.save(crewMember1);

		//크루 없는 경우
		Member member2 = memberRepository.save(createMember());

		//when
		ProfileResponse profileResponseWithCrew = memberService.getProfile(member1.getId());
		ProfileResponse profileResponseWithoutCrew = memberService.getProfile(member2.getId());


		//then
		assertThat(profileResponseWithCrew).isNotNull();
		assertThat(profileResponseWithCrew.nickname()).isEqualTo(member1.getNickname());
		assertThat(profileResponseWithCrew.myCrew()).isEqualTo("고구마크루");

		assertThat(profileResponseWithoutCrew).isNotNull();
		assertThat(profileResponseWithoutCrew.nickname()).isEqualTo(member2.getNickname());
		assertThat(profileResponseWithoutCrew.myCrew()).isNull();
	}

	@DisplayName("프로필을 수정할 수 있다.")
	@Test
	void updateProfileTest() {
		//given
		Member member = createMember();
		Member savedMember = memberRepository.save(member);

		MultipartFile imageFile = getImageFile();
		UpdateProfileRequest request = new UpdateProfileRequest("스위트포테이토");
		MemberService memberService = getMemberService();

		//when
		memberService.updateProfile(member.getId(),request,imageFile);
		Member updatedMember = memberRepository.findById(savedMember.getId())
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));;

		//then
		assertThat(updatedMember.getNickname()).isEqualTo(request.nickname());
		assertThat(updatedMember.getProfileImg()).isEqualTo("fakeImageUrl1");
	}



	private Member createMember() {
		return Member.builder()
			.nickname("고구마")
			.profileImg(
				"https://pbs.twimg.com/media/E86TJH1VkAQ0BGV.png")
			.age(34)
			.gender(Gender.MALE)
			.runScore(20)
			.avgPace(2400)
			.avgDistance(5000)
			.totalDistance(30000)
			.build();
	}

	private MultipartFile getImageFile() {

		return new MockMultipartFile(
				"image1", // 파일 파라미터 이름
				"image1.jpg", // 파일명
				"image/jpeg", // 컨텐츠 타입
				"이미지_콘텐츠1".getBytes() // 파일 콘텐츠
				);
	}

	// 테스트용 멤버서비스
	private MemberService getMemberService() {
		FakeS3StorageManager fakeS3StorageManager = new FakeS3StorageManager();
		return new MemberService(
			memberRepository,
			crewMemberRepository,
			socialLoginRepository,
			fakeS3StorageManager
		);
	}

}