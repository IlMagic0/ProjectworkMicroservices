package io.datajek.spring.basics.client.controller;

import io.datajek.spring.basics.client.service.ImageProcessingService;
import io.datajek.spring.basics.client.service.BucketService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ImageController {

    private final ImageProcessingService imageProcessingService;
    private final BucketService bucketService;

    public ImageController(ImageProcessingService imageProcessingService, BucketService bucketService) {
        this.imageProcessingService = imageProcessingService;
        this.bucketService = bucketService;
    }

    @PostMapping(value = "/process-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> processImage(@RequestParam("file") MultipartFile file) {
        try {
            byte[] processedImage = imageProcessingService.processImageThroughPipeline(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(processedImage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping(value = "/upload-to-bucket", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadToBucket(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> response = bucketService.checkAndUploadToBucket(file, file.getOriginalFilename());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
