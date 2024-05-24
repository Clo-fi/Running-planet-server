package clofi.runningplanet.running.domain;

import java.time.LocalDateTime;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@SQLDelete(sql = "update record set deleted_at = now() where record_id = ?")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Record extends BaseSoftDeleteEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "record_id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "run_time", nullable = false)
	private int runTime;

	@Column(name = "run_distance", nullable = false)
	private double runDistance;

	@Column(name = "calories", nullable = false)
	private int calories;

	@Column(name = "avg_pace", nullable = false)
	private int avgPace;

	@Column(name = "end_time")
	private LocalDateTime endTime;

	@Column(name = "is_end")
	private boolean isEnd;

	@Builder
	private Record(Member member, int runTime, double runDistance, int calories, int avgPace, LocalDateTime endTime) {
		this.member = member;
		this.runTime = runTime;
		this.runDistance = runDistance;
		this.calories = calories;
		this.avgPace = avgPace;
		this.endTime = endTime;
	}

	public void update(int runTime, double runDistance, int calories, int min, int sec, boolean isEnd) {
		this.runTime = runTime;
		this.runDistance = runDistance;
		this.calories = calories;
		this.avgPace = min * 60 + sec;
		this.isEnd = isEnd;
	}

	@PrePersist
	@PreUpdate
	protected void onUpdate() {
		if (this.isEnd) {
			this.endTime = LocalDateTime.now();
		}
	}
}
