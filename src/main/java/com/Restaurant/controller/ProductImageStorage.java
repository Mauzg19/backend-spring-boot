package com.Restaurant.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ProductImageStorage {
	private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

	private final Path imageDirectory;

	public ProductImageStorage(@Value("${app.uploads.path:./uploads}") String uploadsPath) {
		this.imageDirectory = Path.of(uploadsPath).toAbsolutePath().normalize().resolve("product-images");
	}

	public String save(MultipartFile image) throws IOException {
		if (image.isEmpty()) {
			throw new IllegalArgumentException("La imagen está vacía.");
		}
		if (image.getSize() > MAX_IMAGE_SIZE) {
			throw new IllegalArgumentException("La imagen no puede superar los 10 MB.");
		}

		String originalFilename = image.getOriginalFilename();
		String extension = originalFilename == null || !originalFilename.contains(".")
				? ""
				: originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new IllegalArgumentException("Formato de imagen no permitido.");
		}

		Files.createDirectories(imageDirectory);
		String filename = UUID.randomUUID() + "." + extension;
		try (var inputStream = image.getInputStream()) {
			Files.copy(inputStream, imageDirectory.resolve(filename));
		}
		return "/uploads/product-images/" + filename;
	}

	public String getImageDirectoryLocation() {
		String location = imageDirectory.toUri().toString();
		return location.endsWith("/") ? location : location + "/";
	}
}
