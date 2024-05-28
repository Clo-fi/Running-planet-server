package clofi.runningplanet.crew.domain;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import clofi.runningplanet.common.domain.BaseSoftDeleteEntity;
import clofi.runningplanet.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@SQLDelete(sql = "update crew_member set deleted_at = now() where crew_member_id = ?")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class CrewMember extends BaseSoftDeleteEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "crew_member_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "crew_id", nullable = false)
	private Crew crew;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 10)
	private Role role;

	@Builder
	public CrewMember(Long id, Crew crew, Member member, Role role) {
		this.id = id;
		this.crew = crew;
		this.member = member;
		this.role = role;
	}

	public static CrewMember createLeader(Crew crew, Member member) {
		return new CrewMember(null, crew, member, Role.LEADER);
	}

	public static CrewMember of(Crew crew, Member member, Role role) {
		return new CrewMember(null, crew, member, role);
	}

	public static CrewMember createMember(Crew crew, Member member) {
		return new CrewMember(null, crew, member, Role.MEMBER);
	}

	public boolean isLeader() {
		return role == Role.LEADER;
	}

	public void validateMembership(Long crewId) {
		if (!this.crew.getId().equals(crewId)) {
			throw new IllegalArgumentException("크루에 속해있는 사용자만 이용할 수 있습니다.");
		}
	}

	public void checkLeaderPrivilege() {
		if (!isLeader()) {
			throw new IllegalArgumentException("리더만 사용할 수 있는 기능입니다.");
		}
	}
}
