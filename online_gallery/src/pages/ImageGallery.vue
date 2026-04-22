<script setup>
import { onMounted, ref } from "vue";
import axios from "axios";
import { ElMessage, ElMessageBox } from "element-plus";

const images = ref([]);
const previewVisible = ref(false);
const currentImage = ref(null);

function decodeRfc2047Q(value) {
  if (!value || typeof value !== "string") return value;

  const m = value.match(/^=\?([^?]+)\?Q\?([^?]*)\?=$/i);
  if (!m) return value;

  const charset = m[1];
  const encodedText = m[2];

  const bytes = [];
  for (let i = 0; i < encodedText.length; i++) {
    const ch = encodedText[i];
    if (ch === "_") {
      bytes.push(32);
    } else if (
      ch === "=" &&
      i + 2 < encodedText.length &&
      /[0-9A-Fa-f]{2}/.test(encodedText.slice(i + 1, i + 3))
    ) {
      bytes.push(parseInt(encodedText.slice(i + 1, i + 3), 16));
      i += 2;
    } else {
      bytes.push(ch.charCodeAt(0));
    }
  }

  try {
    return new TextDecoder(charset).decode(new Uint8Array(bytes));
  } catch (e) {
    console.error("解码失败:", e);
    return value;
  }
}

const loadImages = async () => {
  const response = await axios.get("/file/all");

  images.value = response.data.map((item) => ({
    ...item,
    info: decodeRfc2047Q(item.info),
  }));
};

const openPreview = (image) => {
  currentImage.value = image;
  previewVisible.value = true;
};

const closePreview = () => {
  previewVisible.value = false;
  currentImage.value = null;
};

const saveImage = async (image) => {
  try {
    const imageUrl = `/file/${image.fileName}`;
    const response = await axios.get(imageUrl, { responseType: "blob" });
    const blobUrl = window.URL.createObjectURL(response.data);

    const link = document.createElement("a");
    link.href = blobUrl;
    link.download = image.fileName || "image.jpg";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    window.URL.revokeObjectURL(blobUrl);
    ElMessage.success("图片已开始保存");
  } catch (error) {
    console.error("保存失败:", error);
    ElMessage.error("保存失败，请稍后重试");
  }
};

const deleteImageRequest = async (image) => {
  const fileName = image?.fileName == null ? "" : String(image.fileName).trim();
  if (!fileName) {
    throw new Error("missing fileName");
  }

  return axios.delete("/file", { params: { fileName } });
};

const deleteImage = async (image) => {
  try {
    await ElMessageBox.confirm(
      `确定删除图片「${image.info || image.fileName}」吗？删除后无法恢复。`,
      "删除确认",
      {
        confirmButtonText: "删除",
        cancelButtonText: "取消",
        type: "warning",
      }
    );

    await deleteImageRequest(image);
    images.value = images.value.filter((item) => item.fileName !== image.fileName);

    if (currentImage.value?.fileName === image.fileName) {
      closePreview();
    }

    ElMessage.success("图片已删除");
  } catch (error) {
    // 用户主动取消时不提示错误
    const action = error?.action;
    if (error === "cancel" || error === "close" || action === "cancel" || action === "close") return;

    const backendMessage =
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      error?.response?.data?.error ||
      error?.message;

    console.error("删除失败:", error);
    ElMessage.error(backendMessage ? `删除失败：${backendMessage}` : "删除失败，请检查后端删除接口");
  }
};

onMounted(loadImages);
</script>

<template>
  <div class="flex flex-wrap gap-4 p-4">
    <el-card v-for="image in images" :key="image.fileName" style="width: 320px;">
      <template #header>
        {{ image.info }}
      </template>
      <img
        :src="`/file/${image.fileName}`"
        style="width: 100%; cursor: zoom-in;"
        @click="openPreview(image)"
      />
      <div style="display: flex; justify-content: space-between; margin-top: 12px;">
        <el-button type="primary" plain size="small" @click="saveImage(image)">保存</el-button>
        <el-button type="danger" plain size="small" @click="deleteImage(image)">删除</el-button>
      </div>
    </el-card>
  </div>

  <el-dialog
    v-model="previewVisible"
    width="70%"
    :title="currentImage?.info || '图片预览'"
    destroy-on-close
    @closed="closePreview"
  >
    <div style="text-align: center;">
      <img
        v-if="currentImage"
        :src="`/file/${currentImage.fileName}`"
        alt="预览图"
        style="max-width: 100%; max-height: 70vh; border-radius: 8px;"
      />
    </div>
    <template #footer>
      <el-button @click="closePreview">关闭</el-button>
      <el-button v-if="currentImage" type="primary" @click="saveImage(currentImage)">
        保存图片
      </el-button>
      <el-button v-if="currentImage" type="danger" @click="deleteImage(currentImage)">
        删除图片
      </el-button>
    </template>
  </el-dialog>
</template>

