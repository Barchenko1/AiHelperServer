package com.helper.server.service.text;

import com.helper.server.entity.ExtensionPayload;

import java.security.Principal;

public interface ITextService {
    void sendText(Principal principal, ExtensionPayload payload);
}
