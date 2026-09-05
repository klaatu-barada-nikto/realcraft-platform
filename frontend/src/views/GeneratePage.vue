<script setup>
import { useGenerate } from '../composables/useGenerate'
import GuideSection from '../components/GuideSection.vue'
import UploadPanel from '../components/UploadPanel.vue'
import ResultPanel from '../components/ResultPanel.vue'

const { fileList, loading, result, error, submit } = useGenerate()

function handleFilesChange(files) {
  fileList.value = files
}
</script>

<template>
  <div
    v-loading="loading"
    element-loading-text="正在生成方块模型..."
    class="generate-page"
  >
    <GuideSection />
    <UploadPanel
      :loading="loading"
      @change-files="handleFilesChange"
      @submit="submit"
    />
    <ResultPanel :result="result" :error="error" />
  </div>
</template>

<style scoped>
.generate-page {
  display: flex;
  flex-direction: column;
}
</style>