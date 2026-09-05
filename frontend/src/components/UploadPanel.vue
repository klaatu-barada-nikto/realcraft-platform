<script setup>
import { ElMessage } from 'element-plus'

defineProps({
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['change-files', 'submit'])

function handleChange(uploadFile, uploadFiles) {
  emit('change-files', uploadFiles.map((f) => f.raw).filter(Boolean))
}

function handleRemove(uploadFile, uploadFiles) {
  emit('change-files', uploadFiles.map((f) => f.raw).filter(Boolean))
}

function handleExceed() {
  ElMessage.warning('最多上传 3 张图片')
}

function handleSubmit() {
  emit('submit')
}
</script>

<template>
  <section class="upload">
    <div class="upload-card">
      <el-upload
        list-type="picture-card"
        :auto-upload="false"
        :limit="3"
        accept="image/*"
        :on-change="handleChange"
        :on-remove="handleRemove"
        :on-exceed="handleExceed"
      >
        <div class="upload-trigger">
          <span class="upload-plus">+</span>
          <span class="upload-hint">选择图片</span>
        </div>
      </el-upload>

      <div class="upload-actions">
        <el-button
          type="primary"
          size="large"
          :disabled="loading"
          :loading="loading"
          @click="handleSubmit"
        >
          生成方块模型
        </el-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.upload {
  margin-bottom: 24px;
}

.upload-card {
  background: var(--rc-panel);
  border: 1px solid var(--rc-panel-border);
  border-radius: 12px;
  padding: 24px;
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.upload-plus {
  font-size: 28px;
  line-height: 1;
  color: var(--el-color-primary);
}

.upload-hint {
  font-size: 13px;
  color: var(--rc-text-dim);
}

.upload-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>