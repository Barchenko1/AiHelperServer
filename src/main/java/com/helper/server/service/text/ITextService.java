package com.helper.server.service.text;

import com.helper.server.entity.ExtensionPayload;

public interface ITextService {
    void sendText(ExtensionPayload payload);
}
