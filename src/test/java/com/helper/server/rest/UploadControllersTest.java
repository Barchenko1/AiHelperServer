package com.helper.server.rest;

import com.helper.server.entity.ExtensionPayload;
import com.helper.server.process.extension.IExtensionProcess;
import com.helper.server.process.screenshot.IScreenshotProcess;
import com.helper.server.process.voice.IVoiceCutterProcess;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UploadControllersTest {

    private final Principal pa = () -> "pa";

    @Test
    void textIsHandedToExtensionProcess() {
        IExtensionProcess process = mock(IExtensionProcess.class);
        ExtensionPayload payload = new ExtensionPayload("code", "solve", "java");

        assertThat(new ExtensionController(process).handleText(payload, pa).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(process).executeText(pa, payload);
    }

    @Test
    void canvasIsHandedToExtensionProcess() {
        IExtensionProcess process = mock(IExtensionProcess.class);
        ExtensionPayload payload = new ExtensionPayload(null, "solve", "java");
        MultipartFile file = new MockMultipartFile("file", "x".getBytes());

        assertThat(new ExtensionController(process).handleCanvasTag(file, payload, pa).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(process).executeCanvasTag(pa, payload, file);
    }

    @Test
    void singleScreenIsHandedToScreenshotProcess() {
        IScreenshotProcess process = mock(IScreenshotProcess.class);
        MultipartFile file = new MockMultipartFile("file", new byte[]{1});

        assertThat(new ScreenController(process).screen(file, "what", pa).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(process).execute(file, "what", pa);
    }

    @Test
    void multipleScreensAreHandedToScreenshotProcess() {
        IScreenshotProcess process = mock(IScreenshotProcess.class);
        List<MultipartFile> files = List.of(new MockMultipartFile("files", new byte[]{1}));

        assertThat(new ScreenController(process).screens(files, "what", pa).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(process).execute(files, "what", pa);
    }

    @Test
    void wavVoiceIsAccepted() {
        IVoiceCutterProcess process = mock(IVoiceCutterProcess.class);
        VoiceController controller = new VoiceController(process);
        MultipartFile wav = new MockMultipartFile("file", "a.wav", "audio/wav", new byte[]{1});
        MultipartFile xwav = new MockMultipartFile("file", "a.wav", "AUDIO/X-WAV", new byte[]{1});

        assertThat(controller.handleVoice(wav, "p", pa).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.handleVoice(xwav, null, pa).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(process).execute(pa, wav, "p");
        verify(process).execute(pa, xwav, null);
    }

    @Test
    void nonWavVoiceIsRejected() {
        IVoiceCutterProcess process = mock(IVoiceCutterProcess.class);
        MultipartFile mp3 = new MockMultipartFile("file", "a.mp3", "audio/mpeg", new byte[]{1});

        assertThat(new VoiceController(process).handleVoice(mp3, "p", pa).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(process);
    }
}
