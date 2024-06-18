package clofi.runningplanet.mission.service.integration;

import static org.assertj.core.api.SoftAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import clofi.runningplanet.common.DataCleaner;
import clofi.runningplanet.crew.domain.ApprovalType;
import clofi.runningplanet.crew.domain.Category;
import clofi.runningplanet.crew.domain.Crew;
import clofi.runningplanet.crew.domain.CrewMember;
import clofi.runningplanet.crew.dto.RuleDto;
import clofi.runningplanet.crew.dto.request.CreateCrewReqDto;
import clofi.runningplanet.crew.repository.CrewMemberRepository;
import clofi.runningplanet.crew.repository.CrewRepository;
import clofi.runningplanet.crew.service.CrewService;
import clofi.runningplanet.member.domain.Gender;
import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.member.repository.MemberRepository;
import clofi.runningplanet.mission.domain.CrewMission;
import clofi.runningplanet.mission.domain.MissionType;
import clofi.runningplanet.mission.dto.response.CrewMissionListDto;
import clofi.runningplanet.mission.repository.CrewMissionRepository;
import clofi.runningplanet.mission.service.MissionService;
import clofi.runningplanet.running.domain.Record;
import clofi.runningplanet.running.repository.RecordRepository;

@SpringBootTest
public class MissionServiceIntegrationTest {

	@Autowired
	MissionService missionService;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	CrewRepository crewRepository;

	@Autowired
	CrewMemberRepository crewMemberRepository;

	@Autowired
	CrewMissionRepository crewMissionRepository;

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	CrewService crewService;

	@Autowired
	DataCleaner cleaner;

	@AfterEach
	void setUp() {
		cleaner.truncateAllTables();
	}

	@DisplayName("크루를 생성하면 크루 미션 2개가 생성된다.")
	@Test
	void createCrewCreate2CrewMission() {
		//given
		Member member = saveMember1();

		CreateCrewReqDto reqDto = new CreateCrewReqDto("크루명", Category.RUNNING, List.of("태그"), ApprovalType.AUTO,
			"크루 소개", new RuleDto(3, 10));
		MockMultipartFile image = new MockMultipartFile("imgFile", "크루로고.png", MediaType.IMAGE_PNG_VALUE,
			"크루로고.png".getBytes());
		Long crewId = crewService.createCrew(reqDto, image, member.getId());

		//when
		CrewMissionListDto result = missionService.getCrewMission(crewId, member.getId());

		//then
		assertSoftly(softly -> {
			softly.assertThat(result.missions()).hasSize(2)
				.extracting("missionContent")
				.containsExactlyInAnyOrder(MissionType.DURATION, MissionType.DISTANCE);

			softly.assertThat(result.missions())
				.extracting("missionProgress")
				.allMatch(progress -> progress.equals(0.0));

			softly.assertThat(result.missions())
				.extracting("missionComplete")
				.allMatch(complete -> complete.equals(false));
		});

	}

	@DisplayName("조건을 만족한 경우 미션을 완료처리 할 수 있다.")
	@Test
	void successDistanceMission() {
		//given
		Member member = saveMember1();
		Crew crew = createCrew(member);
		createRecord(member, 3600, 1000);

		CrewMission mission1 = new CrewMission(member, crew, MissionType.DISTANCE);
		Long missionId1 = crewMissionRepository.save(mission1).getId();
		CrewMission mission2 = new CrewMission(member, crew, MissionType.DURATION);
		Long missionId2 = crewMissionRepository.save(mission2).getId();

		//when
		//then
		assertDoesNotThrow(() -> missionService.successMission(crew.getId(), missionId1, member.getId()));
		assertDoesNotThrow(() -> missionService.successMission(crew.getId(), missionId2, member.getId()));
	}

	private Member saveMember1() {
		Member member1 = Member.builder()
			.nickname("크루장")
			.profileImg("https://test.com")
			.gender(Gender.MALE)
			.age(30)
			.weight(70)
			.build();
		return memberRepository.save(member1);
	}

	private Crew createCrew(Member member) {
		Crew crew = new Crew(null, member.getId(), "구름", 10, Category.RUNNING, ApprovalType.AUTO, "크루 소개", 3, 1, 0, 0,
			0,
			1);
		Crew savedCrew = crewRepository.save(crew);

		CrewMember crewMember = CrewMember.createLeader(savedCrew, member);
		crewMemberRepository.save(crewMember);

		return savedCrew;
	}

	private void createRecord(Member member, int duration, double distance) {
		Record record = Record.builder()
			.member(member)
			.runTime(duration)
			.runDistance(distance)
			.isEnd(true)
			.build();
		recordRepository.save(record);
	}
}
