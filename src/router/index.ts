import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomePage.vue'),
    meta: { 
      title: '首页',
      requiresAuth: false 
    }
  },
  {
    path: '/plant/:id',
    name: 'PlantDetail',
    component: () => import('@/views/PlantDetail.vue'),
    meta: { 
      title: '电厂详情',
      requiresAuth: false 
    }
  },
  {
    path: '/alarm',
    name: 'AlarmCenter',
    component: () => import('@/views/AlarmCenter.vue'),
    meta: { 
      title: '报警中心',
      requiresAuth: false 
    }
  },
  {
    path: '/alarm/:id',
    name: 'AlarmDetail',
    component: () => import('@/views/AlarmDetail.vue'),
    meta: { 
      title: '报警详情',
      requiresAuth: false 
    }
  },
  {
    path: '/warning',
    name: 'WarningList',
    component: () => import('@/views/WarningList.vue'),
    meta: { 
      title: '预警列表',
      requiresAuth: false 
    }
  },
  {
    path: '/stats',
    name: 'Statistics',
    component: () => import('@/views/Statistics.vue'),
    meta: { 
      title: '数据统计',
      requiresAuth: false 
    }
  },
  // 404页面
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面未找到' }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 更新页面标题
  document.title = `${to.meta.title || '页面'} - 电厂监控管理系统`
  
  // 模拟API请求检查（如果有需要）
  try {
    // 检查是否需要认证
    if (to.meta.requiresAuth) {
      const token = localStorage.getItem('token')
      if (!token) {
        ElMessage.warning('请先登录')
        next('/login')
        return
      }
    }
    
    // 继续导航
    next()
  } catch (error) {
    console.error('路由守卫错误:', error)
    ElMessage.error('路由处理失败')
    next(false)
  }
})

// 路由错误处理
router.onError((error) => {
  console.error('路由错误:', error)
  
  // 尝试恢复
  const chunk = error.message?.match(/.*?(chunk-[\w-]+)/)?.[1]
  if (chunk) {
    // 尝试重新加载chunk
    window.location.reload()
  } else {
    ElMessage.error('页面加载失败，请刷新重试')
  }
})

export default router
