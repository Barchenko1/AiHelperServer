package com.helper.server.util;

public final class Constant {
    public static final String LANGUAGE_TEXT = "Write a clear and correct solution in %s. (write it) ";
    /** Language value (e.g. from the extension) that leaves the language sentence out of the prompt. */
    public static final String NO_LANGUAGE = "None";

    private Constant() {}

    /** The prompt plus the "solution in &lt;language&gt;" sentence, unless the language is None/empty. */
    public static String withLanguage(String prompt, String language) {
        String base = prompt == null ? "" : prompt.trim();
        if (language == null || language.isBlank() || NO_LANGUAGE.equalsIgnoreCase(language.trim())) {
            return base;
        }
        return (base + " " + LANGUAGE_TEXT.formatted(language.trim())).trim();
    }
}
