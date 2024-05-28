package clofi.runningplanet.board.thumbsUp.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import clofi.runningplanet.board.core.repository.BoardRepository;
import clofi.runningplanet.board.domain.Board;
import clofi.runningplanet.board.domain.ThumbsUp;
import clofi.runningplanet.board.thumbsUp.repository.ThumbsUpRepository;
import clofi.runningplanet.crew.domain.ApprovalType;
import clofi.runningplanet.crew.domain.Category;
import clofi.runningplanet.crew.domain.Crew;
import clofi.runningplanet.crew.repository.CrewRepository;
import clofi.runningplanet.member.domain.Gender;
import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.member.repository.MemberRepository;

@SpringBootTest
class ThumbsUpServiceTest {
	@Autowired
	private ThumbsUpService thumbsUpService;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private CrewRepository crewRepository;
	@Autowired
	private BoardRepository boardRepository;
	@Autowired
	private ThumbsUpRepository thumbsUpRepository;

	@AfterEach
	void tearDown() {
		thumbsUpRepository.deleteAllInBatch();
		boardRepository.deleteAllInBatch();
		crewRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@DisplayName("사용자는 좋아요를 표시할 수 있다.")
	@Test
	void thumbsUpTest() {
		//given
		Member member = new Member(null, "테스트", Gender.FEMALE, 10, "테스트", 10, 10, 10, 10);
		Member memberId = memberRepository.save(member);
		Crew crewInstance = new Crew(1L, "테스트", 10, 10, Category.RUNNING, ApprovalType.AUTO, "테스트", 10, 10);
		Crew crew = crewRepository.save(crewInstance);
		Board boardInstance = new Board("테스트", "테스트", crew, memberId);
		Board board = boardRepository.save(boardInstance);
		//when
		thumbsUpService.createLike(crew.getId(), board.getId(), memberId.getId());
		Boolean isThumbsUp = thumbsUpRepository.existsByMemberAndBoard(memberId, board);
		//then
		assertThat(isThumbsUp).isTrue();
	}

	@DisplayName("좋아요를 한번 더 누르면 좋아요가 취소된다")
	@Test
	void cancelThumbsUpTest() {
		//given
		Member member = new Member(null, "테스트", Gender.FEMALE, 10, "테스트", 10, 10, 10, 10);
		Member memberId = memberRepository.save(member);
		Crew crewInstance = new Crew(1L, "테스트", 10, 10, Category.RUNNING, ApprovalType.AUTO, "테스트", 10, 10);
		Crew crew = crewRepository.save(crewInstance);
		Board boardInstance = new Board("테스트", "테스트", crew, memberId);
		Board board = boardRepository.save(boardInstance);
		ThumbsUp thumbsUp = new ThumbsUp(board, member);
		thumbsUpRepository.save(thumbsUp);
		//when
		thumbsUpService.createLike(crew.getId(), board.getId(), memberId.getId());
		//then
		assertThat(thumbsUpRepository.existsByMemberAndBoard(memberId, board)).isFalse();
	}
}