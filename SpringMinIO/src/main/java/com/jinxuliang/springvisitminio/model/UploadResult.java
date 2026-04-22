package com.jinxuliang.springvisitminio.model;

import lombok.Builder;
import lombok.Data;

//此类封装上传结束后，服务端发给客户端的信息
@Data
@Builder
public class UploadResult {
    private boolean succeed;
    private String message;
    private String savedFileName;
}
