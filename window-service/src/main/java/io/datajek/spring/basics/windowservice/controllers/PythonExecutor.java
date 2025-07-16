package io.datajek.spring.basics.windowservice.controllers;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;

@Component
public class PythonExecutor {

    public boolean runScript(String inputPath, String outputPath) {
        try {
            String script = Paths.get("scripts", "infer_yolov8.py").toString();
            ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    script,
                    inputPath,
                    outputPath
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                br.lines().forEach(System.out::println);
            }
            return process.waitFor() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

