package com.condos.board.api;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

@RestController
@RequestMapping("/condos/api/files")
@RequiredArgsConstructor
public class FilesController {

    private static final String BASE = "/condos/api/files/";
    private final MinioClient minio;

    @Value("${minio.bucket}") String bucket;

    private static String extractKey(HttpServletRequest req) {
        String uri = req.getRequestURI(); // p.ej. /condos/api/files/boards/.../a.png
        int idx = uri.indexOf(BASE);
        return (idx >= 0) ? uri.substring(idx + BASE.length()) : uri;
    }

    /** GET (descarga/visualización) */
    @GetMapping("/**")
    @PreAuthorize("@jwtAuth.isAuthenticated(authentication)")
    public void download(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String key = extractKey(req);
        // 1) Obtenemos metadata (content-type y tamaño)
        var stat = minio.statObject(
                StatObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build()
        );

        String ct = stat.contentType() != null && !stat.contentType().isBlank()
                ? stat.contentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        long size = stat.size();

        // 2) Seteamos headers *antes* de escribir cuerpo
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(ct);
        if (size > 0) resp.setContentLengthLong(size);
        resp.setHeader("Accept-Ranges", "bytes");
        resp.setHeader("Cache-Control", "private, max-age=86400, no-transform");

        String fileName = key.substring(key.lastIndexOf('/') + 1);
        resp.setHeader(
                "Content-Disposition",
                "inline; filename=\"" + fileName + "\"; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8));

        // 3) Copiamos el contenido binario al output stream
        try (var in = minio.getObject(
                GetObjectArgs.builder().bucket(bucket).object(key).build());
             var out = resp.getOutputStream()) {

            in.transferTo(out);
            out.flush();
        }
    }

    /** POST binario directo */
    @PostMapping("/**")
    @PreAuthorize("@jwtAuth.isAuthenticated(authentication)")
    public ResponseEntity<Void> uploadRawPOST(HttpServletRequest req,
                                              @RequestHeader("Content-Type") String contentType,
                                              @RequestBody InputStream body) throws Exception {
        String key = extractKey(req);
        minio.putObject(PutObjectArgs.builder()
                .bucket(bucket).object(key).contentType(contentType)
                .stream(body, -1, 10 * 1024 * 1024).build());
        return ResponseEntity.noContent().build();
    }

    /** PUT binario directo */
    @PutMapping("/**")
    @PreAuthorize("@jwtAuth.isAuthenticated(authentication)")
    public ResponseEntity<Void> uploadRawPUT(HttpServletRequest req,
                                             @RequestHeader("Content-Type") String contentType) {
        String key = extractKey(req);
        try (InputStream body = req.getInputStream()) {
            minio.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(key).contentType(contentType)
                    .stream(body, -1, 10 * 1024 * 1024).build());
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            System.err.printf("Upload failed key=%s, ct=%s, err=%s%n", key, contentType, ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}