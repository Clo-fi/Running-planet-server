package clofi.runningplanet.running.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.member.repository.MemberRepository;
import clofi.runningplanet.running.domain.Coordinate;
import clofi.runningplanet.running.domain.Record;
import clofi.runningplanet.running.dto.RecordFindAllResponse;
import clofi.runningplanet.running.dto.RecordFindCurrentResponse;
import clofi.runningplanet.running.dto.RecordFindResponse;
import clofi.runningplanet.running.dto.RecordSaveRequest;
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

	@Transactional
	public Record save(RecordSaveRequest request, String username) {
		Member member = getMember(username);
		Record record = getCurrentRecordOrElseNew(member);

		record.update(request.runTime(), request.runDistance(), request.calories(), request.avgPace().min(),
			request.avgPace().sec(), request.isEnd());

		Record savedRecord = recordRepository.save(record);

		Coordinate coordinate = request.toCoordinate(savedRecord);
		coordinateRepository.save(coordinate);

		return savedRecord;
	}

	private Member getMember(String username) {
		return memberRepository.findByNickname(username);
	}

	private Record getCurrentRecordOrElseNew(Member member) {
		return recordRepository.findOneByMemberAndEndTimeIsNull(member)
			.orElse(Record.builder().member(member).build());
	}

	public List<RecordFindAllResponse> findAll(Integer year, Integer month, String username) {
		Member member = getMember(username);
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

	public RecordFindResponse find(Long recordId, String username) {
		Member member = getMember(username);
		Record record = getCurrentRecord(recordId, member);
		List<Coordinate> coordinates = coordinateRepository.findAllByRecord(record);

		return new RecordFindResponse(record, coordinates);
	}

	private Record getCurrentRecord(Long recordId, Member member) {
		return recordRepository.findByIdAndMemberAndEndTimeIsNotNull(recordId, member)
			.orElseThrow(() -> new IllegalArgumentException("운동 기록을 찾을 수 없습니다."));
	}

	public RecordFindCurrentResponse findCurrentRecord(String username) {
		Member member = getMember(username);
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

}
