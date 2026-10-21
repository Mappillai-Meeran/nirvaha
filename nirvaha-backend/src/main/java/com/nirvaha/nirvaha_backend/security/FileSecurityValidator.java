package com.nirvaha.nirvaha_backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class FileSecurityValidator {

    @Value("${nirvaha.max-evidence-size:10485760}") // Default 10MB
    private long maxFileSize;

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "webp"
    );

    // Magic Bytes signatures
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47};
    private static final byte[] RIFF_MAGIC = new byte[]{(byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46}; // WEBP starts with RIFF

    public void validateMultipartFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded evidence file cannot be empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of " + (maxFileSize / (1024 * 1024)) + "MB");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file format: " + mimeType + ". Only JPEG, PNG, and WEBP images are allowed.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file extension: ." + extension + ". Only jpg, jpeg, png, and webp are allowed.");
        }

        // Validate Magic Bytes
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 4 || !matchesMagicBytes(header, mimeType)) {
                throw new IllegalArgumentException("File signature verification failed. Content does not match claimed image format.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read file content for security validation", e);
        }

        // Validate Image Decoding (PNG / JPEG)
        if (!"image/webp".equalsIgnoreCase(mimeType)) {
            try (InputStream is = file.getInputStream()) {
                BufferedImage image = ImageIO.read(is);
                if (image == null) {
                    throw new IllegalArgumentException("File could not be decoded as a valid image");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Corrupted or malformed image file", e);
            }
        }
    }

    private boolean matchesMagicBytes(byte[] header, String mimeType) {
        if ("image/jpeg".equalsIgnoreCase(mimeType)) {
            return header[0] == JPEG_MAGIC[0] && header[1] == JPEG_MAGIC[1] && header[2] == JPEG_MAGIC[2];
        } else if ("image/png".equalsIgnoreCase(mimeType)) {
            return header[0] == PNG_MAGIC[0] && header[1] == PNG_MAGIC[1] && header[2] == PNG_MAGIC[2] && header[3] == PNG_MAGIC[3];
        } else if ("image/webp".equalsIgnoreCase(mimeType)) {
            return header[0] == RIFF_MAGIC[0] && header[1] == RIFF_MAGIC[1] && header[2] == RIFF_MAGIC[2] && header[3] == RIFF_MAGIC[3];
        }
        return false;
    }

    public String generateSecureFilename(String originalFilename) {
        String ext = getFileExtension(originalFilename);
        if (ext == null || ext.isEmpty()) {
            ext = "jpg";
        }
        return UUID.randomUUID().toString() + "." + ext.toLowerCase();
    }

    public String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    public void validateFile(MultipartFile file) {
        validateMultipartFile(file);
    }

    public String sanitizeFilename(String filename) {
        return generateSecureFilename(filename);
    }
}
