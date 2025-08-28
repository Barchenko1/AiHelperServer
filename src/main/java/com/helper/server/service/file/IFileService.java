package com.helper.server.service.file;

import org.springframework.web.multipart.MultipartFile;

public interface IFileService {
    void sendFile(MultipartFile file, String subPrompt);
}
