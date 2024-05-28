package clofi.runningplanet.board.comment.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import clofi.runningplanet.board.comment.dto.request.CreateCommentRequest;
import clofi.runningplanet.board.comment.repository.CommentRepository;
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
class CommentServiceTest {
	@Autowired
	private CommentService commentService;
	@Autowired
	private CrewRepository crewRepository;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private CommentRepository commentRepository;
	@Autowired
	private BoardRepository boardRepository;

	@AfterEach
	void tearDown() {
		commentRepository.deleteAllInBatch();
		boardRepository.deleteAllInBatch();
		crewRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@DisplayName("사용자는 댓글을 작성할 수 있다.")
	@Test
	void createCommentTest() {
		//given
		Member member = new Member(null, "테스트", Gender.MALE, 10, "테스트", 10, 10, 10, 10);
		memberRepository.save(member);
		Crew crewInstance = new Crew(1L, "테스트", 10, 10, Category.RUNNING, ApprovalType.AUTO, "테스트", 10, 10);
		Crew crew = crewRepository.save(crewInstance);
		Board boardInstance = new Board("기존 게시글 제목", "기존 게시글 내용", crew, member);
		Board board = boardRepository.save(boardInstance);
		CreateCommentRequest commentRequest = new CreateCommentRequest("댓글");
		//when
		Long comment = commentService.create(crew.getId(), board.getId(), commentRequest, member.getId());
		Comment saveComment = commentRepository.findById(comment)
			.orElseThrow(() -> new IllegalArgumentException("댓글이 없습니다."));
		//then
		Assertions.assertThat(saveComment.getContent()).isEqualTo("댓글");
	}

}