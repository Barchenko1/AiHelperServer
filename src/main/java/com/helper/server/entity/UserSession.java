package com.helper.server.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSession {
    public String username;
    public String token;
    public String code;
    public Instant expiresAt;

    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
}
