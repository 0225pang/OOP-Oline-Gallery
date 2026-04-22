package com.jinxuliang.springvisitminio.model;

import lombok.*;

//这个类，用于向客户端返回已经保存在MinIO中的文件信息
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FileInfo {
    //上传文件对应的fileName
    private String fileName;
    //上传文件时，由客户端设置的附加信息
    private String info;
    private String mime;
}
