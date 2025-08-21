package com.helper.server.openaiclient.wisper;

import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class OpenAITranscribeClient implements IOpenAITranscribeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAITranscribeClient.class);
    @Value(value = "${OPEN_AI_API_KEY}")
    protected String openAiApiKey;
    @Value(value = "${openai.api.url.transcriptions}")
    private String transcriptionApiUrl;

    @Override
    public String transcribeWithOpenAI(MultipartFile file) {
        String boundary = "----OpenAIFormBoundary" + System.currentTimeMillis();
        HttpURLConnection conn = null;

        try {
            URL url = new URL(transcriptionApiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Authorization", "Bearer " + openAiApiKey);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("audio");
            String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");

            try (OutputStream out = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true)) {

                final String CRLF = "\r\n";

                // --- file part ---
                writer.append("--").append(boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                        .append(filename).append("\"").append(CRLF);
                writer.append("Content-Type: ").append(contentType).append(CRLF).append(CRLF).flush();
                try (InputStream in = file.getInputStream()) {
                    in.transferTo(out);
                }
                out.flush();
                writer.append(CRLF).flush();

                // --- model ---
                writer.append("--").append(boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"model\"").append(CRLF).append(CRLF);
                writer.append("whisper-1").append(CRLF).flush();

                // --- language (optional; set as needed) ---
                writer.append("--").append(boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"language\"").append(CRLF).append(CRLF);
                writer.append("en").append(CRLF).flush();

                // --- close ---
                writer.append("--").append(boundary).append("--").append(CRLF).flush();
            }

            int code = conn.getResponseCode();
            InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String body;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line; while ((line = br.readLine()) != null) sb.append(line);
                body = sb.toString();
            }

            if (code == 200) {
                return JsonParser.parseString(body)
                        .getAsJsonObject()
                        .get("text")
                        .getAsString();
            } else {
                LOGGER.error("❌ OpenAI transcribe failed: {} {}", code, body);
                return null;
            }

        } catch (IOException e) {
            LOGGER.error("❌ Error calling OpenAI: {}", e.toString());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    @Override
    public String transcribeWithOpenAI(File file) {
        try {
            String boundary = "----OpenAIFormBoundary" + System.currentTimeMillis();

            URL url = new URL(transcriptionApiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + openAiApiKey);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream out = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true)) {

                // file part
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n");
                writer.append("Content-Type: audio/wav\r\n\r\n").flush();
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    inputStream.transferTo(out);
                }
                out.flush();
                writer.append("\r\n").flush();

                // model param
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
                writer.append("whisper-1").append("\r\n").flush();

                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"language\"\r\n\r\n");
                writer.append("en").append("\r\n").flush();

                // close boundary
                writer.append("--").append(boundary).append("--").append("\r\n").flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    return JsonParser
                            .parseString(response.toString())
                            .getAsJsonObject()
                            .get("text")
                            .getAsString();
                }
            } else {
                LOGGER.error("❌ OpenAI API failed: {}", responseCode);
                return null;
            }

        } catch (IOException e) {
            LOGGER.error("❌ Error sending to OpenAI: {}", e.getMessage());
            return null;
        }
    }
}
