package io.datajek.spring.basics.client.controller;

import io.datajek.spring.basics.client.service.ImageProcessingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ImageController {

    private final ImageProcessingService imageProcessingService;

    public ImageController(ImageProcessingService imageProcessingService) {
        this.imageProcessingService = imageProcessingService;
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
}
