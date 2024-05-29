package clofi.runningplanet.member.dto.response;

import clofi.runningplanet.crew.domain.CrewMember;
import clofi.runningplanet.member.domain.Gender;
import clofi.runningplanet.member.domain.Member;

public record ProfileResponse(
	String nickname,

	Gender gender,

	Integer age,

	String profileImg,

	Integer runScore,

	AvgPace avgPace,

	Integer avgDistance,

	int totalDistance,

	String myCrew
) {
	public ProfileResponse(Member member, CrewMember crewMember) {
		this(member.getNickname(), member.getGender(), member.getAge(), member.getProfileImg(), member.getRunScore(),
			member.getAvgPace() != null ? calculateAvgPace(member.getAvgPace()) : null
			, member.getAvgDistance(), member.getTotalDistance(), crewMember != null? crewMember.getCrew().getCrewName() : null);
	}

	public record AvgPace(
		int min,

		int sec
	) {
	}

	private static AvgPace calculateAvgPace(int totalSec) {
		int min = totalSec / 60;
		int sec = totalSec % 60;
		return new AvgPace(min, sec);
	}
}
