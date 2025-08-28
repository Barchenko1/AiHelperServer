package com.helper.server.service.transform;

import org.springframework.web.multipart.MultipartFile;

public interface IFileTransformService {
    String getFilePayload(MultipartFile file);
}
