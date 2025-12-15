package com.auction.auction.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 파일이 비어있는지 확인
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("파일이 비어있습니다.");
            }

            // 파일 확장자 검증
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            if (!isImageFile(extension)) {
                return ResponseEntity.badRequest().body("이미지 파일만 업로드 가능합니다. (jpg, jpeg, png, gif)");
            }

            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 고유한 파일명 생성 (UUID + 원본 확장자)
            String filename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(filename);

            // 파일 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 저장된 파일의 URL 반환 (웹에서 접근 가능한 경로)
            String fileUrl = "/auction/uploads/images/" + filename;

            return ResponseEntity.ok().body(new FileUploadResponse(fileUrl, filename));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("파일 업로드 실패: " + e.getMessage());
        }
    }

    // 이미지 파일 확장자 검증
    private boolean isImageFile(String extension) {
        extension = extension.toLowerCase();
        return extension.equals(".jpg") ||
               extension.equals(".jpeg") ||
               extension.equals(".png") ||
               extension.equals(".gif") ||
               extension.equals(".webp");
    }

    // 응답 DTO
    public static class FileUploadResponse {
        private String url;
        private String filename;

        public FileUploadResponse(String url, String filename) {
            this.url = url;
            this.filename = filename;
        }

        public String getUrl() {
            return url;
        }

        public String getFilename() {
            return filename;
        }
    }
}
