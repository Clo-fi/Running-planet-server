package clofi.runningplanet.board.core.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import clofi.runningplanet.board.comment.repository.CommentRepository;
import clofi.runningplanet.board.core.dto.response.BoardDetailResponse;
import clofi.runningplanet.board.core.repository.BoardRepository;
import clofi.runningplanet.board.domain.Board;
import clofi.runningplanet.board.domain.Comment;
import clofi.runningplanet.crew.domain.ApprovalType;
import clofi.runningplanet.crew.domain.Category;
import clofi.runningplanet.crew.domain.Crew;
import clofi.runningplanet.crew.repository.CrewRepository;
import clofi.runningplanet.member.domain.Gender;
import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.member.repository.MemberRepository;

@SpringBootTest
class BoardReadServiceTest {
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private CrewRepository crewRepository;
	@Autowired
	private BoardRepository boardRepository;
	@Autowired
	private CommentRepository commentRepository;
	@Autowired
	private BoardReadService boardReadService;

	@AfterEach
	void tearDown() {
		commentRepository.deleteAllInBatch();
		boardRepository.deleteAllInBatch();
		crewRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@DisplayName("사용자는 게시글 리스트를 조회 할 수 있다.")
	@Test
	void getBoardDetail() {
		//given
		Member member = new Member(null, "테스트", Gender.MALE, 10, "테스트", 10, 10, 10, 10);
		Member commentMember = new Member(null, "댓글 테스트", Gender.MALE, 10, "테스트", 10, 10, 10, 10);
		memberRepository.save(member);
		memberRepository.save(commentMember);
		Crew crewInstance = new Crew(1L, "테스트", 10, 10, Category.RUNNING, ApprovalType.AUTO, "테스트", 10, 10);
		Crew crew = crewRepository.save(crewInstance);
		Board boardInstance = new Board("기존 게시글 제목", "기존 게시글 내용", crew, member);
		Board board = boardRepository.save(boardInstance);
		Comment commentInstance = new Comment(board, "댓글", commentMember);
		Comment comment = commentRepository.save(commentInstance);
		//when
		BoardDetailResponse boardDetail = boardReadService.getBoardDetail(crew.getId(), board.getId(),
			member.getId());
		//then
		assertThat(boardDetail.getBoardResponse().getAuthor()).isEqualTo("테스트");
		assertThat(boardDetail.getComments().get(0).getAuthor()).isEqualTo("댓글 테스트");
		assertThat(boardDetail.getComments().get(0).getIsModified()).isFalse();
		assertThat(boardDetail.getBoardResponse().getCommentCnt()).isEqualTo(1);
	}
}
