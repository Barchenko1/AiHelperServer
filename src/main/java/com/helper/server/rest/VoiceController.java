package com.helper.server.rest;

import com.helper.server.process.voice.IVoiceCutterProcess;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1")
public class VoiceController {

    private final IVoiceCutterProcess voiceCutterProcess;

    public VoiceController(IVoiceCutterProcess voiceCutterProcess) {
        this.voiceCutterProcess = voiceCutterProcess;
    }

    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> handleVoice(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "prompt", required = false) String prompt,
            Principal principal) {
        if (!"audio/wav".equalsIgnoreCase(file.getContentType())
                && !"audio/x-wav".equalsIgnoreCase(file.getContentType())) {
            return ResponseEntity.badRequest().build();
        }

        voiceCutterProcess.execute(principal, file, prompt);
        return ResponseEntity.ok().build();
    }
}
