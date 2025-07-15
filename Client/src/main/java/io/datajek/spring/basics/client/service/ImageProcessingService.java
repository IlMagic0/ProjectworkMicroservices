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

    // Helper to convert MultipartFile to ByteArrayResource with filename
    private ByteArrayResource toByteArrayResource(MultipartFile file) throws IOException {
        return new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
    }

    public byte[] processImageThroughPipeline(MultipartFile file) throws IOException {
        // Step 1: Call removebackground service
        MultiValueMap<String, Object> removeBody = new LinkedMultiValueMap<>();
        removeBody.add("file", toByteArrayResource(file));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> removeRequest = new HttpEntity<>(removeBody, headers);

        ResponseEntity<byte[]> removeResponse = restTemplate.postForEntity(
                "http://removebackground/image/process",
                removeRequest,
                byte[].class
        );

        if (!removeResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Remove background failed");
        }

        byte[] removedBackgroundImage = removeResponse.getBody();

        // Step 2: Call shadow service
        ByteArrayResource processedImageResource = new ByteArrayResource(removedBackgroundImage) {
            @Override
            public String getFilename() {
                return "processed.png";
            }
        };

        MultiValueMap<String, Object> shadowBody = new LinkedMultiValueMap<>();
        shadowBody.add("file", processedImageResource);

        HttpEntity<MultiValueMap<String, Object>> shadowRequest = new HttpEntity<>(shadowBody, headers);

        ResponseEntity<byte[]> shadowResponse = restTemplate.postForEntity(
                "http://shadow-service/shadow/process",
                shadowRequest,
                byte[].class
        );

        if (!shadowResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Add shadow failed");
        }

        return shadowResponse.getBody();
    }
}
