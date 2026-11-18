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
import com.Restaurant.domain.USER_ROLE;
import com.Restaurant.service.RestaurantService;
import com.Restaurant.service.UserService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9_-]+\\.(csv|txt)$");
    private static final Pattern RESTAURANT_REPORT = Pattern.compile("^(sales|menu)_(\\d+)_\\d+\\.(csv|txt)$");
    private final Path reportsDir;
    private final UserService users;
    private final RestaurantService restaurants;

    public ReportController(@Value("${app.reports.path:./reports}") String path,
            UserService users, RestaurantService restaurants) {
        this.reportsDir = Paths.get(path).toAbsolutePath().normalize();
        this.users = users;
        this.restaurants = restaurants;
    }

    @GetMapping("/{filename}")
    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT_OWNER')")
    public ResponseEntity<Resource> download(@PathVariable String filename,
            @RequestHeader("Authorization") String jwt) throws Exception {
        if (!SAFE_FILENAME.matcher(filename).matches()) {
            return ResponseEntity.badRequest().build();
        }
        var actor = users.findUserProfileByJwt(jwt);
        if (actor.getRole() != USER_ROLE.ROLE_ADMIN) {
            var own = restaurants.getRestaurantsByUserId(actor.getId());
            var report = RESTAURANT_REPORT.matcher(filename);
            if (own == null || !report.matches() || !own.getId().toString().equals(report.group(2))) {
                return ResponseEntity.notFound().build();
            }
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
