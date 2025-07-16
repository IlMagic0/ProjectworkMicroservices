package io.datajek.spring.basics.orientationservice.controllers;

import io.datajek.spring.basics.orientationservice.controllers.PythonExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/orientation")
public class OrientationController {

    private final PythonExecutor executor;

    public OrientationController(PythonExecutor executor) {
        this.executor = executor;
    }

    @PostMapping
    public ResponseEntity<String> analyzeOrientation(@RequestParam("file") MultipartFile file) {
        try {
            File temp = File.createTempFile("auto", ".png");
            file.transferTo(temp);

            String resultJson = executor.runOrientationScript(temp.getAbsolutePath());

            return ResponseEntity.ok(resultJson);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("{\"error\": \"Errore durante l'analisi\"}");
        }
    }
}
