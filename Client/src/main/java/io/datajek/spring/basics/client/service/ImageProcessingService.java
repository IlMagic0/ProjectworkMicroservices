package io.datajek.spring.basics.client.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ImageProcessingService {

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
        // Step 1: REMOVE BACKGROUND
        byte[] removedBackground = callService(
                "http://removebackground/image/process",
                file.getBytes(),
                file.getOriginalFilename()
        );

        // Step 2: ADD SHADOW
        byte[] shadowedImage = callService(
                "http://shadow-service/shadow/process",
                removedBackground,
                "shadowed.png"
        );

        // Step 3: EXTRACT ALPHA MASK
        byte[] alphaMaskImage = callService(
                "http://alphamask-service/alphamask/generate",
                shadowedImage,
                "final.png"
        );

        return alphaMaskImage;
    }

    private byte[] callService(String url, byte[] fileBytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", toByteArrayResource(fileBytes, filename));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(url, request, byte[].class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("❌ Failed calling: " + url);
        }

        return response.getBody();
    }
}
