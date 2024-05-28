package clofi.runningplanet.crew.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CrewApplicationTest {

	@DisplayName("크루 가입 대기 상태에서 다시 신청할 경우 예외 발생")
	@Test
	void failApplyByPending() {
		//given
		CrewApplication crewApplication = new CrewApplication(1L, "크루 신청서", Approval.PENDING, null, null);

		//when
		//then
		assertThatThrownBy(crewApplication::checkDuplicateApply)
			.isInstanceOf(IllegalArgumentException.class);

	}

	@DisplayName("크루 가입 승인 메서드 테스트")
	@Test
	void successApprove() {
		//given
		CrewApplication crewApplication = new CrewApplication(1L, "크루 신청서", Approval.PENDING, null, null);

		//when
		crewApplication.approve();

		//then
		assertThat(crewApplication.getApproval()).isEqualTo(Approval.APPROVE);
	}

	@DisplayName("크루 가입 거절 메서드 테스트")
	@Test
	void successReject() {
		//given
		CrewApplication crewApplication = new CrewApplication(1L, "크루 신청서", Approval.PENDING, null, null);

		//when
		crewApplication.reject();

		//then
		assertThat(crewApplication.getApproval()).isEqualTo(Approval.REJECT);
	}
}
