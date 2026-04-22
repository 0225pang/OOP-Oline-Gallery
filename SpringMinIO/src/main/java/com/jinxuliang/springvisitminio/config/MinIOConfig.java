package com.jinxuliang.springvisitminio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "minio")
public class MinIOConfig {
    private String endpoint;
    private String user;
    private String password;
    private String bucketName;
}

