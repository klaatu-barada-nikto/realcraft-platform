<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { VoxelScene } from '../three/voxelScene'

const props = defineProps({
  voxels: { type: Array, default: null },
})

const containerRef = ref(null)
const webglSupported = ref(true)
const renderLoading = ref(false)

let scene = null
let pendingRender = null

function render(blocks) {
  if (!scene || !webglSupported.value) {
    return
  }
  renderLoading.value = true
  if (pendingRender) {
    cancelAnimationFrame(pendingRender)
  }
  pendingRender = requestAnimationFrame(() => {
    pendingRender = null
    if (!scene) {
      renderLoading.value = false
      return
    }
    scene.setVoxels(blocks)
    renderLoading.value = false
  })
}

watch(
  () => props.voxels,
  (blocks) => {
    if (blocks && blocks.length > 0) {
      render(blocks)
    } else if (scene) {
      scene.clear()
    }
  },
)

onMounted(() => {
  scene = new VoxelScene(containerRef.value)
  const result = scene.mount()
  webglSupported.value = result.supported
  if (!result.supported) {
    scene = null
    return
  }
  if (props.voxels && props.voxels.length > 0) {
    render(props.voxels)
  }
})

onUnmounted(() => {
  if (pendingRender) {
    cancelAnimationFrame(pendingRender)
    pendingRender = null
  }
  if (scene) {
    scene.dispose()
    scene = null
  }
})
</script>

<template>
  <div class="voxel-preview">
    <div v-if="!webglSupported" class="preview-fallback">
      当前浏览器不支持 3D 预览
    </div>
    <div v-else class="preview-container">
      <div ref="containerRef" class="preview-canvas"></div>
      <div v-if="renderLoading" class="preview-loading">
        <span>模型加载中...</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.voxel-preview {
  margin-top: 16px;
}

.preview-container {
  position: relative;
  width: 100%;
  height: 360px;
  border: 1px solid var(--rc-panel-border);
  border-radius: 8px;
  overflow: hidden;
  background: #0d120d;
}

.preview-canvas {
  width: 100%;
  height: 100%;
}

.preview-canvas :deep(canvas) {
  display: block;
}

.preview-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(13, 18, 13, 0.6);
  color: var(--rc-text-dim);
  font-size: 14px;
}

.preview-fallback {
  padding: 24px;
  text-align: center;
  color: var(--rc-text-dim);
  border: 1px dashed var(--rc-panel-border);
  border-radius: 8px;
}
</style>