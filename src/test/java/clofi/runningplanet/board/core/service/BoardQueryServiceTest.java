package clofi.runningplanet.board.core.service;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import clofi.runningplanet.board.core.dto.request.CreateBoardRequest;
import clofi.runningplanet.board.core.dto.request.UpdateBoardRequest;
import clofi.runningplanet.board.core.dto.response.CreateBoardResponse;
import clofi.runningplanet.board.core.factory.BoardFactory;
import clofi.runningplanet.common.FakeS3StorageManager;
import clofi.runningplanet.board.core.repository.BoardImageRepository;
import clofi.runningplanet.board.core.repository.BoardRepository;
import clofi.runningplanet.board.domain.Board;
import clofi.runningplanet.board.domain.BoardImage;
import clofi.runningplanet.crew.domain.ApprovalType;
import clofi.runningplanet.crew.domain.Category;
import clofi.runningplanet.crew.domain.Crew;
import clofi.runningplanet.crew.repository.CrewRepository;
import clofi.runningplanet.member.domain.Gender;
import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.member.repository.MemberRepository;

@SpringBootTest
class BoardQueryServiceTest {

	@Autowired
	private BoardRepository boardRepository;
	@Autowired
	private BoardImageRepository boardImageRepository;
	@Autowired
	private CrewRepository crewRepository;
	@Autowired
	private MemberRepository memberRepository;

	@AfterEach
	void tearDown() {
		boardImageRepository.deleteAllInBatch();
		boardRepository.deleteAllInBatch();
		crewRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@DisplayName("사용자는 게시글을 작성할 수 있다.")
	@Test
	void createBoardTest() {
		//given
		Member memberInstance = new Member(null, "테스트", Gender.FEMALE, 10, "테스트", 10, 10, 10, 10);
		Member member = memberRepository.save(memberInstance);
		Crew crewInstance = new Crew(1L, "테스트", 10, 10, Category.RUNNING, ApprovalType.AUTO, "테스트", 10, 10);
		Crew crew = crewRepository.save(crewInstance);
		CreateBoardRequest createBoardRequest = new CreateBoardRequest("게시판 제목", "게시판 내용");
		List<MultipartFile> imageFile = getImageFile();
		BoardQueryService boardQueryService = getBoardService();

		//when
		CreateBoardResponse createBoardResponse = boardQueryService.create(crew.getId(), createBoardRequest, imageFile,
			member.getId());
		Board board = boardRepository.findById(createBoardResponse.boardId())
			.orElseThrow(() -> new IllegalArgumentException("게시판이 없습니다"));
		List<BoardImage> boardImage = boardImageRepository.findAllByBoard(board);
		//then
		assertThat(board.getId()).isEqualTo(createBoardResponse.boardId());
		assertThat(board.getTitle()).isEqualTo("게시판 제목");
		assertThat(board.getContent()).isEqualTo("게시판 내용");
		assertThat(boardImage.stream().map(BoardImage::getImageUrl).collect(Collectors.toList()))
			.containsExactlyInAnyOrder("fakeImageUrl1", "fakeImageUrl2");
	}

	@DisplayName("게시글을 수정할 수 있다.")
	@Test
	@Transactional
	void updateTest() {
		//given
		Member memberInstance = new Member(null, "테스트", Gender.FEMALE, 10, "테스트", 10, 10, 10, 10);
		Member member = memberRepository.save(memberInstance);
		Crew crewInstance = new Crew(1L, "테스트", 10, 10, Category.RUNNING, ApprovalType.AUTO, "테스트", 10, 10);
		Crew crew = crewRepository.save(crewInstance);

		Board board = new Board("기존 게시글 제목", "기존 게시글 내용", crew, member);
		boardRepository.save(board);

		BoardImage boardImage = new BoardImage(board, "기존 이미지 주소");
		boardImageRepository.save(boardImage);

		List<MultipartFile> imageFile = getImageFile();

		UpdateBoardRequest updateBoardRequest = new UpdateBoardRequest("업데이트 게시글 제목", "업데이트 게시글 내용");
		BoardQueryService boardService = getBoardService();
		//when
		boardService.update(crew.getId(), board.getId(), updateBoardRequest, imageFile, member.getId());
		List<BoardImage> updateImage = boardImageRepository.findAllByBoard(board);
		//then
		assertThat(board.getContent()).isEqualTo("업데이트 게시글 내용");
		assertThat(board.getTitle()).isEqualTo("업데이트 게시글 제목");
		assertThat(
			updateImage.stream().map(BoardImage::getImageUrl).collect(Collectors.toList())).containsExactlyInAnyOrder(
			"fakeImageUrl1", "fakeImageUrl2");

	}

	private List<MultipartFile> getImageFile() {

		return Arrays.asList(
			new MockMultipartFile(
				"image1", // 파일 파라미터 이름
				"image1.jpg", // 파일명
				"image/jpeg", // 컨텐츠 타입
				"이미지_콘텐츠1".getBytes() // 파일 콘텐츠
			),
			new MockMultipartFile(
				"image2", // 파일 파라미터 이름
				"image2.jpg", // 파일명
				"image/jpeg", // 컨텐츠 타입
				"이미지_콘텐츠2".getBytes() // 파일 콘텐츠
			)
		);
	}

	private BoardQueryService getBoardService() {
		FakeS3StorageManager fakeS3StorageManager = new FakeS3StorageManager();
		return new BoardQueryService(
			new BoardFactory(
				boardRepository,
				boardImageRepository,
				fakeS3StorageManager
			),
			crewRepository,
			boardRepository,
			memberRepository,
			fakeS3StorageManager
		);
	}
}