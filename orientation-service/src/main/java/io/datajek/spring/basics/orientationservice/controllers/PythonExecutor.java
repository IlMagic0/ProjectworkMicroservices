package io.datajek.spring.basics.orientationservice.controllers;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Component
public class PythonExecutor {

    public String runOrientationScript(String imagePath) {
        try {
            String scriptPath = Paths.get("scripts", "get_car_orientation.py").toString();
            ProcessBuilder pb = new ProcessBuilder("python", scriptPath, imagePath);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return reader.lines().collect(Collectors.joining());
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "{\"error\": \"Errore nell'esecuzione dello script\"}";
        }
    }
}
