# gallery_app（Jetpack Compose Android 客户端）

本项目是基于 Jetpack Compose 编写的 Android 客户端，已对接 `SpringMinIO` 后端接口，实现以下功能：

- App 拍照并保存到本地系统相册
- 从本地相册选择图片
- 上传图片到 Spring 后端（MinIO 持久化）
- 在线浏览服务器全部图片

---

## 1. 功能清单

### 1.1 拍照并保存本地相册

- 使用 `ActivityResultContracts.TakePicture()`
- 通过 `MediaStore` 预先创建目标 `Uri`
- 拍摄完成后图片保存在系统图库 `Pictures/GalleryComposeApp`

### 1.2 相册选图

- 使用 `ActivityResultContracts.PickVisualMedia()`
- 支持选择系统相册中的图片资源

### 1.3 上传到 SpringMinIO

- 调用接口：`POST /file/upload`
- 表单字段：
  - `file`：二进制图片文件
  - `info`：文本描述（页面输入框）
- 上传成功后自动刷新在线图库

### 1.4 在线浏览图片

- 调用接口：`GET /file/all` 获取列表
- 列表图片地址拼接：`GET /file/{objectName}`
- 在 Compose 网格中展示图片缩略图、文件名、info 信息

---

## 2. 后端接口对应关系（SpringMinIO）

客户端使用的后端接口与 `SpringMinIO` 中 `FileController` 对应：

- `POST /file/upload`
- `GET /file/all`
- `GET /file/{objectName}`

默认客户端基地址：

- 模拟器：`http://10.0.2.2:8080/`

代码位置：

- `app/src/main/java/com/example/galleryapp/network/GalleryApi.kt`

---

## 3. 工程结构

```text
gallery_app
├─ app
│  ├─ build.gradle
│  └─ src/main
│     ├─ AndroidManifest.xml
│     ├─ java/com/example/galleryapp
│     │  ├─ MainActivity.kt
│     │  └─ network
│     │     ├─ GalleryApi.kt
│     │     └─ GalleryRepository.kt
│     └─ res
│        ├─ values/strings.xml
│        ├─ values/themes.xml
│        └─ xml/network_security_config.xml
├─ build.gradle
├─ settings.gradle
├─ gradle.properties
└─ local.properties
```

---

## 4. 国内镜像配置（已完成）

为了避免国外源慢，工程已切换阿里云镜像：

- `https://maven.aliyun.com/repository/gradle-plugin`
- `https://maven.aliyun.com/repository/google`
- `https://maven.aliyun.com/repository/central`
- `https://maven.aliyun.com/repository/public`

配置文件：

- `settings.gradle`

---

## 5. 构建环境说明

本次实际打包环境：

- Gradle：`9.3.1`
- JDK：`25`（系统已有）
- Android SDK：`C:\Users\Lenovo\AppData\Local\Android\Sdk`
- compileSdk / targetSdk：`34`
- minSdk：`24`

`local.properties` 已配置：

```properties
sdk.dir=C\:\\Users\\Lenovo\\AppData\\Local\\Android\\Sdk
```

---

## 6. 如何构建 APK

在项目根目录执行：

```powershell
& 'C:\Users\Lenovo\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat' assembleDebug
```

如你的环境里存在代理变量指向无效地址（例如 `127.0.0.1:9`），建议先清空：

```powershell
$env:HTTP_PROXY=''
$env:HTTPS_PROXY=''
$env:ALL_PROXY=''
$env:GIT_HTTP_PROXY=''
$env:GIT_HTTPS_PROXY=''
```

---

## 7. APK 产物位置

Debug APK 已成功生成：

- `app/build/outputs/apk/debug/app-debug.apk`

绝对路径：

- `H:\OOP\Online\gallery_app\app\build\outputs\apk\debug\app-debug.apk`

---

## 8. 运行与联调说明

### 8.1 启动后端

先运行 `SpringMinIO` 项目，确保服务监听 `8080` 端口。

### 8.2 模拟器与真机地址差异

- Android 模拟器访问宿主机：`10.0.2.2`
- 如果真机调试，请把 `GalleryApi.kt` 的 `BASE_URL` 改为你电脑局域网 IP（例如 `http://192.168.x.x:8080/`）

### 8.3 权限

Manifest 中已声明：

- `INTERNET`
- `CAMERA`
- `READ_MEDIA_IMAGES`（Android 13+）
- `READ_EXTERNAL_STORAGE`（Android 12 及以下）

---

## 9. 页面交互说明

主页面按钮：

- `拍照`：打开系统相机拍摄并落盘本地相册
- `相册选图`：打开系统图库选择图片
- `上传到 SpringMinIO`：上传当前选中的图片及 info
- `刷新在线图库`：重新拉取后端 `file/all` 列表

页面还包含：

- 选中图片预览区
- 上传状态/错误提示文本
- 在线图片网格（含文件名、info）

---

## 10. 已知提示

- 构建过程中有一个 Kotlin 警告（ViewModel Factory 的泛型强转），不影响功能和 APK 产出。
- Gradle 会提示部分“未来 Gradle 10 不兼容”的 deprecations，当前构建不受影响。

