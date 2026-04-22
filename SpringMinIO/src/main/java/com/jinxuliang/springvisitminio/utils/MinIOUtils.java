package com.jinxuliang.springvisitminio.utils;
import io.minio.RemoveObjectArgs;

import io.minio.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

/**
 * MinIO工具类
 */
@Slf4j
public class MinIOUtils {

    //用于访问MinIO服务器
    private MinioClient minioClient;
    //连接MinIO服务器所需要的基本信息
    private final String endpoint;
    private final String minIoUserName;
    private final String minIoUserPwd;

    public MinIOUtils(String endpoint, String userName, String password) {
        this.endpoint = endpoint;
        this.minIoUserName = userName;
        this.minIoUserPwd = password;
        createMinioClient();
    }

    /**
     * 创建基于Java端的MinioClient
     */
    private void createMinioClient() {
        try {
            if (null == minioClient) {
                log.info("开始创建 MinioClient...");
                minioClient = MinioClient
                        .builder()
                        .endpoint(endpoint)
                        .credentials(minIoUserName, minIoUserPwd)
                        .build();
                log.info("创建完毕 MinioClient...");
            }
        } catch (Exception e) {
            log.error("MinIO服务器异常：{}", e);
        }
    }

    /**
     * 判断Bucket是否存在，true：存在，false：不存在
     */
    public boolean bucketExists(String bucketName) throws Exception {
        return minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName).build());
    }

    /**
     * 启动SpringBoot容器的时候初始化Bucket
     * 如果没有Bucket则创建
     */
    public void createBucket(String bucketName) throws Exception {
        if (!bucketExists(bucketName)) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName).build());
        }
    }

    /**
     * 获取指定名字的Bucket中的所有文件
     */
    public List<Item> listAllObjectInfo(String bucketName) throws Exception {
        var results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).recursive(true).build()
        );
        var fileList = new ArrayList<Item>();
        for (Result<Item> result : results) {
            Item item = result.get();
            fileList.add(item);
        }
        return fileList;
    }


    //获取指定名字的对象信息
    public StatObjectResponse getFileStatus(String bucketName, String objectName)
            throws Exception {
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
    }


    /**
     * 获取文件流
     */
    public InputStream getObject(String bucketName, String objectName)
            throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }


    /**
     * 上传文件，附加有元数据
     */
    public ObjectWriteResponse uploadFileWithMetaData(String bucketName, MultipartFile file,
                                                      String objectName, Map<String, String> metaData)
            throws Exception {
        InputStream inputStream = file.getInputStream();
        return minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .contentType(file.getContentType())
                        .stream(inputStream, inputStream.available(), -1)
                        .userMetadata(metaData)
                        .build());
    }

    public void removeObject(String bucketName, String objectName) throws Exception {
    minioClient.removeObject(
            RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
    );
}

}

