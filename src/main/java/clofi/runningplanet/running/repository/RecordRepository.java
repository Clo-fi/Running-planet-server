package clofi.runningplanet.running.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.running.domain.Record;

public interface RecordRepository extends JpaRepository<Record, Long> {

	Optional<Record> findByIdAndMemberAndEndTimeIsNotNull(Long id, Member member);

	List<Record> findAllByMemberAndCreatedAtBetweenAndEndTimeIsNotNull(Member member, LocalDateTime start, LocalDateTime end);

	Optional<Record> findOneByMemberAndEndTimeIsNull(Member member);
}
