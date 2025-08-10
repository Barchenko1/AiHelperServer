package com.helper.server.controller;

import com.helper.server.process.voice.IVoiceCutterProcess;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class VoiceController {

    private final IVoiceCutterProcess voiceCutterProcess;

    public VoiceController(IVoiceCutterProcess voiceCutterProcess) {
        this.voiceCutterProcess = voiceCutterProcess;
    }


    @PostMapping("/voice")
    public ResponseEntity<Void> handle(@RequestBody String body,
                                       @RequestPart(value = "subPrompt", required = false) String subPrompt) {
        voiceCutterProcess.execute(body, subPrompt);
        return ResponseEntity.ok().build();
    }
}
