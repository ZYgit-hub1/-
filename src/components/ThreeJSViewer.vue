<template>
  <div class="three-viewer" ref="containerRef">
    <!-- Three.js Canvas -->
    <canvas ref="canvasRef" class="viewer-canvas" />

    <!-- 加载状态 -->
    <Transition name="fade">
      <div v-if="isLoading" class="loading-overlay">
        <div class="loading-content">
          <div class="loading-spinner">
            <div class="spinner-ring"></div>
            <div class="spinner-ring delay-1"></div>
            <div class="spinner-ring delay-2"></div>
          </div>
          <span class="loading-text">正在加载 3D 模型...</span>
          <div class="loading-progress">
            <div class="progress-bar" :style="{ width: `${loadProgress}%` }"></div>
          </div>
          <span class="progress-text">{{ loadProgress }}%</span>
        </div>
      </div>
    </Transition>

    <!-- 错误状态 -->
    <Transition name="fade">
      <div v-if="hasError && !isLoading" class="error-overlay">
        <div class="error-content">
          <div class="error-icon">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
            </svg>
          </div>
          <h3>模型加载失败</h3>
          <p>{{ errorMessage }}</p>
          <div class="error-actions">
            <button class="retry-btn" @click="retryLoad">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
                <path d="M3 3v5h5"/>
              </svg>
              重试
            </button>
            <button class="fallback-btn" @click="showFallback = !showFallback">
              {{ showFallback ? '隐藏' : '显示' }}占位符
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 占位符模型 -->
    <Transition name="fade">
      <div v-if="showFallback && !isLoading" class="fallback-overlay">
        <div class="fallback-model">
          <!-- 简化电厂示意 -->
          <svg width="200" height="200" viewBox="0 0 200 200" fill="none">
            <!-- 烟囱 -->
            <rect x="45" y="30" width="20" height="80" rx="2" fill="#6b7280"/>
            <rect x="42" y="25" width="26" height="10" rx="2" fill="#4b5563"/>
            <!-- 主厂房 -->
            <rect x="20" y="100" width="80" height="60" rx="4" fill="#3b82f6"/>
            <rect x="25" y="105" width="15" height="20" rx="2" fill="#93c5fd"/>
            <rect x="45" y="105" width="15" height="20" rx="2" fill="#93c5fd"/>
            <rect x="65" y="105" width="15" height="20" rx="2" fill="#93c5fd"/>
            <!-- 冷却塔 -->
            <ellipse cx="140" cy="120" rx="30" ry="40" fill="#1e40af"/>
            <ellipse cx="140" cy="80" rx="20" ry="8" fill="#1e3a8a"/>
            <!-- 变压器 -->
            <rect x="120" y="150" width="30" height="25" rx="2" fill="#f59e0b"/>
            <!-- 电线 -->
            <line x1="80" y1="160" x2="120" y2="160" stroke="#fbbf24" stroke-width="2"/>
            <!-- 地面 -->
            <rect x="0" y="170" width="200" height="30" fill="#d1d5db"/>
          </svg>
          <div class="fallback-label">
            <span class="model-icon">🏭</span>
            <span>电厂 3D 模型</span>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 控制提示 -->
    <div v-if="!hasError && !isLoading" class="control-hints">
      <div class="hint-item">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <path d="M12 8v4l3 3"/>
        </svg>
        <span>拖拽旋转</span>
      </div>
      <div class="hint-item">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="3"/>
          <path d="M12 2v2M12 20v2M2 12h2M20 12h2"/>
        </svg>
        <span>滚轮缩放</span>
      </div>
      <div class="hint-item">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
          <line x1="9" y1="3" x2="9" y2="21"/>
        </svg>
        <span>右键平移</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useResizeObserver } from '@vueuse/core'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'

interface Props {
  modelUrl?: string
  autoRotate?: boolean
  backgroundColor?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelUrl: '/models/power-plant.glb',
  autoRotate: false,
  backgroundColor: '#1a1a2e'
})

const emit = defineEmits<{
  (e: 'loaded'): void
  (e: 'error', message: string): void
  (e: 'click', event: MouseEvent): void
}>()

// DOM 引用
const containerRef = ref<HTMLElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)

// 状态
const isLoading = ref(true)
const hasError = ref(false)
const errorMessage = ref('')
const loadProgress = ref(0)
const showFallback = ref(false)

// Three.js 对象
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let renderer: THREE.WebGLRenderer | null = null
let controls: OrbitControls | null = null
let animationFrameId: number | null = null
let model: THREE.Object3D | null = null

// 初始化场景
function initScene() {
  if (!containerRef.value || !canvasRef.value) return

  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight

  // 创建场景
  scene = new THREE.Scene()
  scene.background = new THREE.Color(props.backgroundColor)

  // 创建相机
  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000)
  camera.position.set(10, 8, 15)
  camera.lookAt(0, 0, 0)

  // 创建渲染器
  renderer = new THREE.WebGLRenderer({
    canvas: canvasRef.value,
    antialias: true
  })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.0

  // 添加轨道控制器
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.autoRotate = props.autoRotate
  controls.autoRotateSpeed = 1.0
  controls.minDistance = 5
  controls.maxDistance = 50
  controls.maxPolarAngle = Math.PI / 2

  // 添加光源
  addLights()

  // 添加地面
  addGround()

  // 加载模型
  loadModel()
}

// 添加光源
function addLights() {
  if (!scene) return

  // 环境光
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.4)
  scene.add(ambientLight)

  // 主方向光
  const mainLight = new THREE.DirectionalLight(0xffffff, 1.0)
  mainLight.position.set(10, 20, 10)
  mainLight.castShadow = true
  mainLight.shadow.mapSize.width = 2048
  mainLight.shadow.mapSize.height = 2048
  mainLight.shadow.camera.near = 0.5
  mainLight.shadow.camera.far = 50
  mainLight.shadow.camera.left = -20
  mainLight.shadow.camera.right = 20
  mainLight.shadow.camera.top = 20
  mainLight.shadow.camera.bottom = -20
  mainLight.shadow.bias = -0.0001
  scene.add(mainLight)

  // 补光
  const fillLight = new THREE.DirectionalLight(0x4fc3f7, 0.3)
  fillLight.position.set(-10, 10, -10)
  scene.add(fillLight)

  // 背光
  const backLight = new THREE.DirectionalLight(0xffffff, 0.2)
  backLight.position.set(0, 10, -20)
  scene.add(backLight)
}

// 添加地面
function addGround() {
  if (!scene) return

  // 主地面
  const groundGeometry = new THREE.PlaneGeometry(100, 100)
  const groundMaterial = new THREE.MeshStandardMaterial({
    color: 0x2d3748,
    roughness: 0.9,
    metalness: 0.1
  })
  const ground = new THREE.Mesh(groundGeometry, groundMaterial)
  ground.rotation.x = -Math.PI / 2
  ground.position.y = -0.01
  ground.receiveShadow = true
  scene.add(ground)

  // 网格辅助线
  const gridHelper = new THREE.GridHelper(50, 50, 0x4a5568, 0x374151)
  gridHelper.position.y = 0
  scene.add(gridHelper)
}

// 创建占位符模型
function createFallbackModel() {
  if (!scene) return

  const group = new THREE.Group()

  // 主厂房
  const buildingGeometry = new THREE.BoxGeometry(4, 3, 2)
  const buildingMaterial = new THREE.MeshStandardMaterial({
    color: 0x3b82f6,
    roughness: 0.5,
    metalness: 0.3
  })
  const building = new THREE.Mesh(buildingGeometry, buildingMaterial)
  building.position.y = 1.5
  building.castShadow = true
  building.receiveShadow = true
  group.add(building)

  // 烟囱
  const chimneyGeometry = new THREE.CylinderGeometry(0.3, 0.4, 5, 16)
  const chimneyMaterial = new THREE.MeshStandardMaterial({
    color: 0x6b7280,
    roughness: 0.7
  })
  const chimney = new THREE.Mesh(chimneyGeometry, chimneyMaterial)
  chimney.position.set(-1.5, 4.5, 0)
  chimney.castShadow = true
  group.add(chimney)

  // 冷却塔
  const coolingGeometry = new THREE.CylinderGeometry(0.5, 2, 4, 32, 1, true)
  const coolingMaterial = new THREE.MeshStandardMaterial({
    color: 0x1e40af,
    roughness: 0.6,
    metalness: 0.2,
    side: THREE.DoubleSide
  })
  const cooling = new THREE.Mesh(coolingGeometry, coolingMaterial)
  cooling.position.set(2, 2, 0)
  cooling.castShadow = true
  group.add(cooling)

  // 冷却塔顶部
  const topGeometry = new THREE.RingGeometry(0.3, 0.5, 32)
  const topMesh = new THREE.Mesh(topGeometry, coolingMaterial)
  topMesh.rotation.x = -Math.PI / 2
  topMesh.position.set(2, 4, 0)
  group.add(topMesh)

  // 变压器
  const transformerGeometry = new THREE.BoxGeometry(1.5, 1.2, 0.8)
  const transformerMaterial = new THREE.MeshStandardMaterial({
    color: 0xf59e0b,
    roughness: 0.4,
    metalness: 0.5
  })
  const transformer = new THREE.Mesh(transformerGeometry, transformerMaterial)
  transformer.position.set(0, 0.6, 2)
  transformer.castShadow = true
  group.add(transformer)

  model = group
  scene.add(group)

  // 相机适应
  fitCameraToObject(group)
}

// 加载 GLTF 模型
function loadModel() {
  const loader = new GLTFLoader()

  loader.load(
    props.modelUrl,
    (gltf) => {
      model = gltf.scene
      
      // 设置模型属性
      model.traverse((child) => {
        if (child instanceof THREE.Mesh) {
          child.castShadow = true
          child.receiveShadow = true
        }
      })

      if (scene) {
        scene.add(model)
      }

      isLoading.value = false
      emit('loaded')
      
      fitCameraToObject(model)
    },
    (progress) => {
      if (progress.total > 0) {
        loadProgress.value = Math.round((progress.loaded / progress.total) * 100)
      }
    },
    (error) => {
      console.warn('模型加载失败，使用占位符:', error)
      
      // 加载失败时显示占位符
      hasError.value = true
      errorMessage.value = '无法加载 3D 模型文件，已显示示意模型'
      showFallback.value = true
      
      createFallbackModel()
      isLoading.value = false
      
      emit('error', errorMessage.value)
    }
  )
}

// 相机适应模型
function fitCameraToObject(object: THREE.Object3D) {
  if (!camera || !controls) return

  const box = new THREE.Box3().setFromObject(object)
  const center = box.getCenter(new THREE.Vector3())
  const size = box.getSize(new THREE.Vector3())

  const maxDim = Math.max(size.x, size.y, size.z)
  const fov = camera.fov * (Math.PI / 180)
  let cameraZ = Math.abs(maxDim / Math.tan(fov / 2)) * 1.5

  camera.position.set(
    center.x + cameraZ * 0.5,
    center.y + cameraZ * 0.3,
    center.z + cameraZ
  )
  camera.lookAt(center)

  controls.target.copy(center)
  controls.update()
}

// 重试加载
function retryLoad() {
  hasError.value = false
  isLoading.value = true
  loadProgress.value = 0
  
  if (model && scene) {
    scene.remove(model)
    model = null
  }
  
  loadModel()
}

// 处理点击
function handleClick(event: MouseEvent) {
  emit('click', event)
}

// 动画循环
function animate() {
  animationFrameId = requestAnimationFrame(animate)
  
  if (controls) {
    controls.update()
  }
  
  if (renderer && scene && camera) {
    renderer.render(scene, camera)
  }
}

// 处理窗口大小变化
function handleResize() {
  if (!containerRef.value || !camera || !renderer) return

  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight

  camera.aspect = width / height
  camera.updateProjectionMatrix()

  renderer.setSize(width, height)
}

// 使用 vueuse 的 resize observer
useResizeObserver(containerRef, (entries) => {
  const entry = entries[0]
  if (entry) {
    handleResize()
  }
})

// 监听 autoRotate 变化
watch(() => props.autoRotate, (value) => {
  if (controls) {
    controls.autoRotate = value
  }
})

// 清理资源
function dispose() {
  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId)
  }

  if (controls) {
    controls.dispose()
  }

  if (renderer) {
    renderer.dispose()
  }

  if (scene) {
    scene.traverse((object) => {
      if (object instanceof THREE.Mesh) {
        object.geometry.dispose()
        if (Array.isArray(object.material)) {
          object.material.forEach(m => m.dispose())
        } else {
          object.material.dispose()
        }
      }
    })
  }
}

onMounted(() => {
  initScene()
  animate()
})

onUnmounted(() => {
  dispose()
})

// 暴露方法
defineExpose({
  resetCamera: () => {
    if (model) fitCameraToObject(model)
  },
  setAutoRotate: (value: boolean) => {
    if (controls) controls.autoRotate = value
  },
  takeScreenshot: (): string | null => {
    if (!renderer) return null
    return renderer.domElement.toDataURL('image/png')
  }
})
</script>

<style scoped>
.three-viewer {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 400px;
  background: #1a1a2e;
  border-radius: 12px;
  overflow: hidden;
}

.viewer-canvas {
  display: block;
  width: 100%;
  height: 100%;
}

/* 加载状态 */
.loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(26, 26, 46, 0.95);
  z-index: 10;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.loading-spinner {
  position: relative;
  width: 60px;
  height: 60px;
}

.spinner-ring {
  position: absolute;
  inset: 0;
  border: 3px solid transparent;
  border-top-color: #0ea5e9;
  border-radius: 50%;
  animation: spin 1.2s linear infinite;
}

.spinner-ring.delay-1 {
  inset: 8px;
  border-top-color: #06b6d4;
  animation-delay: 0.2s;
}

.spinner-ring.delay-2 {
  inset: 16px;
  border-top-color: #38bdf8;
  animation-delay: 0.4s;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 14px;
  color: #94a3b8;
}

.loading-progress {
  width: 200px;
  height: 4px;
  background: #334155;
  border-radius: 2px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #0ea5e9, #06b6d4);
  border-radius: 2px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: #64748b;
}

/* 错误状态 */
.error-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(26, 26, 46, 0.9);
  z-index: 10;
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 32px;
  max-width: 320px;
}

.error-icon {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(239, 68, 68, 0.1);
  border-radius: 50%;
  color: #ef4444;
  margin-bottom: 16px;
}

.error-content h3 {
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
  margin: 0 0 8px 0;
}

.error-content p {
  font-size: 14px;
  color: #94a3b8;
  margin: 0 0 20px 0;
}

.error-actions {
  display: flex;
  gap: 12px;
}

.retry-btn,
.fallback-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.retry-btn {
  background: #0ea5e9;
  border: none;
  color: white;
}

.retry-btn:hover {
  background: #0284c7;
}

.fallback-btn {
  background: transparent;
  border: 1px solid #475569;
  color: #94a3b8;
}

.fallback-btn:hover {
  border-color: #64748b;
  color: #cbd5e1;
}

/* 占位符 */
.fallback-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5;
}

.fallback-model {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.fallback-model svg {
  filter: drop-shadow(0 10px 20px rgba(0, 0, 0, 0.3));
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.fallback-label {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  font-size: 14px;
  color: #94a3b8;
}

.model-icon {
  font-size: 18px;
}

/* 控制提示 */
.control-hints {
  position: absolute;
  bottom: 16px;
  left: 16px;
  display: flex;
  gap: 12px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 8px;
  backdrop-filter: blur(4px);
}

.hint-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
