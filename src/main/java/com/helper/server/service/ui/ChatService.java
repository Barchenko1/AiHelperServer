package com.helper.server.service.ui;

import com.helper.server.modal.Chat;
import com.helper.server.modal.ChatEntry;
import com.helper.server.modal.Role;
import com.helper.server.repository.ChatRepository;
import jakarta.transaction.Transactional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

import static com.helper.server.modal.Role.ASSISTANT;
import static com.helper.server.modal.Role.USER;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatClient chatClient;
    private final ChatService myProxy;

    @Autowired
    public ChatService(ChatRepository chatRepository,
                       ChatClient chatClient,
                       @Lazy ChatService myProxy) {
        this.chatRepository = chatRepository;
        this.chatClient = chatClient;
        this.myProxy = myProxy;
    }

    public List<Chat> getAllChats() {
        return chatRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Chat getChat(Long chatId) {
        return chatRepository.findById(chatId).orElseThrow();
    }

    public Chat createNewChat(String title) {
        Chat chat = Chat.builder()
                .title(title)
                .build();

        return chatRepository.save(chat);
    }

    public void deleteChat(Long chatId) {
        chatRepository.deleteById(chatId);
    }

    @Transactional
    public void proceedInteraction(Long chatId, String prompt) {
        myProxy.addChatEntry(chatId, prompt, USER);
        String answer = chatClient.prompt().user(prompt).call().content();
        myProxy.addChatEntry(chatId, answer, ASSISTANT);
    }

    @Transactional
    public void addChatEntry(Long chatId, String prompt, Role role) {
        Chat chat = chatRepository.findById(chatId).orElseThrow();
        ChatEntry chatEntry = ChatEntry.builder()
                .content(prompt)
                .role(role)
                .build();
        chat.addEntry(chatEntry);
    }

    public SseEmitter proceedInteractionWithStreaming(Long chatId, String prompt) {
//        myProxy.addChatEntry(chatId, prompt, USER);

        SseEmitter sseEmitter = new SseEmitter(0L);
        StringBuilder answer = new StringBuilder();
        chatClient.prompt(prompt)
//                .advisors(MessageChatMemoryAdvisor.builder(postgresChatMemory)
//                        .conversationId(String.valueOf(chatId)).build())
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .chatResponse()
                .subscribe(
                        (ChatResponse response) -> processToken(response, sseEmitter, answer),
                        sseEmitter::completeWithError,
                        sseEmitter::complete);
//                        () -> {
//                            try {
//                                myProxy.addChatEntry(chatId, answer.toString(), ASSISTANT);
//                                sseEmitter.send(SseEmitter.event().name("done").data("ok"));
//                            } catch (Exception saveEx) {
//                                saveEx.printStackTrace();
//                                try { sseEmitter.completeWithError(saveEx); } catch (Throwable ignored) {}
//                            } finally {
//                                try { sseEmitter.complete(); } catch (Throwable ignored) {}
//                            }
//                        });
        return sseEmitter;
    }

    private void processToken(ChatResponse response, SseEmitter sseEmitter, StringBuilder answer) {
        AssistantMessage token =  response.getResult().getOutput();
        try {
            sseEmitter.send(response.getResult());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        answer.append(token.getText());
    }
}
