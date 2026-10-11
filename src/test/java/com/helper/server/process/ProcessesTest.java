package com.helper.server.process;

import com.google.gson.JsonParser;
import com.helper.server.client.IAIClient;
import com.helper.server.client.gpt.wisper.ITranscribeClient;
import com.helper.server.entity.ExtensionPayload;
import com.helper.server.process.extension.ExtensionProcess;
import com.helper.server.process.screenshot.ScreenshotProcess;
import com.helper.server.process.voice.VoiceCutterProcess;
import com.helper.server.service.file.IFileService;
import com.helper.server.service.text.ITextService;
import com.helper.server.service.transform.IFileTransformService;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.websocket.WSHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessesTest {

    private final Principal pa = () -> "pa";
    private final WSHandler ws = mock(WSHandler.class);

    @Test
    void extensionTextShowsProcessingThenSendsText() {
        ITextService text = mock(ITextService.class);
        ExtensionProcess process = new ExtensionProcess(ws, text, mock(IFileTransformService.class));
        ExtensionPayload payload = new ExtensionPayload("t", "p", "java");

        process.executeText(pa, payload);

        verify(ws).broadcastToUser("pa", "Processing...");
        verify(text).sendText(pa, payload);
    }

    @Test
    void extensionCanvasUsesFileContentAsText() {
        ITextService text = mock(ITextService.class);
        IFileTransformService transform = mock(IFileTransformService.class);
        MultipartFile file = new MockMultipartFile("file", "x".getBytes());
        when(transform.getFilePayload(file)).thenReturn("file body");
        ExtensionPayload payload = new ExtensionPayload(null, "p", "java");

        new ExtensionProcess(ws, text, transform).executeCanvasTag(pa, payload, file);

        assertThat(payload.getText()).isEqualTo("file body");
        verify(ws).broadcastToUser("pa", "Processing...");
        verify(text).sendText(pa, payload);
    }

    @Test
    void screenshotWithoutFileTellsUserFolderIsEmpty() {
        IFileService files = mock(IFileService.class);
        ScreenshotProcess process = new ScreenshotProcess(ws, files);

        process.execute((MultipartFile) null, "p", pa);
        process.execute((List<MultipartFile>) null, "p", pa);
        process.execute(List.of(), "p", pa);

        verify(ws, org.mockito.Mockito.times(3)).broadcastToUser("pa", "Commit folder is empty");
        verifyNoInteractions(files);
    }

    @Test
    void screenshotsAreSentToFileService() {
        IFileService files = mock(IFileService.class);
        ScreenshotProcess process = new ScreenshotProcess(ws, files);
        MultipartFile file = new MockMultipartFile("file", new byte[]{1});

        process.execute(file, "p", pa);
        process.execute(List.of(file), "p", pa);

        verify(files).sendFile(pa, file, "p");
        verify(files).sendFiles(pa, List.of(file), "p");
    }

    private final ITranscribeClient transcriber = mock(ITranscribeClient.class);
    private final IAIClient ai = mock(IAIClient.class);
    private final IJsonTemplateService templates = mock(IJsonTemplateService.class);
    private final VoiceCutterProcess voice = new VoiceCutterProcess(transcriber, ai, templates, ws);

    @Test
    void voiceWithoutFileIsReported() {
        voice.execute(pa, null, "p");

        verify(ws).broadcastToUser("pa", "No voice file provided");
        verifyNoInteractions(transcriber);
    }

    @Test
    void voiceIsTranscribedAndAnswered() {
        MultipartFile file = new MockMultipartFile("file", new byte[]{1});
        when(transcriber.transcribeWithGPT(file)).thenReturn("what is 2+2");
        when(templates.buildJsonTextPayload("what is 2+2", "p")).thenReturn("{json}");
        when(ai.sendToAI("{json}")).thenReturn("\"4\"");

        voice.execute(pa, file, "p");

        verify(ws).broadcastToUser("pa", "Processing...");
        verify(ws).broadcastToUser("pa", "\"4\"");
    }

    @Test
    void silentRecordingTellsTheUser() {
        MultipartFile file = new MockMultipartFile("file", new byte[]{1});
        when(transcriber.transcribeWithGPT(file)).thenReturn("");

        voice.execute(pa, file, "p");
        when(transcriber.transcribeWithGPT(file)).thenReturn(null);
        voice.execute(pa, file, "p");

        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(ws, org.mockito.Mockito.times(4)).broadcastToUser(eq("pa"), msg.capture());
        assertThat(JsonParser.parseString(msg.getAllValues().get(1)).getAsString()).contains("No speech detected");
        verify(ai, never()).sendToAI(anyString());
    }
}
