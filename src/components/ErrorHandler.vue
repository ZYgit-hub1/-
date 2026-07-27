<template>
  <Teleport to="body">
    <Transition name="toast">
      <div v-if="visible" class="error-toast" :class="typeClass">
        <div class="error-content">
          <el-icon class="error-icon">
            <CircleCloseFilled v-if="type === 'error'" />
            <WarningFilled v-else-if="type === 'warning'" />
            <SuccessFilled v-else />
          </el-icon>
          <div class="error-message">
            <div v-if="title" class="error-title">{{ title }}</div>
            <div class="error-desc">{{ message }}</div>
          </div>
          <el-button 
            v-if="showClose" 
            text 
            class="close-btn"
            @click="close"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div v-if="type === 'error' || type === 'warning'" class="error-action">
          <el-button size="small" type="primary" @click="handleRetry">
            重试
          </el-button>
          <el-button size="small" @click="close">
            关闭
          </el-button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { CircleCloseFilled, WarningFilled, SuccessFilled, Close } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  visible: boolean
  type?: 'error' | 'warning' | 'success'
  title?: string
  message?: string
  duration?: number
  showClose?: boolean
  onClose?: () => void
  onRetry?: () => void
}>(), {
  type: 'error',
  duration: 5000,
  showClose: true
})

const typeClass = computed(() => {
  const classes = {
    error: 'error-toast--error',
    warning: 'error-toast--warning',
    success: 'error-toast--success'
  }
  return classes[props.type]
})

const close = () => {
  props.onClose?.()
}

const handleRetry = () => {
  props.onRetry?.()
  close()
}

let timer: number | null = null

watch(() => props.visible, (newVal) => {
  if (newVal && props.duration > 0) {
    timer = window.setTimeout(() => {
      close()
    }, props.duration)
  }
})

onMounted(() => {
  if (props.visible && props.duration > 0) {
    timer = window.setTimeout(() => {
      close()
    }, props.duration)
  }
})
</script>

<style scoped>
.error-toast {
  position: fixed;
  top: 80px;
  right: 20px;
  max-width: 400px;
  z-index: 10000;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.error-toast--error {
  border-left: 4px solid #ef4444;
}

.error-toast--warning {
  border-left: 4px solid #f59e0b;
}

.error-toast--success {
  border-left: 4px solid #10b981;
}

.error-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
}

.error-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.error-toast--error .error-icon {
  color: #ef4444;
}

.error-toast--warning .error-icon {
  color: #f59e0b;
}

.error-toast--success .error-icon {
  color: #10b981;
}

.error-message {
  flex: 1;
  min-width: 0;
}

.error-title {
  font-weight: 600;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.error-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

.close-btn {
  flex-shrink: 0;
  padding: 4px;
}

.error-action {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background: #f9fafb;
  border-top: 1px solid #eee;
}

/* 动画 */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>
