package clofi.runningplanet.crew.service.integration;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import clofi.runningplanet.common.DatabaseCleaner;
import clofi.runningplanet.crew.domain.ApprovalType;
import clofi.runningplanet.crew.domain.Category;
import clofi.runningplanet.crew.dto.RuleDto;
import clofi.runningplanet.crew.dto.request.CreateCrewReqDto;
import clofi.runningplanet.crew.dto.response.FindAllCrewResDto;
import clofi.runningplanet.crew.service.CrewService;
import clofi.runningplanet.member.domain.Gender;
import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.member.repository.MemberRepository;

@SpringBootTest
public class CrewServiceIntegrationTest {

	@Autowired
	CrewService crewService;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	DatabaseCleaner cleaner;

	@BeforeEach
	void init() {
		Member member1 = Member.builder()
			.nickname("크루장")
			.id(1L)
			.profileImg("https://test.com")
			.gender(Gender.MALE)
			.age(30)
			.weight(70)
			.build();
		memberRepository.save(member1);

		Member member2 = Member.builder()
			.nickname("크루")
			.id(2L)
			.profileImg("https://test.com")
			.gender(Gender.FEMALE)
			.age(20)
			.weight(60)
			.build();
		memberRepository.save(member2);
	}

	@AfterEach
	void setUp() {
		cleaner.truncateAllTables();
	}

	@DisplayName("크루를 생성 테스트 코드")
	@Test
	void createCrew() {
		//given
		Long memberId = 1L;

		CreateCrewReqDto reqDto = new CreateCrewReqDto("크루명", Category.RUNNING, List.of("태그"), ApprovalType.AUTO,
			"크루 소개", new RuleDto(3, 10));
		MockMultipartFile image = new MockMultipartFile("imgFile", "크루로고.png", MediaType.IMAGE_PNG_VALUE,
			"크루로고.png".getBytes());

		//when
		Long crewId = crewService.createCrew(reqDto, image, memberId);

		//then
		assertThat(crewId).isNotNull();
	}

	@DisplayName("크루 목록을 조회 할 수 있다.")
	@Test
	void findAllCrews() {
		//given

		CreateCrewReqDto reqDto1 = new CreateCrewReqDto("크루명1", Category.RUNNING, List.of("태그1"), ApprovalType.AUTO,
			"크루 소개2", new RuleDto(3, 10));
		MockMultipartFile image1 = new MockMultipartFile("imgFile", "크루로고1.png", MediaType.IMAGE_PNG_VALUE,
			"크루로고1.png".getBytes());

		Long crewId1 = crewService.createCrew(reqDto1, image1, 1L);

		CreateCrewReqDto reqDto2 = new CreateCrewReqDto("크루명2", Category.RUNNING, List.of("태그2"), ApprovalType.AUTO,
			"크루 소개2", new RuleDto(3, 10));
		MockMultipartFile image2 = new MockMultipartFile("imgFile", "크루로고2.png", MediaType.IMAGE_PNG_VALUE,
			"크루로고2.png".getBytes());

		Long crewId2 = crewService.createCrew(reqDto2, image2, 2L);

		//when
		List<FindAllCrewResDto> result = crewService.findAllCrew();

		//then
		assertSoftly(
			softAssertions -> {
				softAssertions.assertThat(result.size()).isEqualTo(2);
				softAssertions.assertThat(result).extracting("crewId")
					.contains(crewId1, crewId2);
			}
		);

	}
}
