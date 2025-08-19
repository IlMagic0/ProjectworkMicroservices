package io.datajek.spring.basics.renderinclined.controllers;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;

@Component
public class BlenderExecutor {
    public boolean render(String colorPath, String maskPath, String windowMaskPath, int rotateFlag, String outputPath) {
        try {
            String blend = Paths.get("render-inclined/blend", "backgroundtest.blend").toString();
            String script = Paths.get("render-inclined/scripts", "render_with_mask.py").toString();

            String blenderPath = "C:\\Program Files\\Blender Foundation\\Blender 4.3\\blender.exe";

            ProcessBuilder pb = new ProcessBuilder(
                    blenderPath,
                    "-b", blend,
                    "-P", script,
                    "--", colorPath, maskPath, windowMaskPath, String.valueOf(rotateFlag), outputPath
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
