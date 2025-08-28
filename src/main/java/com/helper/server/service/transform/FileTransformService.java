package com.helper.server.service.transform;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class FileTransformService implements IFileTransformService {

    @Override
    public String getFilePayload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("No file or file empty");
        }
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
