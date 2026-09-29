package com.kaveri.service.impl;

import com.kaveri.exception.BadRequestException;
import com.kaveri.service.ImageUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ImageUploadServiceImpl implements ImageUploadService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or missing");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file type. Only JPEG, PNG, and WebP are allowed");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("File size exceeds 5MB limit");
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String extension = getExtension(file.getOriginalFilename());
            if (!Set.of("jpg", "jpeg", "png", "webp").contains(extension)) {
                throw new BadRequestException("Invalid file extension. Only JPG, PNG, and WebP are allowed");
            }
            String filename = UUID.randomUUID() + "." + extension;
            Path targetPath = uploadPath.resolve(filename);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/uploads/" + filename;
            log.info("Image uploaded successfully: {}", imageUrl);
            return imageUrl;

        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to upload image: {}", e.getMessage());
            throw new BadRequestException("Failed to upload image. Please try again.");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
