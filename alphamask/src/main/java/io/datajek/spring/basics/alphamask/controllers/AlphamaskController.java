package io.datajek.spring.basics.alphamask.controllers;


import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/alphamask")
public class AlphamaskController {

    private final PythonExecutor pythonExecutor;

    public AlphamaskController(PythonExecutor pythonExecutor) {
        this.pythonExecutor = pythonExecutor;
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateAlphaMask(@RequestParam("file") MultipartFile file) throws IOException {
        File inputFile = File.createTempFile("input-", ".png");
        file.transferTo(inputFile);

        File outputFile = File.createTempFile("output-mask-", ".png");

        boolean success = pythonExecutor.runScript(inputFile.getAbsolutePath(), outputFile.getAbsolutePath());

        if (!success || !outputFile.exists()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        byte[] maskBytes = Files.readAllBytes(outputFile.toPath());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(maskBytes);
    }
}
