package io.datajek.spring.basics.renderservice.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/render")
public class RenderController {

    private final BlenderExecutor blenderExecutor;

    public RenderController(BlenderExecutor blenderExecutor) {
        this.blenderExecutor = blenderExecutor;
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> renderImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam("mask") MultipartFile mask,
            @RequestParam("windowMask") MultipartFile windowMask) throws IOException {

        // Save temp files
        File colorTmp = File.createTempFile("color-", ".png");
        image.transferTo(colorTmp);

        File maskTmp = File.createTempFile("mask-", ".png");
        mask.transferTo(maskTmp);

        File windowMaskTmp = File.createTempFile("windowMask-", ".png");
        windowMask.transferTo(windowMaskTmp);

        File outputTmp = File.createTempFile("render-", ".png");

        // Call BlenderExecutor with 4 args now
        boolean ok = blenderExecutor.render(
                colorTmp.getAbsolutePath(),
                maskTmp.getAbsolutePath(),
                windowMaskTmp.getAbsolutePath(),
                outputTmp.getAbsolutePath()
        );

        if (!ok || !outputTmp.exists()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        byte[] bytes = Files.readAllBytes(outputTmp.toPath());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes);
    }
}
