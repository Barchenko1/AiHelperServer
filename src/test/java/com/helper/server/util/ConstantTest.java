package com.helper.server.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantTest {

    @Test
    void languageSentenceIsAppended() {
        assertThat(Constant.withLanguage("Solve it.", "Java"))
                .isEqualTo("Solve it. Write a clear and correct solution in Java. (write it)");
    }

    @Test
    void noneOrMissingLanguageLeavesItOut() {
        assertThat(Constant.withLanguage(" Solve it. ", "None")).isEqualTo("Solve it.");
        assertThat(Constant.withLanguage("Solve it.", " none ")).isEqualTo("Solve it.");
        assertThat(Constant.withLanguage("Solve it.", null)).isEqualTo("Solve it.");
        assertThat(Constant.withLanguage("Solve it.", "")).isEqualTo("Solve it.");
    }

    @Test
    void missingPromptIsTreatedAsEmpty() {
        assertThat(Constant.withLanguage(null, "Go")).isEqualTo("Write a clear and correct solution in Go. (write it)");
        assertThat(Constant.withLanguage(null, "None")).isEmpty();
    }
}
