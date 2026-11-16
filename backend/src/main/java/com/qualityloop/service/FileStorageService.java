package com.qualityloop.service;

import com.qualityloop.exception.BadRequestException;
import com.qualityloop.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

// Saves uploaded files safely. Three rules:
// 1) only allowed extensions, 2) the real file bytes must match the extension,
// 3) the name on disk is a random UUID, so a user can never choose a path.
@Service
public class FileStorageService {

    public record StoredFile(String storedName, String originalName, String contentType, long size) {
    }

    private static final Map<String, String> TYPES = Map.of(
            "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg", "pdf", "application/pdf",
            "dwg", "application/octet-stream", "dxf", "application/octet-stream");

    public static final Set<String> EVIDENCE = Set.of("png", "jpg", "jpeg", "pdf");
    public static final Set<String> DRAWING = Set.of("png", "jpg", "jpeg", "pdf", "dwg", "dxf");

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public StoredFile save(MultipartFile file, Set<String> allowed) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Please choose a file");

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        if (original.contains("..")) throw new BadRequestException("Invalid file name");
        original = original.substring(original.lastIndexOf('/') + 1);
        if (original.length() > 200) original = original.substring(original.length() - 200);

        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase();
        if (!allowed.contains(ext))
            throw new BadRequestException("File type not allowed. Allowed types: " + String.join(", ", new TreeSet<>(allowed)));

        try {
            byte[] head;
            try (InputStream in = file.getInputStream()) {
                head = in.readNBytes(8);
            }
            if (!signatureMatches(ext, head))
                throw new BadRequestException("The file content does not match its type ." + ext);

            String stored = UUID.randomUUID() + "." + ext;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, root.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(stored, original, TYPES.get(ext), file.getSize());
        } catch (IOException e) {
            throw new BadRequestException("The file could not be saved");
        }
    }

    public Resource load(String storedName) {
        try {
            Path p = root.resolve(storedName).normalize();
            if (!p.startsWith(root) || !Files.exists(p)) throw new NotFoundException("The file was not found on the server");
            return new UrlResource(p.toUri());
        } catch (MalformedURLException e) {
            throw new NotFoundException("The file was not found on the server");
        }
    }

    public void delete(String storedName) {
        try {
            Files.deleteIfExists(root.resolve(storedName).normalize());
        } catch (IOException ignored) {
            // a leftover file is harmless, the database row is what matters
        }
    }

    // Looks at the first bytes of the file (its "magic number"), not at the name the user gave
    private boolean signatureMatches(String ext, byte[] h) {
        switch (ext) {
            case "png":
                return h.length >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            case "jpg":
            case "jpeg":
                return h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            case "pdf":
                return h.length >= 4 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F';
            case "dwg":
                return h.length >= 2 && h[0] == 'A' && h[1] == 'C';
            default:
                return true; // dxf is plain text
        }
    }
}
