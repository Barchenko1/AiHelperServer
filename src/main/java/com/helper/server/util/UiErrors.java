package com.helper.server.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Turns failures into short, readable messages for the UI (sent over the websocket like normal answers). */
public final class UiErrors {

    private static final Gson GSON = new Gson();

    private UiErrors() {}

    /** JSON-encoded "⚠️ ..." string, same wire format as a normal answer. */
    public static String toUiMessage(Throwable error) {
        return GSON.toJson("⚠️ " + describe(error));
    }

    public static String toUiMessage(String text) {
        return GSON.toJson("⚠️ " + text);
    }

    /** Root-cause message; a trailing provider JSON error body is replaced by its "error.message". */
    public static String describe(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String message = root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();

        int brace = message.indexOf('{');
        if (brace >= 0) {
            String friendly = providerMessage(message.substring(brace));
            if (friendly != null) {
                return message.substring(0, brace) + friendly;
            }
        }
        return message;
    }

    private static String providerMessage(String json) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) return null;
            JsonElement error = parsed.getAsJsonObject().get("error");
            if (error != null && error.isJsonObject()) {
                JsonObject e = error.getAsJsonObject();
                if (e.has("message")) return e.get("message").getAsString();
            }
        } catch (RuntimeException ignored) {
            // not JSON - keep the original text
        }
        return null;
    }
}
