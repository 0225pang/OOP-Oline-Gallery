package com.jinxuliang.springvisitminio.repository;

import com.jinxuliang.springvisitminio.config.MinIOConfig;
import com.jinxuliang.springvisitminio.model.FileInfo;
import com.jinxuliang.springvisitminio.utils.MinIOUtils;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
@Slf4j
public class MinIoRepository {

    private static final Pattern RFC2047_ENCODED_WORD =
            Pattern.compile("=\\?([^?]+)\\?([bBqQ])\\?([^?]*)\\?=");

    private final MinIOConfig minIoConfig;
    private MinIOUtils minIOUtils = null;

    public MinIoRepository(MinIOConfig config) {
        this.minIoConfig = config;
        try {
            log.info("开始创建 MinioClient...");
            minIOUtils = new MinIOUtils(
                    minIoConfig.getEndpoint(),
                    minIoConfig.getUser(),
                    minIoConfig.getPassword());
            log.info("开始创建 Bucket...");
            minIOUtils.createBucket(minIoConfig.getBucketName());
            log.info("Minio 客户端初始化完毕...");

        } catch (Exception e) {
            log.error("MinIO 服务异常", e);
        }
    }

    public void uploadFile(MultipartFile file, String objectName, String info)
            throws Exception {
        Map<String, String> metaData = Map.of("info", info);
        minIOUtils.uploadFileWithMetaData(minIoConfig.getBucketName(),
                file, objectName, metaData);
    }

    public InputStream getFile(String objectName) throws Exception {
        return minIOUtils.getObject(minIoConfig.getBucketName(), objectName);
    }

    public String getFileMime(String objectName) throws Exception {
        var info = minIOUtils.getFileStatus(minIoConfig.getBucketName(), objectName);
        return info.contentType();
    }

    public void deleteFile(String objectName) throws Exception {
        minIOUtils.removeObject(minIoConfig.getBucketName(), objectName);
    }

    public List<FileInfo> getAllFiles(String bucketName) {
        List<FileInfo> fileInfoList = new ArrayList<>();
        try {
            List<Item> items = minIOUtils.listAllObjectInfo(bucketName);
            for (Item item : items) {
                var status = minIOUtils.getFileStatus(bucketName, item.objectName());
                String mime = status.contentType();
                String info = "无";
                if (status.userMetadata() != null && status.userMetadata().get("info") != null) {
                    info = decodeRfc2047Text(status.userMetadata().get("info"));
                }
                FileInfo fileInfo = FileInfo.builder()
                        .fileName(item.objectName())
                        .info(info)
                        .mime(mime)
                        .build();
                fileInfoList.add(fileInfo);
            }
        } catch (Exception e) {
            log.error("无法读取 bucket 文件: {}", bucketName, e);
        }
        return fileInfoList;
    }

    private String decodeRfc2047Text(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        Matcher matcher = RFC2047_ENCODED_WORD.matcher(value);
        if (!matcher.find()) {
            return value;
        }

        StringBuilder decoded = new StringBuilder();
        int lastEnd = 0;
        matcher.reset();

        while (matcher.find()) {
            decoded.append(value, lastEnd, matcher.start());
            decoded.append(decodeEncodedWord(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(0)));
            lastEnd = matcher.end();
        }

        decoded.append(value.substring(lastEnd));
        return decoded.toString();
    }

    private String decodeEncodedWord(String charsetName, String encoding, String encodedText, String rawValue) {
        try {
            Charset charset = Charset.forName(charsetName);
            if ("B".equalsIgnoreCase(encoding)) {
                byte[] bytes = Base64.getDecoder().decode(encodedText);
                return new String(bytes, charset);
            }

            byte[] bytes = decodeQEncodedText(encodedText);
            return new String(bytes, charset);
        } catch (Exception e) {
            log.warn("解码元数据失败，保持原值: {}", rawValue, e);
            return rawValue;
        }
    }

    private byte[] decodeQEncodedText(String text) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '_') {
                out.write(' ');
                continue;
            }

            if (ch == '=' && i + 2 < text.length()) {
                int hi = Character.digit(text.charAt(i + 1), 16);
                int lo = Character.digit(text.charAt(i + 2), 16);
                if (hi >= 0 && lo >= 0) {
                    out.write((hi << 4) + lo);
                    i += 2;
                    continue;
                }
            }

            out.write((byte) ch);
        }
        return out.toByteArray();
    }
}
