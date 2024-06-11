package clofi.runningplanet.running.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import clofi.runningplanet.crew.repository.CrewMemberRepository;
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
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class RecordService {
	private final RecordRepository recordRepository;
	private final CoordinateRepository coordinateRepository;
	private final MemberRepository memberRepository;
	private final CrewMemberRepository crewMemberRepository;
	private final SimpMessagingTemplate messagingTemplate;

	@Transactional
	public Record save(RecordSaveRequest request, Long memberId) {
		Member member = getMember(memberId);
		Record record = getCurrentRecordOrElseNew(member);

		record.update(request.runTime(), request.runDistance(), request.calories(), request.avgPace().min(),
			request.avgPace().sec(), request.isEnd());

		Record savedRecord = recordRepository.save(record);

		Coordinate coordinate = request.toCoordinate(savedRecord);
		coordinateRepository.save(coordinate);

		sendRunningStatus(member, savedRecord);

		return savedRecord;
	}

	private Member getMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
	}

	private Record getCurrentRecordOrElseNew(Member member) {
		return recordRepository.findOneByMemberAndEndTimeIsNull(member)
			.orElse(Record.builder().member(member).build());
	}

	private void sendRunningStatus(Member member, Record savedRecord) {
		crewMemberRepository.findByMemberId(member.getId())
			.ifPresent(crewMember -> messagingTemplate.convertAndSend(
				String.format("/sub/crew/%s/running", crewMember.getCrew().getId()),
				new RunningStatusResponse(member, savedRecord)));
	}

	public List<RecordFindAllResponse> findAll(Integer year, Integer month, Long memberId) {
		Member member = getMember(memberId);
		YearMonth yearMonth = YearMonth.of(year, month);

		LocalDateTime startDateTime = getStartDateTime(yearMonth);
		LocalDateTime endDateTime = getEndDateTime(yearMonth);
		List<Record> records = recordRepository.findAllByMemberAndCreatedAtBetweenAndEndTimeIsNotNull(member,
			startDateTime, endDateTime);

		return records.stream()
			.map(RecordFindAllResponse::new)
			.toList();
	}

	private LocalDateTime getStartDateTime(YearMonth yearMonth) {
		return yearMonth.atDay(1).atStartOfDay();
	}

	private static LocalDateTime getEndDateTime(YearMonth yearMonth) {
		return yearMonth.atEndOfMonth().atTime(23, 59, 59);
	}

	public RecordFindResponse find(Long recordId, Long memberId) {
		Member member = getMember(memberId);
		Record record = getCurrentRecord(recordId, member);
		List<Coordinate> coordinates = coordinateRepository.findAllByRecord(record);

		return new RecordFindResponse(record, coordinates);
	}

	private Record getCurrentRecord(Long recordId, Member member) {
		return recordRepository.findByIdAndMemberAndEndTimeIsNotNull(recordId, member)
			.orElseThrow(() -> new IllegalArgumentException("운동 기록을 찾을 수 없습니다."));
	}

	public RecordFindCurrentResponse findCurrentRecord(Long memberId) {
		Member member = getMember(memberId);
		Optional<Record> optionalRecord = recordRepository.findOneByMemberAndEndTimeIsNull(member);
		if (optionalRecord.isEmpty()) {
			return null;
		}
		Record record = optionalRecord.get();
		Coordinate coordinate = getLastCoordinate(record);

		return new RecordFindCurrentResponse(record, coordinate);
	}

	private Coordinate getLastCoordinate(Record record) {
		return coordinateRepository.findFirstByRecordOrderByCreatedAtDesc(record)
			.orElseThrow(() -> new IllegalArgumentException("좌표 정보를 찾을 수 없습니다."));
	}

	@Transactional
	public List<RunningStatusResponse> findAllRunningStatus(Long memberId, Long crewId) {
		if (!crewMemberRepository.existsByCrewIdAndMemberId(crewId, memberId)) {
			throw new IllegalArgumentException("크루에 소속된 회원이 아닙니다.");
		}

		List<Member> members = crewMemberRepository.findMembersByCrewId(crewId);
		LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
		LocalDateTime startOfTomorrow = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN);

		List<Record> records = recordRepository.findRunningRecordsByMembersAndDateRange(members, startOfToday,
			startOfTomorrow);

		List<RunningStatusResponse> runningStatusResponses = convertToRunningStatusResponses(records);
		sortByIsEndAndRunTime(runningStatusResponses);

		return runningStatusResponses;
	}

	private List<RunningStatusResponse> convertToRunningStatusResponses(List<Record> records) {
		Map<Long, List<Record>> groupedByMemberId = records.stream()
			.collect(Collectors.groupingBy(record -> record.getMember().getId()));

		return groupedByMemberId.values().stream()
			.map(recordList ->
				new RunningStatusResponse(
					recordList.getFirst().getMember().getId(),
					recordList.getFirst().getMember().getNickname(),
					recordList.stream().mapToInt(Record::getRunTime).sum(),
					recordList.stream().mapToDouble(Record::getRunDistance).sum(),
					recordList.stream().allMatch(r -> r.getEndTime() != null)
				)
			)
			.collect(Collectors.toList());
	}

	private void sortByIsEndAndRunTime(List<RunningStatusResponse> runningStatusResponses) {
		runningStatusResponses.sort((r1, r2) -> {
			if (r1.isEnd() != r2.isEnd()) {
				return Boolean.compare(r1.isEnd(), r2.isEnd());
			}
			return Integer.compare(r2.runTime(), r1.runTime());
		});
	}
}
