package com.Restaurant.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9_-]+\\.(csv|txt)$");
    private final Path reportsDir;

    public ReportController(@Value("${app.reports.path:./reports}") String path) {
        this.reportsDir = Paths.get(path).toAbsolutePath().normalize();
    }

    @GetMapping("/{filename}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        if (!SAFE_FILENAME.matcher(filename).matches()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Path file = reportsDir.resolve(filename).normalize();
            if (!file.startsWith(reportsDir) || !Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(file.toUri());
            String contentType = filename.endsWith(".csv")
                    ? "text/csv" : "text/plain";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
