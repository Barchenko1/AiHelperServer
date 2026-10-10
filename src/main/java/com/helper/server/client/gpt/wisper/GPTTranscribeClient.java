package com.helper.server.client.gpt.wisper;

import com.google.gson.JsonParser;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
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
public class GPTTranscribeClient implements ITranscribeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GPTTranscribeClient.class);
    @Value(value = "${GPT_API_KEY}")
    protected String apiKey;
    @Value(value = "${openai.api.url.transcriptions}")
    private String transcriptionApiUrl;

    @Override
    public String transcribeWithGPT(@NonNull MultipartFile file) {
        String boundary = "----OpenAIFormBoundary" + System.currentTimeMillis();
        HttpURLConnection conn = null;

        try {
            URL url = new URL(transcriptionApiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("audio");
            String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");

            try (OutputStream out = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true)) {

                final String CRLF = "\r\n";

                writer.append("--").append(boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                        .append(filename).append("\"").append(CRLF);
                writer.append("Content-Type: ").append(contentType).append(CRLF).append(CRLF).flush();
                try (InputStream in = file.getInputStream()) {
                    in.transferTo(out);
                }
                out.flush();
                writer.append(CRLF).flush();

                writer.append("--").append(boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"model\"").append(CRLF).append(CRLF);
                writer.append("whisper-1").append(CRLF).flush();

                writer.append("--").append(boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"language\"").append(CRLF).append(CRLF);
                writer.append("en").append(CRLF).flush();

                writer.append("--").append(boundary).append("--").append(CRLF).flush();
            }

            int code = conn.getResponseCode();
            InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String body = "";
            if (is != null) try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
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
                LOGGER.error("❌ GPT transcribe failed: {} {}", code, body);
                throw new IOException("GPT transcribe error " + code + ": " + body);
            }

        } catch (IOException e) {
            LOGGER.error("❌ Error calling GPT: {}", e.toString());
            throw new RuntimeException(e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
