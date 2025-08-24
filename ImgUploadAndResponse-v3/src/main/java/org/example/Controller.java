package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

@org.springframework.stereotype.Controller
public class Controller {

    @Autowired
    private ImageService imageService;

    @Autowired
    private Evaluer evaluer;

    @Autowired
    private ResponseLogic responseLogic;

    @Autowired
    private AmazonS3Service amazonS3Service;

    @GetMapping("/")
    public String index() {
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("error", "ERROR: No file selected");
            return "upload";
        }

        try {
            ImageDTO imageDTO = imageService.processImage(file);
            boolean evaluationResult = evaluer.evaluate(imageDTO);
            responseLogic.setMeetsCriteria(evaluationResult);

            if (!evaluationResult) {
                model.addAttribute("error", "ERROR: Image too big or wrong file type");
                return "upload";
            }

            boolean uploadSuccess = amazonS3Service.uploadFile(imageDTO.getFileName(), file.getBytes());
            if (!uploadSuccess) {
                model.addAttribute("error", "ERROR: Failed to upload the file");
                return "upload";
            }

            model.addAttribute("imageDTO", imageDTO);
            model.addAttribute("meetsCriteria", responseLogic.isMeetsCriteria());
            model.addAttribute("uploadSuccess", uploadSuccess);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "ERROR: An unexpected error occurred");
            return "upload";
        }

        return "result";
    }

    @GetMapping("/download/{fileName:.+}")
    @ResponseBody
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileName) {
        byte[] fileContent = amazonS3Service.downloadFile(fileName);

        if (fileContent == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData(fileName, fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);
    }

    @PostMapping("/upload-api")
    @ResponseBody
    public Map<String, Object> checkAndUpload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("No file selected");
        }

        ImageDTO imageDTO = imageService.processImage(file);
        boolean evaluationResult = evaluer.evaluate(imageDTO);
        responseLogic.setMeetsCriteria(evaluationResult);

        if (!evaluationResult) {
            throw new RuntimeException("Image does not meet criteria");
        }

        boolean uploadSuccess = amazonS3Service.uploadFile(imageDTO.getFileName(), file.getBytes());
        if (!uploadSuccess) {
            throw new RuntimeException("Failed to upload the file");
        }

        return Map.of(
                "fileName", imageDTO.getFileName(),
                "uploadSuccess", uploadSuccess,
                "meetsCriteria", responseLogic.isMeetsCriteria()
        );
    }

    /*
    @PostMapping("/upload-rendered")
    @ResponseBody
    public Map<String, Object> uploadRendered(@RequestParam("file") MultipartFile file,
                                              @RequestParam("renderedName") String renderedName) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("No file selected");
        }

        // Read image
        BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
        if (bufferedImage == null) {
            throw new RuntimeException("Invalid image file");
        }

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int[] pixelArray = bufferedImage.getRGB(0, 0, width, height, null, 0, width);

        // Use the uploaded bytes directly for S3
        byte[] fileBytes = file.getBytes();

        // Create ImageDTO for tracking (optional)
        ImageDTO imageDTO = new ImageDTO(fileBytes, renderedName, file.getContentType(), file.getSize(), height, width, pixelArray);

        // Evaluate image if needed
        boolean evaluationResult = evaluer.evaluate(imageDTO);
        responseLogic.setMeetsCriteria(evaluationResult);
        if (!evaluationResult) {
            throw new RuntimeException("Rendered image does not meet criteria");
        }

        // Upload to S3 using the provided rendered name
        boolean uploadSuccess = amazonS3Service.uploadFile(renderedName, fileBytes);
        if (!uploadSuccess) {
            throw new RuntimeException("Failed to upload the rendered file");
        }

        return Map.of(
                "fileName", renderedName,
                "uploadSuccess", uploadSuccess,
                "meetsCriteria", responseLogic.isMeetsCriteria()
        );
    }

     */

}