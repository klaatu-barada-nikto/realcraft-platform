import { ref } from 'vue'
import { generateModel, GenerateError, NETWORK_ERROR } from '../api/generate'

export function useGenerate() {
  const fileList = ref([])
  const loading = ref(false)
  const result = ref(null)
  const error = ref('')

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
    loading.value = true
    try {
      const jsonUrl = await generateModel(fileList.value)
      result.value = jsonUrl
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
  }

  return { fileList, loading, result, error, submit, reset }
}