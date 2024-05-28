package clofi.runningplanet.crew.domain;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import clofi.runningplanet.common.domain.BaseSoftDeleteEntity;
import clofi.runningplanet.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@SQLDelete(sql = "update crew set deleted_at = now() where crew_application_id = ?")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class CrewApplication extends BaseSoftDeleteEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "introduction")
	private String introduction;

	@Column(name = "approval")
	private Approval approval;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "crew_id")
	private Crew crew;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	public CrewApplication(Long id, String introduction, Approval approval, Crew crew, Member member) {
		this.id = id;
		this.introduction = introduction;
		this.approval = approval;
		this.crew = crew;
		this.member = member;
	}

	public CrewApplication(String introduction, Crew crew, Member member) {
		this(null, introduction, Approval.PENDING, crew, member);
	}

	public void checkDuplicateApply() {
		if (this.approval == Approval.PENDING) {
			throw new IllegalArgumentException("이미 신청한 크루입니다.");
		}
	}

	public void approve() {
		this.approval = Approval.APPROVE;
	}

	public void reject() {
		this.approval = Approval.REJECT;
	}
}
