<template>
  <div v-if="visible" class="loading-bar" :style="barStyle">
    <div class="loading-progress" :style="{ width: progress + '%' }"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{
  loading?: boolean
  duration?: number
}>(), {
  loading: false,
  duration: 2000
})

const visible = ref(false)
const progress = ref(0)
let timer: number | null = null
let animationFrame: number | null = null

const barStyle = computed(() => ({
  opacity: visible.value ? 1 : 0,
  transition: 'opacity 0.3s ease'
}))

const startLoading = () => {
  visible.value = true
  progress.value = 0
  
  const startTime = Date.now()
  
  const animate = () => {
    const elapsed = Date.now() - startTime
    progress.value = Math.min(90, (elapsed / props.duration) * 100)
    
    if (progress.value < 90) {
      animationFrame = requestAnimationFrame(animate)
    }
  }
  
  animationFrame = requestAnimationFrame(animate)
}

const finishLoading = () => {
  progress.value = 100
  
  timer = window.setTimeout(() => {
    visible.value = false
    progress.value = 0
  }, 300)
}

const errorLoading = () => {
  progress.value = 100
  visible.value = false
  progress.value = 0
}

// 监听loading状态
watch(() => props.loading, (newVal) => {
  if (newVal) {
    startLoading()
  } else {
    finishLoading()
  }
})

// 暴露方法
defineExpose({
  start: startLoading,
  finish: finishLoading,
  error: errorLoading
})

onUnmounted(() => {
  if (timer) clearTimeout(timer)
  if (animationFrame) cancelAnimationFrame(animationFrame)
})
</script>

<style scoped>
.loading-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.1);
}

.loading-progress {
  height: 100%;
  background: linear-gradient(to right, #0ea5e9, #3b82f6);
  transition: width 0.3s ease;
  box-shadow: 0 0 10px rgba(14, 165, 233, 0.7);
}
</style>
