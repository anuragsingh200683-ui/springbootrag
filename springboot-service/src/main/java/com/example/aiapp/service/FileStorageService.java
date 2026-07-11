package com.example.aiapp.service;

import com.example.aiapp.exception.FileStorageException;
import com.example.aiapp.exception.InvalidFileTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory: " + this.uploadRoot, e);
        }
    }

    public String storePdf(MultipartFile file, UUID documentId) {
        if (file.isEmpty()) {
            throw new InvalidFileTypeException("Uploaded file is empty.");
        }
        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename() == null ? "document.pdf" : file.getOriginalFilename();
        boolean looksLikePdf = "application/pdf".equals(contentType) || originalName.toLowerCase().endsWith(".pdf");
        if (!looksLikePdf) {
            throw new InvalidFileTypeException("Only PDF files are supported. Received: " + contentType);
        }

        String safeFileName = documentId + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = uploadRoot.resolve(safeFileName);

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store uploaded file: " + originalName, e);
        }

        log.info("Stored uploaded PDF at {}", target);
        return target.toString();
    }

    /**
     * Deletes a previously stored PDF from disk. Idempotent: a missing file is logged
     * and ignored rather than treated as an error, since the goal (file no longer present)
     * is already satisfied.
     */
    public void deleteFile(String filePath) {
        Path target = Paths.get(filePath);
        try {
            boolean removed = Files.deleteIfExists(target);
            if (removed) {
                log.info("Deleted stored PDF at {}", target);
            } else {
                log.warn("Stored PDF not found at {} (already removed?)", target);
            }
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete stored file: " + target, e);
        }
    }
}
