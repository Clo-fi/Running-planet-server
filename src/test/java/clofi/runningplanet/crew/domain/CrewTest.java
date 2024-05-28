package clofi.runningplanet.crew.domain;

import static clofi.runningplanet.crew.domain.ApprovalType.*;
import static clofi.runningplanet.crew.domain.Category.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CrewTest {

	@DisplayName("크루 제한 운동점수보다 같거나 높은 운동점수가 들어올 경우 통과")
	@ParameterizedTest()
	@ValueSource(ints = {50, 70, 100})
	void successUpperRunScore(int runScore) {
		//given
		Crew crew = new Crew(1L, 2L, "구름 크루", 10, 50,
			RUNNING, MANUAL, "구름 크루는 성실한 크루", 5, 100,
			0, 0);

		//when
		//then
		assertDoesNotThrow(() -> crew.checkRunScore(runScore));
	}

	@DisplayName("크루 제한 운동점수보다 낮은 운동점수가 들어올 경우 예외 발생")
	@ParameterizedTest()
	@ValueSource(ints = {0, 49, 30})
	void failEqualOrLowerRunScore(int runScore) {
		//given
		Crew crew = new Crew(1L, 2L, "구름 크루", 10, 50,
			RUNNING, MANUAL, "구름 크루는 성실한 크루", 5, 100,
			0, 0);

		//when
		//then
		assertThatThrownBy(() -> crew.checkRunScore(runScore))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@DisplayName("크루 제한 인원수 검증 통과")
	@ParameterizedTest()
	@ValueSource(ints = {1, 5, 9})
	void successCheckReachedMemberLimit(int currentMemberCnt) {
		//given
		Crew crew = new Crew(1L, 2L, "구름 크루", 10, 50,
			RUNNING, MANUAL, "구름 크루는 성실한 크루", 5, 100,
			0, 0);

		//when
		boolean result = crew.checkReachedMemberLimit(currentMemberCnt);

		//then
		assertThat(result).isFalse();
	}

	@DisplayName("크루 제한 인원수 검증 통과")
	@ParameterizedTest()
	@ValueSource(ints = {10, 11, 15, 100})
	void failCheckReachedMemberLimit(int currentMemberCnt) {
		//given
		Crew crew = new Crew(1L, 2L, "구름 크루", 10, 50,
			RUNNING, MANUAL, "구름 크루는 성실한 크루", 5, 100,
			0, 0);

		//when
		boolean result = crew.checkReachedMemberLimit(currentMemberCnt);

		//then
		assertThat(result).isTrue();
	}
}
