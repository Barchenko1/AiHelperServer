package com.helper.server.service.file;

import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

public interface IFileService {
    void sendFile(Principal principal, MultipartFile file, String prompt);
    void sendFiles(Principal principal, List<MultipartFile> file, String prompt);
}
