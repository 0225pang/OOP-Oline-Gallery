package com.jinxuliang.springvisitminio.controller;

import com.jinxuliang.springvisitminio.model.FileInfo;
import com.jinxuliang.springvisitminio.model.UploadResult;
import com.jinxuliang.springvisitminio.repository.MinIoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/file")
@CrossOrigin(origins = "http://localhost:5173")
@Slf4j
public class FileController {

    @Autowired
    private MinIoRepository minIoRepository;

    @PostMapping("upload")
    public UploadResult uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("info") String info) throws Exception {
        minIoRepository.uploadFile(file, file.getOriginalFilename(), info);
        return UploadResult.builder()
                .savedFileName(file.getOriginalFilename())
                .succeed(true)
                .build();
    }

    @GetMapping("{objectName}")
    public ResponseEntity<byte[]> getFile(@PathVariable("objectName") String objectName) {
        try {
            InputStream inputStream = minIoRepository.getFile(objectName);
            byte[] fileBytes = inputStream.readAllBytes();

            HttpHeaders headers = new HttpHeaders();
            String contentType = minIoRepository.getFileMime(objectName);
            if (contentType != null) {
                headers.setContentType(MediaType.parseMediaType(contentType));
            } else {
                headers.setContentType(MediaType.IMAGE_JPEG);
            }

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to get file: {}", objectName, e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping({"", "delete"})
    public ResponseEntity<UploadResult> deleteImage(@RequestParam("fileName") String fileName) {
        return deleteInternal(fileName);
    }

    @GetMapping("all")
    public List<FileInfo> getAllImage() {
        return minIoRepository.getAllFiles("myfiles");
    }

    private ResponseEntity<UploadResult> deleteInternal(String objectName) {
        if (objectName == null || objectName.isBlank() || "undefined".equalsIgnoreCase(objectName)) {
            return ResponseEntity.badRequest()
                    .body(UploadResult.builder()
                            .succeed(false)
                            .message("missing fileName")
                            .build());
        }

        try {
            minIoRepository.deleteFile(objectName);
            return ResponseEntity.ok(UploadResult.builder()
                    .succeed(true)
                    .savedFileName(objectName)
                    .message("delete success")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete file: {}", objectName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(UploadResult.builder()
                            .succeed(false)
                            .savedFileName(objectName)
                            .message("delete failed")
                            .build());
        }
    }
}
