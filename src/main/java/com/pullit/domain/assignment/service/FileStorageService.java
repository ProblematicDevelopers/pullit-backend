package com.pullit.domain.assignment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
    
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;
    
    @Value("${file.max-size:52428800}") // 50MB default
    private long maxFileSize;
    
    private static final String[] ALLOWED_EXTENSIONS = {
        "pdf", "doc", "docx", "hwp", "txt", "zip", "png", "jpg", "jpeg"
    };
    
    public String storeFile(MultipartFile file, String subDirectory) throws IOException {
        // 파일 확장자 검증
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        
        if (!isAllowedExtension(fileExtension)) {
            throw new IllegalArgumentException("허용되지 않은 파일 형식입니다: " + fileExtension);
        }
        
        // 파일 크기 검증
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("파일 크기가 너무 큽니다. 최대 " + (maxFileSize / 1024 / 1024) + "MB까지 허용됩니다.");
        }
        
        // 저장 경로 생성
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path uploadPath = Paths.get(uploadDir, subDirectory, dateFolder);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 고유한 파일명 생성
        String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName;
        Path targetLocation = uploadPath.resolve(storedFileName);
        
        // 파일 저장
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("파일 저장 완료: {} -> {}", originalFileName, targetLocation);
        
        return Paths.get(subDirectory, dateFolder, storedFileName).toString();
    }
    
    public Resource loadFileAsResource(String filePath) throws IOException {
        try {
            Path file = Paths.get(uploadDir).resolve(filePath).normalize();
            Resource resource = new UrlResource(file.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IOException("파일을 찾을 수 없거나 읽을 수 없습니다: " + filePath);
            }
        } catch (MalformedURLException ex) {
            throw new IOException("파일 경로가 올바르지 않습니다: " + filePath, ex);
        }
    }
    
    public void deleteFile(String filePath) throws IOException {
        Path file = Paths.get(uploadDir).resolve(filePath).normalize();
        Files.deleteIfExists(file);
        log.info("파일 삭제 완료: {}", filePath);
    }
    
    public boolean fileExists(String filePath) {
        Path file = Paths.get(uploadDir).resolve(filePath).normalize();
        return Files.exists(file) && Files.isReadable(file);
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    private boolean isAllowedExtension(String extension) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
    
    public String getContentType(String fileName) {
        String extension = getFileExtension(fileName);
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "hwp" -> "application/x-hwp";
            case "txt" -> "text/plain";
            case "zip" -> "application/zip";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }
}