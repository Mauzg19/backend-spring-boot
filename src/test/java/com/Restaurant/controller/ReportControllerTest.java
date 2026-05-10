package com.Restaurant.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ReportControllerTest {

    @TempDir Path tmp;

    ReportController controller;

    @BeforeEach
    void setUp() {
        controller = new ReportController(tmp.toString());
    }

    @Test
    void download_invalidFilename_returnsBadRequest() {
        ResponseEntity<Resource> r = controller.download("../etc/passwd");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void download_pathTraversalAttempt_returnsBadRequest() {
        ResponseEntity<Resource> r = controller.download("..%2F..%2Fsecret.csv");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void download_specialChars_returnsBadRequest() {
        ResponseEntity<Resource> r = controller.download("file with space.csv");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void download_wrongExtension_returnsBadRequest() {
        ResponseEntity<Resource> r = controller.download("file.exe");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void download_validNameFileMissing_returnsNotFound() {
        ResponseEntity<Resource> r = controller.download("nonexistent_x.csv");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void download_existingCsv_returnsResourceWithCsvHeaders() throws IOException {
        Path file = tmp.resolve("sales_1_abc.csv");
        Files.writeString(file, "id,total\n1,500\n");

        ResponseEntity<Resource> r = controller.download("sales_1_abc.csv");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType().toString()).contains("text/csv");
        assertThat(r.getHeaders().getFirst("Content-Disposition"))
                .contains("attachment")
                .contains("sales_1_abc.csv");
    }

    @Test
    void download_existingTxt_returnsTextPlainHeaders() throws IOException {
        Path file = tmp.resolve("menu_2_xyz.txt");
        Files.writeString(file, "menu content");

        ResponseEntity<Resource> r = controller.download("menu_2_xyz.txt");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType().toString()).contains("text/plain");
    }
}
