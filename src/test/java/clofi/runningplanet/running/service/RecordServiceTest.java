package clofi.runningplanet.running.service;

import static clofi.runningplanet.common.TimeUtils.*;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.auditing.AuditingHandler;

import clofi.runningplanet.crew.domain.ApprovalType;
import clofi.runningplanet.crew.domain.Category;
import clofi.runningplanet.crew.domain.Crew;
import clofi.runningplanet.crew.domain.CrewMember;
import clofi.runningplanet.crew.domain.Role;
import clofi.runningplanet.crew.repository.CrewMemberRepository;
import clofi.runningplanet.crew.repository.CrewRepository;
import clofi.runningplanet.crew.service.CrewService;
import clofi.runningplanet.member.domain.Gender;
import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.member.repository.MemberRepository;
import clofi.runningplanet.running.domain.Coordinate;
import clofi.runningplanet.running.domain.Record;
import clofi.runningplanet.running.dto.RecordFindAllResponse;
import clofi.runningplanet.running.dto.RecordFindCurrentResponse;
import clofi.runningplanet.running.dto.RecordFindResponse;
import clofi.runningplanet.running.dto.RecordSaveRequest;
import clofi.runningplanet.running.dto.RunningStatusResponse;
import clofi.runningplanet.running.repository.CoordinateRepository;
import clofi.runningplanet.running.repository.RecordRepository;

@SpringBootTest
class RecordServiceTest {
	@Autowired
	RecordService recordService;

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	CoordinateRepository coordinateRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	CrewService crewService;

	@SpyBean
	private AuditingHandler auditingHandler;

	@Autowired
	private CrewRepository crewRepository;

	@Autowired
	private CrewMemberRepository crewMemberRepository;

	@AfterEach
	void tearDown() {
		crewMemberRepository.deleteAllInBatch();
		crewRepository.deleteAllInBatch();
		coordinateRepository.deleteAllInBatch();
		recordRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@DisplayName("운동, 좌표 정보로 운동 기록을 생성할 수 있다.")
	@Test
	void saveRecord() {
		// given
		RecordSaveRequest recordSaveRequest = new RecordSaveRequest(
			100.23,
			200.23,
			630,
			1.23,
			300,
			new RecordSaveRequest.AvgPace(
				8,
				20
			),
			false
		);

		// when
		Member member = memberRepository.save(createMember("감자"));
		Record savedRecord = recordService.save(recordSaveRequest, member.getId());

		// then
		assertThat(savedRecord.getId()).isNotNull();
		assertThat(savedRecord)
			.extracting("runTime", "runDistance", "calories", "avgPace", "endTime")
			.contains(630, 1.23, 300, 500, null);

		Optional<Coordinate> savedCoordinate = coordinateRepository.findByRecord(savedRecord);
		assertThat(savedCoordinate).isPresent();
		assertThat(savedCoordinate.get())
			.extracting("latitude", "longitude")
			.contains(100.23, 200.23);
	}

	@DisplayName("종료되지 않은 기록을 업데이트하고, 좌표를 추가할 수 있다.")
	@Test
	void updateRecordAndAddCoordinate() {
		// given
		RecordSaveRequest recordSaveRequest1 = new RecordSaveRequest(
			100.23,
			200.23,
			630,
			1.23,
			300,
			new RecordSaveRequest.AvgPace(
				8,
				20
			),
			false
		);

		Member member = memberRepository.save(createMember("감자"));
		recordService.save(recordSaveRequest1, member.getId());

		RecordSaveRequest recordSaveRequest2 = new RecordSaveRequest(
			200.23,
			300.23,
			900,
			2.0,
			400,
			new RecordSaveRequest.AvgPace(
				10,
				30
			),
			true
		);

		// when
		Record updatedRecord = recordService.save(recordSaveRequest2, member.getId());

		// then
		assertThat(updatedRecord.getId()).isNotNull();
		assertThat(updatedRecord)
			.extracting("runTime", "runDistance", "calories", "avgPace")
			.contains(900, 2.0, 400, 630);
		assertThat(updatedRecord.getEndTime()).isNotNull();

		List<Coordinate> savedCoordinates = coordinateRepository.findAllByRecord(updatedRecord);
		assertThat(savedCoordinates).hasSize(2)
			.extracting("latitude", "longitude")
			.containsExactlyInAnyOrder(
				tuple(100.23, 200.23),
				tuple(200.23, 300.23)
			);
	}

	@DisplayName("year, month 로 운동 기록을 조회할 수 있다.")
	@Test
	void findAllRecordsByYearAndMonth() {
		// given
		Member member = memberRepository.save(createMember("감자"));

		LocalDateTime createdDateTime1 = getEndOfDay(LocalDate.of(2024, 1, 31));
		auditingHandler.setDateTimeProvider(() -> Optional.of(createdDateTime1));
		Record record1 = createRecord(member, 65, 1.00, 1000, 100,
			createdDateTime1.plus(Duration.of(1000, ChronoUnit.SECONDS)));
		recordRepository.save(record1);

		LocalDateTime createdDateTime2 = getStartOfDay(LocalDate.of(2024, 2, 1));
		auditingHandler.setDateTimeProvider(() -> Optional.of(createdDateTime2));
		Record record2 = createRecord(member, 65, 2.00, 2000, 200,
			createdDateTime2.plus(Duration.of(2000, ChronoUnit.SECONDS)));
		recordRepository.save(record2);

		LocalDateTime createdDateTime3 = getEndOfDay(LocalDate.of(2024, 2, 29));
		auditingHandler.setDateTimeProvider(() -> Optional.of(createdDateTime3));
		Record record3 = createRecord(member, 65, 3.00, 3000, 300,
			createdDateTime3.plus(Duration.of(3000, ChronoUnit.SECONDS)));
		recordRepository.save(record3);

		LocalDateTime createdDateTime4 = getStartOfDay(LocalDate.of(2024, 3, 1));
		auditingHandler.setDateTimeProvider(() -> Optional.of(createdDateTime4));
		Record record4 = createRecord(member, 65, 4.00, 4000, 400,
			createdDateTime4.plus(Duration.of(4000, ChronoUnit.SECONDS)));
		recordRepository.save(record4);

		int year = 2024;
		int month = 2;

		// when
		List<RecordFindAllResponse> response = recordService.findAll(year, month, member.getId());

		assertThat(response).hasSize(2)
			.extracting("id", "runDistance", "day")
			.containsExactlyInAnyOrder(
				tuple(record2.getId(), 2.00, 1),
				tuple(record3.getId(), 3.00, 29)
			);
	}

	@DisplayName("운동 아이디로 운동 기록을 조회할 수 있다.")
	@Test
	void findRecord() {
		// given
		Member member = memberRepository.save(createMember("감자"));

		LocalDateTime endTime = getEndOfDay(LocalDate.now());
		Record record = createRecord(member, 65, 1.00, 3665, 300, endTime);
		Coordinate coordinate1 = createCoordinate(record, 10.00, 20.00);
		Coordinate coordinate2 = createCoordinate(record, 20.00, 30.00);
		Record savedRecord = recordRepository.save(record);
		coordinateRepository.save(coordinate1);
		coordinateRepository.save(coordinate2);

		Long recordId = savedRecord.getId();

		// when
		RecordFindResponse response = recordService.find(recordId, member.getId());

		// then
		assertThat(response.id()).isNotNull();
		assertThat(response.avgPace())
			.extracting("min", "sec")
			.contains(1, 5);
		assertThat(response.runTime())
			.extracting("hour", "min", "sec")
			.contains(1, 1, 5);
		assertThat(response)
			.extracting("runDistance", "calories", "endTime")
			.contains(1.00, 300, endTime);
		assertThat(response.coordinateResponses()).hasSize(2)
			.extracting("latitude", "longitude")
			.containsExactlyInAnyOrder(
				tuple(10.00, 20.00),
				tuple(20.00, 30.00)
			);
	}

	@DisplayName("종료되지 않은 운동 기록은 조회할 수 없다.")
	@Test
	void findUnfinishedRecord() {
		// given
		Member member = memberRepository.save(createMember("감자"));

		Record record = createRecord(member, 65, 1.00, 3665, 300, null);
		Record saved = recordRepository.save(record);
		Long recordId = saved.getId();

		// when & then
		assertThatIllegalArgumentException()
			.isThrownBy(() -> recordService.find(recordId, member.getId()))
			.withMessage("운동 기록을 찾을 수 없습니다.");
	}

	@DisplayName("운동 아이디가 존재하지 않거나, 자신의 운동 기록이 아니면 예외가 발생한다.")
	@Test
	void findRecordByInvalidRequest() {
		// given
		Member member = memberRepository.save(createMember("감자"));
		Member member2 = memberRepository.save(createMember("감자"));

		LocalDateTime endTime = getEndOfDay(LocalDate.now());
		Record record = createRecord(member, 65, 1.00, 3665, 300, endTime);
		Coordinate coordinate1 = createCoordinate(record, 10.00, 20.00);
		Coordinate coordinate2 = createCoordinate(record, 20.00, 30.00);
		Record savedRecord = recordRepository.save(record);
		coordinateRepository.save(coordinate1);
		coordinateRepository.save(coordinate2);

		Long recordId = savedRecord.getId();

		// when & then
		assertThatIllegalArgumentException()
			.isThrownBy(() -> recordService.find(recordId + 1, member.getId()))
			.withMessage("운동 기록을 찾을 수 없습니다.");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> recordService.find(recordId, member2.getId()))
			.withMessage("운동 기록을 찾을 수 없습니다.");
	}

	@DisplayName("현재 운동 정보를 조회할 수 있다.")
	@Test
	void findCurrentRecord() {
		// given
		Member member = memberRepository.save(createMember("감자"));

		Record record = createRecord(member, 65, 1.00, 3665, 300, null);
		Coordinate coordinate1 = createCoordinate(record, 10.00, 20.00);
		Coordinate coordinate2 = createCoordinate(record, 20.00, 30.00);
		recordRepository.save(record);
		coordinateRepository.save(coordinate1);
		coordinateRepository.save(coordinate2);

		// when
		RecordFindCurrentResponse response = recordService.findCurrentRecord(member.getId());

		// then
		assertThat(response.id()).isNotNull();
		assertThat(response.avgPace())
			.extracting("min", "sec")
			.contains(1, 5);
		assertThat(response.runTime())
			.extracting("hour", "min", "sec")
			.contains(1, 1, 5);
		assertThat(response)
			.extracting("runDistance", "calories", "latitude", "longitude")
			.contains(1.00, 300, 20.00, 30.00);
	}

	@DisplayName("현재 운동 조회 시 종료되지 않은 운동 기록이 없으면 null이 반환된다.")
	@Test
	void findCurrentWorkoutRecordWhenNoneUnfinished() {
		// given
		Member member = memberRepository.save(createMember("감자"));

		Record record = createRecord(member, 65, 1.00, 3665, 300, LocalDateTime.now());
		Coordinate coordinate = createCoordinate(record, 10.00, 20.00);
		recordRepository.save(record);
		coordinateRepository.save(coordinate);

		// when
		RecordFindCurrentResponse response = recordService.findCurrentRecord(member.getId());

		// then
		assertThat(response).isNull();
	}

	@DisplayName("회원과 크루 정보로 운동 현황 목록 조회를 할 수 있다.")
	@Test
	void findAllRunningStatus() {
		Member member1 = memberRepository.save(createMember("회원1"));
		Member member2 = memberRepository.save(createMember("회원2"));
		Member member3 = memberRepository.save(createMember("회원3"));
		Crew crew = new Crew(member1.getId(), "crew1", 5, Category.RUNNING, ApprovalType.AUTO, "crew1", 1, 1);
		crew = crewRepository.save(crew);
		crewMemberRepository.save(CrewMember.builder().crew(crew).member(member1).role(Role.LEADER).build());
		crewMemberRepository.save(CrewMember.builder().crew(crew).member(member2).role(Role.MEMBER).build());
		crewMemberRepository.save(CrewMember.builder().crew(crew).member(member3).role(Role.MEMBER).build());

		LocalDate today = LocalDate.now();

		LocalDateTime endOfYesterday = getEndOfDay(today.minusDays(1));
		auditingHandler.setDateTimeProvider(() -> Optional.of(endOfYesterday));
		recordService.save(
			new RecordSaveRequest(1, 1, 100, 1.00, 1, new RecordSaveRequest.AvgPace(1, 1), true), member1.getId());

		LocalDateTime startOfToday = getStartOfDay(today);
		auditingHandler.setDateTimeProvider(() -> Optional.of(startOfToday));
		recordService.save(new RecordSaveRequest(1, 1, 200, 2.00, 1, new RecordSaveRequest.AvgPace(1, 1), true),
			member2.getId());
		recordService.save(new RecordSaveRequest(1, 1, 200, 2.00, 1, new RecordSaveRequest.AvgPace(1, 1), true),
			member1.getId());

		LocalDateTime endOfToday = getEndOfDay(today);
		auditingHandler.setDateTimeProvider(() -> Optional.of(endOfToday));
		recordService.save(new RecordSaveRequest(3, 3, 300, 3.00, 3, new RecordSaveRequest.AvgPace(1, 1), true),
			member1.getId());
		recordService.save(new RecordSaveRequest(3, 3, 100, 3.00, 3, new RecordSaveRequest.AvgPace(1, 1), false),
			member3.getId());

		LocalDateTime startOfTomorrow = getStartOfDay(today.plusDays(1));
		auditingHandler.setDateTimeProvider(() -> Optional.of(startOfTomorrow));
		recordService.save(new RecordSaveRequest(3, 3, 4000, 4.00, 3, new RecordSaveRequest.AvgPace(1, 1), true),
			member1.getId());

		// when
		List<RunningStatusResponse> response = recordService.findAllRunningStatus(member1.getId(), crew.getId());

		// then
		assertThat(response).hasSize(3)
			.extracting("memberId", "runTime", "runDistance", "isEnd")
			.containsExactly(
				tuple(member3.getId(), 100, 3.00, false),
				tuple(member1.getId(), 500, 5.00, true),
				tuple(member2.getId(), 200, 2.00, true)
			);
	}

	private Member createMember(String nickname) {
		return Member.builder()
			.nickname(nickname)
			.profileImg(
				"https://d2u3dcdbebyaiu.cloudfront.net/uploads/atch_img/321/d4c6b40843af7f4f1d8000cafef4a2e7.jpeg")
			.age(3)
			.gender(Gender.MALE)
			.avgPace(600)
			.totalDistance(3000)
			.build();
	}

	private Record createRecord(Member member, int avgPace, double runDistance, int runTime, int calories,
		LocalDateTime endTime) {
		return Record.builder()
			.member(member)
			.avgPace(avgPace)
			.runDistance(runDistance)
			.runTime(runTime)
			.calories(calories)
			.endTime(endTime)
			.build();
	}

	private Coordinate createCoordinate(Record record, double latitude, double longitude) {
		return Coordinate.builder()
			.record(record)
			.latitude(latitude)
			.longitude(longitude)
			.build();
	}

}
