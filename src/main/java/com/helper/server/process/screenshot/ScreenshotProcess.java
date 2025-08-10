package com.helper.server.process.screenshot;

import com.helper.server.openaiclient.IOpenAIClient;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
public class ScreenshotProcess implements IScreenshotProcess {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenshotProcess.class);

    private static final String PICTURE_REQ = "/templates/pictureRequest.json";

    private final IOpenAIClient openAIClient;
    private final IJsonTemplateService jsonTemplateService;
    private final WSHandler wsHandler;

    @Autowired
    public ScreenshotProcess(@Qualifier("openAIScreenClient") IOpenAIClient openAIClient,
                             IJsonTemplateService jsonTemplateService, WSHandler wsHandler) {
        this.openAIClient = openAIClient;
        this.jsonTemplateService = jsonTemplateService;
        this.wsHandler = wsHandler;
    }

    @Override
    public void execute(MultipartFile file, String subPrompt) {
        callScreenHelper(file, subPrompt);
    }

    private void callScreenHelper(MultipartFile file, String subPrompt) {
        try {
            long handle1 = System.currentTimeMillis();
//            String base64 = encodeImageToBase64Jpeg(resized, 0.5f);
            byte[] fileBytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(fileBytes);
            String jsonPayload = jsonTemplateService.buildJsonPayload(PICTURE_REQ, base64, subPrompt);
            long timeDiff2 = System.currentTimeMillis() - handle1;
            LOGGER.info("time gets {}", timeDiff2);

            long handle3 = System.currentTimeMillis();
            String response = openAIClient.sendToOpenAI(jsonPayload);
            long timeDiff3 = System.currentTimeMillis() - handle3;
            LOGGER.info("time gets {}", timeDiff3);
            long handle4 = System.currentTimeMillis();
            wsHandler.broadcast(response);
            long timeDiff4 = System.currentTimeMillis() - handle4;
            LOGGER.info("time gets {}", timeDiff4);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String encodeImageToBase64Jpeg(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality); // e.g. 0.5 for good balance

        writer.write(null, new IIOImage(image, null, null), param);
        ios.close();
        writer.dispose();

        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private static File getLatestPng(File folder) {
        File[] pngFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (pngFiles == null || pngFiles.length == 0) return null;

        return Stream.of(pngFiles)
                .filter(File::isFile)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);
    }

}
