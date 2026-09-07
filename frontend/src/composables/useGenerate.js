import { ref } from 'vue'
import { generateModel, fetchModel, GenerateError, NETWORK_ERROR } from '../api/generate'
import { parseVoxelData } from '../three/parseVoxelData'

export function useGenerate() {
  const fileList = ref([])
  const loading = ref(false)
  const result = ref(null)
  const error = ref('')
  const voxels = ref(null)
  const previewError = ref('')
  const previewLoading = ref(false)

  async function submit() {
    if (loading.value) {
      return
    }
    if (fileList.value.length === 0) {
      error.value = '请先选择图片'
      return
    }

    error.value = ''
    result.value = null
    voxels.value = null
    previewError.value = ''
    loading.value = true
    try {
      const jsonUrl = await generateModel(fileList.value)
      result.value = jsonUrl

      previewLoading.value = true
      try {
        const raw = await fetchModel(jsonUrl)
        const parsed = parseVoxelData(raw)
        voxels.value = parsed.blocks
      } catch (e) {
        voxels.value = null
        previewError.value = '预览数据加载失败'
      } finally {
        previewLoading.value = false
      }
    } catch (e) {
      error.value = e instanceof GenerateError ? e.message : NETWORK_ERROR
    } finally {
      loading.value = false
    }
  }

  function reset() {
    fileList.value = []
    result.value = null
    error.value = ''
    voxels.value = null
    previewError.value = ''
    previewLoading.value = false
  }

  return {
    fileList,
    loading,
    result,
    error,
    voxels,
    previewError,
    previewLoading,
    submit,
    reset,
  }
}
