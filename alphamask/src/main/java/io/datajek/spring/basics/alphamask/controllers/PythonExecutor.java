package io.datajek.spring.basics.alphamask.controllers;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class PythonExecutor {

    public boolean runScript(String inputPath, String outputPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    "alphamask/scripts/alpha_mask.py",
                    inputPath,
                    outputPath
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(System.out::println);
            }

            return process.waitFor() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
