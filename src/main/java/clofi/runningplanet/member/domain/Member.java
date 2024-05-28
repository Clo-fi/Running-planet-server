package clofi.runningplanet.member.domain;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@SQLDelete(sql = "update member set deleted_at = now() where member_id = ?")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Member extends BaseSoftDeleteEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id", nullable = false)
	private Long id;

	@Column(name = "nickname", length = 20)
	private String nickname;

	@Enumerated(value = EnumType.STRING)
	@Column(name = "gender", length = 6)
	private Gender gender;

	@Column(name = "age")
	private int age;

	@Column(name = "profile_img", nullable = false)
	private String profileImg;

	@Column(name = "run_score")
	private Integer runScore;

	@Column(name = "avg_pace")
	private Integer avgPace;

	@Column(name = "avg_distance")
	private Integer avgDistance;

	@Column(name = "total_distance", nullable = false)
	private int totalDistance;

	@Builder
	public Member(Long id, String nickname, Gender gender, int age, String profileImg, Integer runScore,
			Integer avgPace,
			Integer avgDistance, int totalDistance) {
		this.id = id;
		this.nickname = nickname;
		this.gender = gender;
		this.age = age;
		this.profileImg = profileImg;
		this.runScore = runScore;
		this.avgPace = avgPace;
		this.avgDistance = avgDistance;
		this.totalDistance = totalDistance;
	}

	public void update(String nickname, String profileImg) {
		this.nickname = nickname;
		this.profileImg = profileImg;
	}
}
