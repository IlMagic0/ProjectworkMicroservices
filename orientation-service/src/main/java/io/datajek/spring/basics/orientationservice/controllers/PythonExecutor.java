package io.datajek.spring.basics.orientationservice.controllers;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Paths;

@Component
public class PythonExecutor {

    public String runOrientationScript(String imagePath) throws IOException, InterruptedException {


        String scriptPath = Paths.get("orientation-service", "scripts", "get_car_orientation.py").toString();

        ProcessBuilder pb = new ProcessBuilder("python", scriptPath, imagePath);
        pb.redirectErrorStream(true);
        Process process = pb.start();




        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python script failed with exit code " + exitCode);
        }

        return output.toString(); // JSON string
    }
}