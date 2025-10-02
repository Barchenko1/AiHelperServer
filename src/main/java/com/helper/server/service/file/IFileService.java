package com.helper.server.service.file;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IFileService {
    void sendFile(MultipartFile file, String prompt);
    void sendFiles(List<MultipartFile> file, String prompt);
}
