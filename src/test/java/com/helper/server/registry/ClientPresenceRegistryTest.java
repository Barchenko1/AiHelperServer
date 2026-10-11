package com.helper.server.registry;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ClientPresenceRegistryTest {

    /** Mutable clock so the tests can move time forward. */
    private static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-10-10T12:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void reportsSecondsSinceLastCheckIn() {
        TestClock clock = new TestClock();
        ClientPresenceRegistry registry = new ClientPresenceRegistry(clock);

        registry.record("pbrchnk", ClientPresenceRegistry.CLI);
        clock.now = clock.now.plusSeconds(42);

        assertThat(registry.secondsSinceSeen("pbrchnk")).containsEntry("cli", 42L);
    }

    @Test
    void neverSeenClientIsAbsent() {
        ClientPresenceRegistry registry = new ClientPresenceRegistry(new TestClock());
        registry.record("pbrchnk", ClientPresenceRegistry.CLI);

        assertThat(registry.secondsSinceSeen("pbrchnk")).doesNotContainKey("extension");
    }

    @Test
    void usersDoNotSeeEachOthersClients() {
        ClientPresenceRegistry registry = new ClientPresenceRegistry(new TestClock());
        registry.record("pa", ClientPresenceRegistry.EXTENSION);

        assertThat(registry.secondsSinceSeen("pbrchnk")).isEmpty();
    }

    @Test
    void newerCheckInReplacesTheOlderOne() {
        TestClock clock = new TestClock();
        ClientPresenceRegistry registry = new ClientPresenceRegistry(clock);
        registry.record("pbrchnk", ClientPresenceRegistry.CLI);
        clock.now = clock.now.plusSeconds(100);
        registry.record("pbrchnk", ClientPresenceRegistry.CLI);

        assertThat(registry.secondsSinceSeen("pbrchnk")).containsEntry("cli", 0L);
    }

    @Test
    void onlyCliAndExtensionAreKnownClients() {
        assertThat(ClientPresenceRegistry.isKnownClient("cli")).isTrue();
        assertThat(ClientPresenceRegistry.isKnownClient("extension")).isTrue();
        assertThat(ClientPresenceRegistry.isKnownClient("browser")).isFalse();
        assertThat(ClientPresenceRegistry.isKnownClient("")).isFalse();
    }

    @Test
    void forgetTurnsOnlyThatClientOffline() {
        ClientPresenceRegistry registry = new ClientPresenceRegistry(new TestClock());
        registry.record("pbrchnk", "cli");
        registry.record("pbrchnk", "extension");

        registry.forget("pbrchnk", "extension");

        assertThat(registry.secondsSinceSeen("pbrchnk")).containsKey("cli").doesNotContainKey("extension");
    }

    @Test
    void forgettingAnUnknownUserIsHarmless() {
        new ClientPresenceRegistry(new TestClock()).forget("nobody", "cli");
    }
}
