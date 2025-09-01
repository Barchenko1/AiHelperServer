package com.helper.server;

import com.helper.server.repository.ChatRepository;
import com.helper.server.service.advisor.extension.ExpansionQueryAdvisor;
import com.helper.server.service.advisor.rag.RagAdvisor;
import com.helper.server.service.ui.PostgresChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AiHelperServerApplication {

    private static final PromptTemplate MY_PROMPT_TEMPLATE = new PromptTemplate(
            "{query}\n\n" +
                    "Контекст:\n" +
                    "---------------------\n" +
                    "{question_answer_context}\n" +
                    "---------------------\n\n" +
                    "Отвечай только на основе контекста выше. Если информации нет в контексте, сообщи, что не можешь ответить."
    );

    private static final PromptTemplate SYSTEM_PROMPT = new PromptTemplate(
            """
                    Ты — Евгений Борисов, Java-разработчик и эксперт по Spring. Отвечай от первого лица, кратко и по делу.
                    
                    Вопрос может быть о СЛЕДСТВИИ факта из Context.
                    ВСЕГДА связывай: факт Context → вопрос.
                    
                    Нет связи, даже косвенной = "я не говорил об этом в докладах".
                    Есть связь = отвечай.
                    """
    );

    private final ChatRepository chatRepository;
    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    @Autowired
    public AiHelperServerApplication(ChatRepository chatRepository, VectorStore vectorStore, ChatModel chatModel) {
        this.chatRepository = chatRepository;
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultAdvisors(
                ExpansionQueryAdvisor.builder(chatModel).order(0).build(),
                getHistoryAdvisor(1),
                SimpleLoggerAdvisor.builder().order(2).build(),
//                getRagAdviser(3),
                RagAdvisor.builder(vectorStore).order(3).build(),
                SimpleLoggerAdvisor.builder().order(4).build()
                ).defaultOptions(OllamaOptions.builder()
                        .temperature(0.3)
                        .topP(0.7)
                        .topK(20)
                        .repeatPenalty(1.1)
                        .build())
                .defaultSystem(SYSTEM_PROMPT.render())
                .build();
    }

    private Advisor getRagAdviser(int order) {
        return QuestionAnswerAdvisor.builder(vectorStore).promptTemplate(MY_PROMPT_TEMPLATE)
                .searchRequest(SearchRequest.builder().topK(4).similarityThreshold(0.6).build())
                .order(order)
                .build();
    }

    private Advisor getHistoryAdvisor(int order) {
        return MessageChatMemoryAdvisor
                .builder(getChatMemory())
                .order(order)
                .build();
    }

    private ChatMemory getChatMemory() {
        return PostgresChatMemory.builder()
                .maxMessages(8)
                .chatMemoryRepository(chatRepository)
                .build();
    }

	public static void main(String[] args) {
		SpringApplication.run(AiHelperServerApplication.class, args);
	}

}
