<template>
  <div class="not-found-page h-full flex items-center justify-center bg-gray-100">
    <div class="text-center">
      <div class="text-9xl font-bold text-gray-300 mb-4">404</div>
      <div class="text-2xl font-semibold text-gray-700 mb-4">页面未找到</div>
      <div class="text-gray-500 mb-8">
        抱歉，您访问的页面不存在或已被删除
      </div>
      <div class="space-x-4">
        <el-button type="primary" size="large" @click="$router.push('/')">
          返回首页
        </el-button>
        <el-button size="large" @click="$router.back()">
          返回上一页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'

onMounted(() => {
  console.log('404 page mounted')
})
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
}
</style>
