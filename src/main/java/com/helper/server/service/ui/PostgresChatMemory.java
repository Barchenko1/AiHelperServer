package com.helper.server.service.ui;

import com.helper.server.modal.Chat;
import com.helper.server.modal.ChatEntry;
import com.helper.server.repository.ChatRepository;
import lombok.Builder;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.Comparator;
import java.util.List;

@Builder
public class PostgresChatMemory implements ChatMemory {

    private final ChatRepository chatMemoryRepository;
    private final int maxMessages;

    @Override
    public void add(String conversationId, List<Message> messages) {
        Chat chat = chatMemoryRepository.findById(Long.valueOf(conversationId)).orElseThrow();
       for (Message message : messages) {
           chat.addEntry(ChatEntry.toChatEntry(message));
       }
        chatMemoryRepository.save(chat);
    }

    @Override
    public List<Message> get(String conversationId) {
        Chat chat = chatMemoryRepository.findById(Long.valueOf(conversationId)).orElseThrow();
        int messagesToSkip = Math.max(0, chat.getHistory().size() - maxMessages);
        return chat.getHistory().stream()
//                .sorted(Comparator.comparing(ChatEntry::getCreatedAt))
                .skip(messagesToSkip)
                .map(ChatEntry::toMessage)
                .limit(maxMessages)
                .toList();
    }



    @Override
    public void clear(String conversationId) {
        //no need
    }
}
