package io.datajek.spring.basics.removebackground.controllers;


import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/image")
public class ImageController {

    private final PythonExecutor pythonExecutor;

    public ImageController(PythonExecutor pythonExecutor) {
        this.pythonExecutor = pythonExecutor;
    }

    @PostMapping("/process")
    public ResponseEntity<byte[]> processImage(@RequestParam("file") MultipartFile file) throws IOException {
        // Salva il file ricevuto
        File inputFile = File.createTempFile("input-", ".png");
        file.transferTo(inputFile);

        // Output temporaneo
        File outputFile = File.createTempFile("output-", ".png");

        // Esegue script Python
        boolean success = pythonExecutor.runScript(inputFile.getAbsolutePath(), outputFile.getAbsolutePath());

        if (!success || !outputFile.exists()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Restituisce l'immagine processata
        byte[] imageBytes = Files.readAllBytes(outputFile.toPath());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(imageBytes);
    }
}

