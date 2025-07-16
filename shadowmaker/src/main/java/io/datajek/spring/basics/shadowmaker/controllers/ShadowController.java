package io.datajek.spring.basics.shadowmaker.controllers;

import io.datajek.spring.basics.shadowmaker.controllers.PythonExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/shadow")
public class ShadowController {

    private final PythonExecutor pythonExecutor;

    public ShadowController(PythonExecutor pythonExecutor) {
        this.pythonExecutor = pythonExecutor;
    }

    @PostMapping("/process")
    public ResponseEntity<byte[]> processImage(@RequestParam("file") MultipartFile file) throws IOException {
        File inputFile = File.createTempFile("input-", ".png");
        file.transferTo(inputFile);

        File outputFile = File.createTempFile("output-", ".png");

        boolean success = pythonExecutor.runScript(inputFile.getAbsolutePath(), outputFile.getAbsolutePath());

        if (!success || !outputFile.exists()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        byte[] imageBytes = Files.readAllBytes(outputFile.toPath());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(imageBytes);
    }
}
