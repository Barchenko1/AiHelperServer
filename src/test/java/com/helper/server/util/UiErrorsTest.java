package com.helper.server.util;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class UiErrorsTest {

    @Test
    void replacesProviderJsonBodyWithItsMessage() {
        var e = new RuntimeException(new IOException(
                "GPT transcribe error 429: {\"error\":{\"message\":\"You have no credits remaining.\",\"code\":\"credit_balance_exhausted\"}}"));

        assertThat(UiErrors.describe(e)).isEqualTo("GPT transcribe error 429: You have no credits remaining.");
    }

    @Test
    void keepsPlainMessagesAsIs() {
        assertThat(UiErrors.describe(new RuntimeException("boom"))).isEqualTo("boom");
    }

    @Test
    void keepsTextWhenBraceIsNotAProviderError() {
        assertThat(UiErrors.describe(new RuntimeException("bad {json"))).isEqualTo("bad {json");
        assertThat(UiErrors.describe(new RuntimeException("x {\"a\":1}"))).isEqualTo("x {\"a\":1}");
    }

    @Test
    void fallsBackToClassNameWhenThereIsNoMessage() {
        assertThat(UiErrors.describe(new NullPointerException())).isEqualTo("NullPointerException");
    }

    @Test
    void uiMessageIsJsonEncodedWithWarningPrefix() {
        String wire = UiErrors.toUiMessage(new RuntimeException("line1\n\"q\""));

        assertThat(JsonParser.parseString(wire).getAsString()).isEqualTo("⚠️ line1\n\"q\"");
    }
}
