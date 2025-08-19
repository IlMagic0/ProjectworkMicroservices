package io.datajek.spring.basics.windowservice.controllers;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;

@Component
public class PythonExecutor {

    public boolean runScript(String inputPath, String outputPath) {
        try {
            String script = Paths.get("window-service/scripts", "infer_yolov8.py").toString();
            String pycharmPython = "C:/Users/NOAHDECASTRO/Desktop/image-segmentation-yolov8-main/.venv/Scripts/python.exe";

            // This ^ path here is because my python.exe DOES NOT LIKE THE YOLO STUFF
            // and I have to use the one from PyCharm, which is in a virtual environment, that already works (technically) with
            // the previous original scripts
            // I hope.

            ProcessBuilder pb = new ProcessBuilder(
                    pycharmPython,
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

