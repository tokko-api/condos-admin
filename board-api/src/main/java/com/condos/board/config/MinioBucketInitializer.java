package com.condos.board.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioBucketInitializer {

    private final MinioClient minio;

    @Value("${minio.bucket}")
    private String bucket;

    @Bean
    ApplicationRunner createBucketIfMissing() {
        return args -> {
            boolean exists = minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                log.info("MinIO bucket '{}' not found. Creating…", bucket);
                minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket '{}' created.", bucket);
            } else {
                log.info("MinIO bucket '{}' already exists.", bucket);
            }
        };
    }
}