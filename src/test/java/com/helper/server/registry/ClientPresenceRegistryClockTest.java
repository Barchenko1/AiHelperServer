package com.helper.server.registry;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ClientPresenceRegistryClockTest {

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void reportsSecondsSinceLastPing() {
        MutableClock clock = new MutableClock();
        ClientPresenceRegistry registry = new ClientPresenceRegistry(clock);
        registry.record("pa", ClientPresenceRegistry.CLI);

        clock.now = clock.now.plus(Duration.ofSeconds(12));

        assertThat(registry.secondsSinceSeen("pa")).containsEntry("cli", 12L);
    }

    @Test
    void forgetForUnknownUserIsANoOp() {
        ClientPresenceRegistry registry = new ClientPresenceRegistry();
        registry.forget("nobody", ClientPresenceRegistry.CLI);
        assertThat(registry.secondsSinceSeen("nobody")).isEmpty();
    }
}
