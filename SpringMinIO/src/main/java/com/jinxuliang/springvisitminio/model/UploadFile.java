package com.jinxuliang.springvisitminio.model;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UploadFile {
    private String fileName;
    private String info;
    private byte[] content;
}
