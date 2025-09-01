package com.helper.server.modal;

import lombok.Getter;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Arrays;

@Getter
public enum Role {
    USER("user") {
        @Override
        Message getMessage(String message) {
            return new UserMessage(message);
        }
    }, ASSISTANT("assistant") {
        @Override
        Message getMessage(String message) {
            return new AssistantMessage(message);
        }
    }, SYSTEM("system") {
        @Override
        Message getMessage(String message) {
            return new SystemMessage(message);
        }
    };

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public static Role getRole(String value) {
        return Arrays.stream(Role.values())
                .filter(r -> r.getValue().equals(value))
                .findFirst()
                .orElseThrow();
    }

    abstract Message getMessage(String prompt);
}
