/**
 * MSW v2 Browser Setup -- 广东省电厂监控平台
 *
 * 在浏览器端启动 Mock Service Worker，拦截 localhost:8080 上的所有请求。
 * 使用方式:
 *   1. npx msw init public/          # 生成 Service Worker 脚本
 *   2. 在应用入口最早期 import './mock/browser'
 *   3. await worker.start({ onUnhandledRequest: 'bypass' })
 */
import { setupWorker } from 'msw/browser';
import { handlers } from './msw-handlers';

export const worker = setupWorker(...handlers);
