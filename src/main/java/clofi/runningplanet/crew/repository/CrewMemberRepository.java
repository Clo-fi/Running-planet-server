package clofi.runningplanet.crew.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import clofi.runningplanet.crew.domain.CrewMember;

@Repository
public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {
	Optional<CrewMember> findByMemberId(Long id);

	boolean existsByMemberId(Long memberId);

	List<CrewMember> findByCrewId(Long crewId);

	int countByCrewId(Long id);
}
