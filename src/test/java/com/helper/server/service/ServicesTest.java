package com.helper.server.service;

import com.helper.server.client.IAIClient;
import com.helper.server.entity.ExtensionPayload;
import com.helper.server.service.file.FileService;
import com.helper.server.service.text.TextService;
import com.helper.server.service.transform.FileTransformService;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.websocket.WSHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServicesTest {

    private final Principal pa = () -> "pa";
    private final IAIClient ai = mock(IAIClient.class);
    private final IJsonTemplateService templates = mock(IJsonTemplateService.class);
    private final WSHandler ws = mock(WSHandler.class);

    @Test
    void textIsSanitizedPromptedAndAnswerBroadcast() {
        when(templates.buildJsonTextPayload(anyString(), anyString())).thenReturn("{json}");
        when(ai.sendToAI("{json}")).thenReturn("\"answer\"");
        String longText = "say  \"hi\"\n\tthere " + "x".repeat(60);

        new TextService(ai, templates, ws).sendText(pa, new ExtensionPayload(longText, "Solve", "Java"));

        verify(templates).buildJsonTextPayload(
                "say \\\"hi\\\" there " + "x".repeat(60),
                "Solve Write a clear and correct solution in Java. (write it)");
        verify(ws).broadcastToUser("pa", "\"answer\"");
    }

    @Test
    void languageNoneSendsOnlyThePrompt() {
        when(templates.buildJsonTextPayload(anyString(), anyString())).thenReturn("{json}");

        new TextService(ai, templates, ws).sendText(pa, new ExtensionPayload("code", "Explain this", "None"));

        verify(templates).buildJsonTextPayload("code", "Explain this");
    }

    @Test
    void shortTextIsAlsoSent() {
        when(templates.buildJsonTextPayload(anyString(), anyString())).thenReturn("{json}");
        when(ai.sendToAI("{json}")).thenReturn("\"ok\"");

        new TextService(ai, templates, ws).sendText(pa, new ExtensionPayload("short", "p", "go"));

        verify(ws).broadcastToUser("pa", "\"ok\"");
    }

    @Test
    void singleFileIsSentAsPlainBase64() {
        when(templates.buildJsonFilePayload(anyList(), any())).thenReturn("{json}");
        when(ai.sendToAI("{json}")).thenReturn("\"seen\"");
        byte[] bytes = {1, 2, 3};

        new FileService(ai, templates, ws).sendFile(pa, new MockMultipartFile("file", bytes), "what");

        verify(templates).buildJsonFilePayload(List.of(Base64.getEncoder().encodeToString(bytes)), "what");
        verify(ws).broadcastToUser("pa", "\"seen\"");
    }

    @Test
    void unreadableFileFailsLoudly() throws IOException {
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.getBytes()).thenThrow(new IOException("disk"));

        assertThatThrownBy(() -> new FileService(ai, templates, ws).sendFile(pa, broken, "p"))
                .isInstanceOf(RuntimeException.class).hasRootCauseMessage("disk");
    }

    @Test
    void imagesBecomeDataUrlsAndEmptyFilesAreSkipped() {
        when(templates.buildJsonFilePayload(anyList(), any())).thenReturn("{json}");
        when(ai.sendToAI("{json}")).thenReturn("\"seen\"");
        byte[] bytes = {9};
        String b64 = Base64.getEncoder().encodeToString(bytes);
        List<MultipartFile> files = new ArrayList<>();
        files.add(new MockMultipartFile("files", "a.png", "image/png", bytes));
        files.add(new MockMultipartFile("files", "b.bin", null, bytes));
        files.add(new MockMultipartFile("files", "c.png", "image/png", new byte[0]));
        files.add(null);

        new FileService(ai, templates, ws).sendFiles(pa, files, "p");

        verify(templates).buildJsonFilePayload(List.of("data:image/png;base64," + b64, b64), "p");
        verify(ws).broadcastToUser("pa", "\"seen\"");
    }

    @Test
    void onlyEmptyFilesSendNothing() {
        new FileService(ai, templates, ws).sendFiles(pa, List.of(new MockMultipartFile("files", new byte[0])), "p");

        verifyNoInteractions(ai, ws);
    }

    @Test
    void unreadableFileInListFailsLoudly() throws IOException {
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.isEmpty()).thenReturn(false);
        when(broken.getContentType()).thenReturn("image/png");
        when(broken.getBytes()).thenThrow(new IOException("disk"));

        assertThatThrownBy(() -> new FileService(ai, templates, ws).sendFiles(pa, List.of(broken), "p"))
                .isInstanceOf(RuntimeException.class).hasRootCauseMessage("disk");
    }

    private final FileTransformService transform = new FileTransformService();

    @Test
    void fileTransformReadsUtf8() {
        assertThat(transform.getFilePayload(new MockMultipartFile("f", "héllo".getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .isEqualTo("héllo");
    }

    @Test
    void fileTransformRejectsMissingOrEmptyFile() {
        assertThatThrownBy(() -> transform.getFilePayload(null)).hasMessage("No file or file empty");
        assertThatThrownBy(() -> transform.getFilePayload(new MockMultipartFile("f", new byte[0]))).hasMessage("No file or file empty");
    }

    @Test
    void fileTransformWrapsReadErrors() throws IOException {
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.getBytes()).thenThrow(new IOException("disk"));

        assertThatThrownBy(() -> transform.getFilePayload(broken)).hasRootCauseMessage("disk");
    }
}
