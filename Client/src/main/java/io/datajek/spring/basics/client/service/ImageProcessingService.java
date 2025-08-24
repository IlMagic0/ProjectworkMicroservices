package io.datajek.spring.basics.client.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@Service
public class ImageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessingService.class);

    private final RestTemplate restTemplate;

    public ImageProcessingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private ByteArrayResource toByteArrayResource(byte[] bytes, String filename) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    public byte[] processImageThroughPipeline(MultipartFile file) throws IOException {
        log.debug("Starting image processing pipeline for file: {}", file.getOriginalFilename());

        // Step 0

        checkAndUploadToBucket(file, file.getOriginalFilename()); // upload original file to bucket

        // Step 1: REMOVE BACKGROUND
        log.debug("Calling remove background service for file: {}", file.getOriginalFilename());
        byte[] removedBackground = callService(
                "http://removebackground/image/process",
                file.getBytes(),
                file.getOriginalFilename()
        );
        log.debug("Background removed for file: {}", file.getOriginalFilename());

        // Step 2

        // WINDOW MASKS DETECTION (e.g., some analysis or processing)
        log.debug("Calling window service for file: {}", file.getOriginalFilename());
        byte[] windowService = callService(
                "http://window-service/yolo/segment",
                removedBackground,
                "removed.png"                        // can reuse this filename
        );
        log.debug("Window service completed for file: {}", file.getOriginalFilename());

        // ORIENTATION ANALYSIS
        log.debug("Calling orientation service for file: {}", file.getOriginalFilename());
        Map<String, Object> orientationResult = callOrientationService(
                "http://orientation-service/orientation",
                removedBackground,
                "processed.png"
        );
        System.out.println("Orientation result: " + orientationResult);
        log.debug("Orientation analysis completed for file: {}", file.getOriginalFilename());

        // ADD SHADOW
        log.debug("Calling background service for file: {}", file.getOriginalFilename());
        byte[] shadowImage = callService(
                "http://shadow-service/shadow/process",
                removedBackground,                   // also use the new service output
                "shadowed.png"
        );
        log.debug("Shadow service completed for file: {}", file.getOriginalFilename());

        // Step 3: EXTRACT ALPHA MASK
        log.debug("Calling alpha mask service for file: {}", file.getOriginalFilename());
        byte[] alphaMaskImage = callService(
                "http://alphamask-service/alphamask/generate",
                shadowImage,
                "alphamask.png"
        );
        log.debug("Alpha mask generated for file: {}", file.getOriginalFilename());

        // CHOOSE THE RENDER SERVICE DEPENDING ON THE JSON RESPONSE
        // "status":"inclined" = use inclined render service
        // "status":"flat" = use normal render service

        // STEP 4: DECIDE WHICH RENDER SERVICE TO USE AND RENDER

        String status = (String) orientationResult.get("status");
        log.debug("Status detected: " + status);
        Object dirObj = orientationResult.get("direction");
        int direction = (dirObj instanceof Number) ? ((Number) dirObj).intValue() : 0;

        byte[] finalRender;

        if (status.equalsIgnoreCase("inclined")) {
            log.debug("Calling inclined render service for file: {}", file.getOriginalFilename());
            finalRender = callRenderService(
                    "http://inclined-render-service/render/image",
                    removedBackground,
                    alphaMaskImage,
                    windowService,
                    "final_render.png",
                    direction
            );
            log.debug("Inclined render service completed for file: {}", file.getOriginalFilename());
        } else if (status.equalsIgnoreCase("flat")) {
            log.debug("Calling flat render service for file: {}", file.getOriginalFilename());
            finalRender = callRenderService(
                    "http://render-service/render/image",
                    removedBackground,
                    alphaMaskImage,
                    windowService,
                    "final_render.png",
                    direction
            );
            log.debug("Flat render service completed for file: {}", file.getOriginalFilename());
        } else {
            throw new RuntimeException("Unknown orientation status: " + status);
        }

        log.debug("Final render completed for file: {}", file.getOriginalFilename());


        // Upload to bucket the render
        /*

        String originalName = file.getOriginalFilename(); // e.g., car.png
        String renderName;

        if (originalName != null && originalName.contains(".")) {
            int dotIndex = originalName.lastIndexOf(".");
            renderName = originalName.substring(0, dotIndex) + "_rendered" + originalName.substring(dotIndex);
        } else {
            renderName = "rendered_file.png";
        }

        // Upload the rendered file
        ByteArrayMultipartFile renderedFile = new ByteArrayMultipartFile(finalRender, renderName, "image/png");
        Map<String, Object> uploadResponse = checkAndUploadToBucket(renderedFile, renderName);
        System.out.println("Upload response: " + uploadResponse);

        */




        // Check if upload was successful
        /*
        boolean uploadSuccess = (boolean) uploadResponse.get("uploadSuccess");

        if (uploadSuccess) {
            String uploadedFileName = (String) uploadResponse.get("fileName");

            // 4️⃣ Download the same file immediately
            byte[] downloadedFile = downloadFromBucket(uploadedFileName);
            System.out.println("Downloaded file size: " + downloadedFile.length);

            // Now you can save it locally or send it elsewhere
        } else {
            System.err.println("Upload failed, cannot download");
        }
        */

        return finalRender;

    }


    private byte[] callService(String url, byte[] fileBytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", toByteArrayResource(fileBytes, filename));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(url, request, byte[].class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed calling: " + url);
        }

        return response.getBody();
    }

    // New method for orientation service (returns JSON as Map)
    private Map<String, Object> callOrientationService(String url, byte[] fileBytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", toByteArrayResource(fileBytes, filename));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed calling: " + url);
        }

        return response.getBody();
    }

    // Inclined version with direction
    private byte[] callRenderService(
            String url, byte[] colorBytes, byte[] alphaBytes,
            byte[] windowBytes, String filename, int direction) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", toByteArrayResource(colorBytes, "color.png"));
        body.add("mask", toByteArrayResource(alphaBytes, "alpha.png"));
        body.add("windowMask", toByteArrayResource(windowBytes, "window.png"));
        int rotateFlag = (direction >= 0) ? 0 : 1;  // or any rule that fits your orientation logic
        body.add("rotateFlag", String.valueOf(rotateFlag));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<byte[]> response = restTemplate.postForEntity(url, request, byte[].class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed calling: " + url);
        }

        return response.getBody();
    }

    // Call the check and upload service

    public Map<String, Object> checkAndUploadToBucket(MultipartFile file, String filename) throws IOException {
        String url = "http://bucket-service/upload-api";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(file.getContentType() != null ? file.getContentType() : "image/png"));

        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(
                toByteArrayResource(file.getBytes(), filename),  // use custom filename
                fileHeaders
        );

        body.add("file", filePart);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed calling bucket-service /upload-api");
        }

        return response.getBody();
    }



    /**
     * Call the bucket-service /download/{fileName} endpoint to download a file as bytes.
     */
    public byte[] downloadFromBucket(String fileName) {
        String url = "http://bucket-service/download/" + fileName; // replace with actual URL

        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed calling bucket-service /download for file: " + fileName);
        }

        return response.getBody();
    }


}
