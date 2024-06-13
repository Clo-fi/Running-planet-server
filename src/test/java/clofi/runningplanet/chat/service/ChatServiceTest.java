package clofi.runningplanet.chat.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import clofi.runningplanet.chat.domain.Chat;
import clofi.runningplanet.chat.dto.response.ChatListResponse;
import clofi.runningplanet.chat.repository.ChatRepository;
import clofi.runningplanet.crew.domain.ApprovalType;
import clofi.runningplanet.crew.domain.Category;
import clofi.runningplanet.crew.domain.Crew;
import clofi.runningplanet.crew.domain.CrewMember;
import clofi.runningplanet.crew.domain.Role;
import clofi.runningplanet.crew.repository.CrewMemberRepository;
import clofi.runningplanet.crew.repository.CrewRepository;
import clofi.runningplanet.member.domain.Gender;
import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.member.repository.MemberRepository;

@SpringBootTest
class ChatServiceTest {

	@Autowired
	ChatService chatService;

	@Autowired
	ChatRepository chatRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	CrewRepository crewRepository;

	@Autowired
	CrewMemberRepository crewMemberRepository;

	@AfterEach
	void tearDown() {
		chatRepository.deleteAllInBatch();
		crewMemberRepository.deleteAllInBatch();
		crewRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@DisplayName("등록된 채팅을 불러올 수 있다.")
	@Test
	void getChatMessagesTest() {
		//given
		Member member1 = new Member(null, "turtle", Gender.MALE, 20, 100, "profileImg1", 10, 300, 250, 1000);
		Member member2 = new Member(null, "rabbit", Gender.FEMALE, 25, 70, "profileImg2", 10, 600, 250, 1000);

		memberRepository.save(member1);
		memberRepository.save(member2);

		Crew crew = new Crew(member1.getId(), "crew1", 10, Category.RUNNING, ApprovalType.AUTO, "introduction", 7, 1);

		crewRepository.save(crew);

		CrewMember crewMember1 = new CrewMember(null, crew, member1, Role.LEADER);
		CrewMember crewMember2 = new CrewMember(null, crew, member2, Role.MEMBER);

		crewMemberRepository.save(crewMember1);
		crewMemberRepository.save(crewMember2);

		Chat chat1 = new Chat(null, member1, crew, "I want your liver");
		Chat chat2 = new Chat(null, member2, crew, "I don't have it. I'll bring it");

		chatRepository.save(chat1);
		chatRepository.save(chat2);

		//when
		ChatListResponse chatList = chatService.getChatMessages(member1.getId(), crew.getId(), 0, 10);
		ChatListResponse chatList2 = chatService.getChatMessages(member2.getId(), crew.getId(), 0, 1);

		//then
		assertThat(chatList.chatArray().size()).isEqualTo(2);

		assertThat(chatList.chatArray().get(0).from()).isEqualTo("rabbit");
		assertThat(chatList.chatArray().get(0).message()).isEqualTo("I don't have it. I'll bring it");

		assertThat(chatList.chatArray().get(1).from()).isEqualTo("turtle");
		assertThat(chatList.chatArray().get(1).message()).isEqualTo("I want your liver");

		assertThat(chatList.existsNestPage()).isFalse();
		assertThat(chatList2.existsNestPage()).isTrue();
	}
}