<script setup>
import GeneratePage from './views/GeneratePage.vue'
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <h1 class="app-title">RealCraft 像素三视图转换器</h1>
      <p class="app-subtitle">上传像素化三视图，生成 Minecraft 体素模型</p>
    </header>
    <main class="app-main">
      <GeneratePage />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  text-align: center;
  padding: 44px 20px 4px;
}

.app-title {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 3px;
  color: var(--rc-text);
}

.app-subtitle {
  margin: 10px 0 0;
  color: var(--rc-text-dim);
  font-size: 14px;
  letter-spacing: 1px;
}

.app-main {
  flex: 1;
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
  padding: 28px 20px 64px;
}
</style>