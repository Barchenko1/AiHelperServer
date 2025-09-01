package com.helper.server.repository;

import com.helper.server.modal.Chat;
import com.helper.server.modal.ChatEntry;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
//    @Override
//    default List<String> findConversationIds() {
//        return findAll().stream()
//                .map(Chat::getId)
//                .map(String::valueOf)
//                .toList();
//    }
//
//    @Override
//    default List<Message> findByConversationId(String conversationId) {
//        Chat chat = findById(Long.valueOf(conversationId)).orElseThrow();
//        return chat.getHistory().stream()
//                .map(ChatEntry::toMessage)
//                .toList();
//    }
//
//    @Override
//    default void saveAll(String conversationId, List<Message> messages) {
//        Chat chat = findById(Long.valueOf(conversationId)).orElseThrow();
//        messages.stream().map(ChatEntry::toChatEntry).forEach(chat::addEntry);
//        save(chat);
//    }
//
//    @Override
//    default void deleteByConversationId(String conversationId) {
//// not implemented
//    }
}
