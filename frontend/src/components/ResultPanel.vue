<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { copyText } from '../utils/clipboard'
import VoxelPreview from './VoxelPreview.vue'

const props = defineProps({
  result: { type: String, default: null },
  error: { type: String, default: '' },
  voxels: { type: Array, default: null },
})

const copied = ref(false)

const command = computed(() => (props.result ? `/buildmodel ${props.result}` : ''))

async function handleCopy() {
  if (!command.value) {
    return
  }
  const res = await copyText(command.value)
  if (res.ok) {
    copied.value = true
    ElMessage.success('复制成功')
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } else {
    selectCommand()
  }
}

function selectCommand() {
  const el = document.getElementById('command-text')
  if (!el) {
    return
  }
  const range = document.createRange()
  range.selectNodeContents(el)
  const selection = window.getSelection()
  selection.removeAllRanges()
  selection.addRange(range)
}
</script>

<template>
  <div v-if="result || error" class="result">
    <div v-if="result" class="result-card result-success">
      <p class="result-title">转换成功</p>
      <p class="result-url">{{ result }}</p>
      <div class="command-box">
        <code id="command-text" class="command-text">{{ command }}</code>
        <el-button type="primary" @click="handleCopy">
          {{ copied ? '已复制' : '复制指令' }}
        </el-button>
      </div>
      <p class="command-tip">在游戏内输入该指令，即可导入并构建体素模型</p>
      <VoxelPreview v-if="voxels && voxels.length > 0" :voxels="voxels" />
    </div>

    <el-alert
      v-if="error"
      class="result-error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
    />
  </div>
</template>

<style scoped>
.result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-card {
  background: var(--rc-panel);
  border: 1px solid var(--rc-panel-border);
  border-radius: 12px;
  padding: 24px;
}

.result-success {
  border-color: rgba(124, 189, 75, 0.4);
}

.result-title {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.result-url {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--rc-text-dim);
  word-break: break-all;
}

.command-box {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.command-text {
  flex: 1;
  min-width: 220px;
  background: #101610;
  border: 1px solid var(--rc-panel-border);
  border-radius: 8px;
  padding: 12px 14px;
  font-family: 'Consolas', 'Menlo', monospace;
  font-size: 14px;
  color: var(--rc-accent);
  word-break: break-all;
}

.command-tip {
  margin: 12px 0 0;
  font-size: 13px;
  color: var(--rc-text-dim);
}
</style>