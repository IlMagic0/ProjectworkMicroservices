package io.datajek.spring.basics.client.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class BucketService {

    private final RestTemplate restTemplate;

    public BucketService(RestTemplate restTemplate) {
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

    /**
     * Calls bucket-service /upload-api with the given file.
     */
    public Map<String, Object> checkAndUploadToBucket(MultipartFile file, String filename) throws IOException {
        String url = "http://bucket-service/upload-api";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(
                MediaType.parseMediaType(file.getContentType() != null ? file.getContentType() : "image/png")
        );

        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(
                toByteArrayResource(file.getBytes(), filename),
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
}
