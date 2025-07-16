package io.datajek.spring.basics.windowservice.controllers;

import io.datajek.spring.basics.windowservice.controllers.PythonExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/yolo")
public class WindowController {

    private final PythonExecutor pythonExecutor;

    public WindowController(PythonExecutor pythonExecutor) {
        this.pythonExecutor = pythonExecutor;
    }

    @PostMapping(value = "/segment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> segment(@RequestParam("file") MultipartFile file) throws IOException {

        File input = File.createTempFile("input-", ".png");
        file.transferTo(input);

        File output = File.createTempFile("mask-", ".png");

        if (!pythonExecutor.runScript(input.getAbsolutePath(), output.getAbsolutePath()) || !output.exists()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(Files.readAllBytes(output.toPath()));
    }
}

