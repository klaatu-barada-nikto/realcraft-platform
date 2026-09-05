<script setup>
const examples = [
  {
    title: '正视图',
    cells: [
      [2, 1, '#7cbd4b'],
      [3, 1, '#7cbd4b'],
      [4, 1, '#7cbd4b'],
      [2, 2, '#7cbd4b'],
      [3, 2, '#7cbd4b'],
      [4, 2, '#7cbd4b'],
      [2, 3, '#79553a'],
      [3, 3, '#79553a'],
      [4, 3, '#79553a'],
    ],
  },
  {
    title: '侧视图',
    cells: [
      [3, 1, '#7cbd4b'],
      [3, 2, '#7cbd4b'],
      [3, 3, '#79553a'],
    ],
  },
  {
    title: '俯视图',
    cells: [
      [2, 2, '#7cbd4b'],
      [3, 2, '#7cbd4b'],
      [4, 2, '#7cbd4b'],
    ],
  },
]
</script>

<template>
  <section class="guide">
    <div class="guide-card">
      <p class="guide-text">
        请上传利用 AI 或工具生成的<strong>像素化三视图</strong>
        （正视图、侧视图、俯视图）
      </p>
      <div class="guide-examples">
        <div v-for="ex in examples" :key="ex.title" class="example-item">
          <div class="example-canvas">
            <svg viewBox="0 0 7 7" shape-rendering="crispEdges" aria-hidden="true">
              <rect
                v-for="(cell, i) in ex.cells"
                :key="i"
                :x="cell[0]"
                :y="cell[1]"
                width="1"
                height="1"
                :fill="cell[2]"
              />
            </svg>
          </div>
          <span class="example-label">{{ ex.title }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.guide {
  margin-bottom: 24px;
}

.guide-card {
  background: var(--rc-panel);
  border: 1px solid var(--rc-panel-border);
  border-radius: 12px;
  padding: 20px 24px;
}

.guide-text {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: var(--rc-text);
}

.guide-text strong {
  color: var(--el-color-primary);
  font-weight: 700;
}

.guide-examples {
  display: flex;
  gap: 20px;
  margin-top: 18px;
  justify-content: center;
}

.example-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.example-canvas {
  width: 64px;
  height: 64px;
  background: #101610;
  border: 1px solid var(--rc-panel-border);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
}

.example-canvas svg {
  width: 100%;
  height: 100%;
}

.example-label {
  font-size: 13px;
  color: var(--rc-text-dim);
}
</style>