<script setup>
import { ref } from "vue";
import axios from "axios";
import { ElMessage, genFileId } from "element-plus";

const uploadFileInfo = ref({
  info: "",
  fileList: [],
});

const info = ref("一次只能选择一张图片上传");
const upload = ref();
const uploadedFile = ref("");

const reset = () => {
  uploadFileInfo.value = {
    info: "",
    fileList: [],
  };
  info.value = "一次只能选择一张图片上传";
  uploadedFile.value = "";
};

const onSubmit = async () => {
  const file = uploadFileInfo.value.fileList[0];
  if (!file?.raw) {
    ElMessage.error("请先选择图片文件");
    return;
  }

  if (!canUpload(file.raw)) return;

  info.value = "开始提交文件上传";

  const formData = new FormData();
  formData.append("file", file.raw);
  formData.append("info", uploadFileInfo.value.info);

  try {
    const response = await axios.post("/file/upload", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });

    const result = response.data;
    info.value = result.message || "上传请求已完成";

    if (result.succeed) {
      info.value = "上传成功";
      uploadedFile.value = "/file/" + result.savedFileName;
    } else {
      uploadedFile.value = "";
    }
  } catch (error) {
    console.error("上传失败:", error);
    const status = error?.response?.status;
    const backendMessage =
      error?.response?.data?.message ||
      (typeof error?.response?.data === "string" ? error.response.data : "");
    info.value =
      "文件上传过程中发生错误：" +
      (status ? `HTTP ${status}` : "") +
      (backendMessage ? `，${backendMessage}` : "") +
      (!status && !backendMessage ? `，${error?.message || "未知错误"}` : "");
  }
};

const canUpload = (file) => {
  const isImage = file.type.startsWith("image/");
  if (!isImage) {
    ElMessage.error("只能上传图片文件");
    return false;
  }
  return true;
};

const handleExceed = (files) => {
  upload.value.clearFiles();
  const file = files[0];
  file.uid = genFileId();
  upload.value.handleStart(file);
};
</script>

<template>
  <div class="text-center text-2xl m-2 p-2">测试文件上传功能（保存到服务器的 MinIO 中）</div>
  <div class="bg-blue-200 m-2 p-2 text-center w-[800px] mx-auto">{{ info }}</div>

  <el-form :model="uploadFileInfo" label-width="auto" style="max-width: 600px; margin: 0 auto">
    <el-form-item label="附加信息">
      <el-input v-model="uploadFileInfo.info" />
    </el-form-item>

    <el-form-item label="上传文件">
      <el-upload
        ref="upload"
        v-model:file-list="uploadFileInfo.fileList"
        :auto-upload="false"
        :limit="1"
        :on-exceed="handleExceed"
        accept="image/*"
      >
        <template #trigger>
          <el-button type="primary">选择要上传的图片文件</el-button>
        </template>
      </el-upload>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="onSubmit">上传</el-button>
      <el-button @click="reset">重置</el-button>
    </el-form-item>
  </el-form>

  <div v-if="uploadedFile.length > 0" class="m-2 p-2 text-center">
    <img :src="uploadedFile" class="mx-auto w-[600px] rounded border" alt="上传的图片" />
  </div>
</template>
