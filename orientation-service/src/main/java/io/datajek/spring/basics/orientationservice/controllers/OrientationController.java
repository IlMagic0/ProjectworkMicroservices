package io.datajek.spring.basics.orientationservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/orientation")
public class OrientationController {

    private final PythonExecutor executor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrientationController(PythonExecutor executor) {
        this.executor = executor;
    }

    @PostMapping(produces = "application/json")
    public ResponseEntity<?> analyzeOrientation(@RequestParam("file") MultipartFile file) {
        try {
            File temp = File.createTempFile("auto", ".png");
            file.transferTo(temp);

            String resultJson = executor.runOrientationScript(temp.getAbsolutePath());

            // Parse Python JSON into Java Map
            Map<String, Object> result = objectMapper.readValue(resultJson, Map.class);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Errore durante l'analisi"));
        }
    }
}
