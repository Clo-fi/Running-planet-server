package clofi.runningplanet.crew.domain;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import clofi.runningplanet.common.domain.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@SQLDelete(sql = "update crew set deleted_at = now() where crew_id = ?")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Crew extends BaseSoftDeleteEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "crew_id", nullable = false)
	private Long id;

	@Column(name = "leader_id", nullable = false)
	private Long leaderId;

	@Column(name = "crew_name", nullable = false)
	private String crewName;

	@Column(name = "limit_member_cnt", nullable = false)
	private int limitMemberCnt;

	@Column(name = "limit_run_score", nullable = false)
	private int limitRunScore;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 10)
	private Category category;

	@Enumerated(EnumType.STRING)
	@Column(name = "approval_type", nullable = false, length = 10)
	private ApprovalType approvalType;

	@Column(name = "introduction", length = 4000)
	private String introduction;

	@Column(name = "rule_run_cnt", nullable = false)
	private int ruleRunCnt;

	@Column(name = "rule_distance", nullable = false)
	private int ruleDistance;

	@Column(name = "weekly_distance", nullable = false)
	private int weeklyDistance;

	@Column(name = "total_distance", nullable = false)
	private int totalDistance;

	public Crew(Long leaderId, String crewName, int limitMemberCnt, int limitRunScore, Category category,
		ApprovalType approvalType,
		String introduction, int ruleRunCnt, int ruleDistance) {
		this(null, leaderId, crewName, limitMemberCnt, limitRunScore, category, approvalType, introduction, ruleRunCnt,
			ruleDistance, 0, 0);
	}

	public Crew(Long id, Long leaderId, String crewName, int limitMemberCnt, int limitRunScore, Category category,
		ApprovalType approvalType, String introduction, int ruleRunCnt, int ruleDistance, int weeklyDistance,
		int totalDistance) {
		this.id = id;
		this.leaderId = leaderId;
		this.crewName = crewName;
		this.limitMemberCnt = limitMemberCnt;
		this.limitRunScore = limitRunScore;
		this.category = category;
		this.approvalType = approvalType;
		this.introduction = introduction;
		this.ruleRunCnt = ruleRunCnt;
		this.ruleDistance = ruleDistance;
		this.weeklyDistance = weeklyDistance;
		this.totalDistance = totalDistance;
	}

	public void checkRunScore(int runScore) {
		if (this.limitRunScore > runScore) {
			throw new IllegalArgumentException("운동 점수가 제한 운동 점수보다 낮아 지원할 수 없습니다.");
		}
	}

	public boolean checkReachedMemberLimit(int currentMemberCnt) {
		return currentMemberCnt >= limitMemberCnt;
	}
}
