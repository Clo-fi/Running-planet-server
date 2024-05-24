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
		Member member = memberRepository.findByNickname(username);
		Record record = recordRepository.findOneByMemberAndEndTimeIsNull(member)
			.orElse(Record.builder().member(member).build());

		record.update(request.runTime(), request.runDistance(), request.calories(), request.avgPace().min(),
			request.avgPace().sec());

		Record savedRecord = recordRepository.save(record);

		savedRecord.end(request.isEnd(), LocalDateTime.now());

		Coordinate coordinate = Coordinate.builder()
			.record(savedRecord)
			.latitude(request.latitude())
			.longitude(request.longitude())
			.build();

		coordinateRepository.save(coordinate);

		return savedRecord;
	}

	public List<RecordFindAllResponse> findAll(Integer year, Integer month, String username) {
		Member member = memberRepository.findByNickname(username);
		YearMonth yearMonth = YearMonth.of(year, month);
		LocalDateTime startDateTime = yearMonth.atDay(1).atStartOfDay();
		LocalDateTime endDateTime = yearMonth.atEndOfMonth().atTime(23, 59, 59);
		List<Record> records = recordRepository
			.findAllByMemberAndCreatedAtBetweenAndEndTimeIsNotNull(member, startDateTime, endDateTime);

		return records.stream()
			.map(RecordFindAllResponse::new)
			.toList();
	}

	public RecordFindResponse find(Long recordId, String username) {
		Member member = memberRepository.findByNickname(username);
		Record record = recordRepository.findByIdAndMemberAndEndTimeIsNotNull(recordId, member)
			.orElseThrow(() -> new IllegalArgumentException("운동 기록을 찾을 수 없습니다."));

		List<Coordinate> coordinates = coordinateRepository.findAllByRecord(record);

		return new RecordFindResponse(record, coordinates);
	}

	public RecordFindCurrentResponse findCurrentRecord(String username) {
		Member member = memberRepository.findByNickname(username);
		Optional<Record> optionalRecord = recordRepository.findOneByMemberAndEndTimeIsNull(member);
		if (optionalRecord.isEmpty()) {
			return null;
		}
		Record record = optionalRecord.get();
		Coordinate coordinate = coordinateRepository.findFirstByRecordOrderByCreatedAtDesc(record)
			.orElseThrow(() -> new IllegalArgumentException("좌표 정보를 찾을 수 없습니다."));

		return new RecordFindCurrentResponse(record, coordinate);
	}

}
