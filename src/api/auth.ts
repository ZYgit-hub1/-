import { request } from '@/utils/request'

// 用户信息
export interface UserInfo {
  id: string
  username: string
  nickname?: string
  email?: string
  phone?: string
  roles: string[]
  avatar?: string
  plantIds: string[]
}

// 登录参数
export interface LoginParams {
  username: string
  password: string
  captcha?: string
  captchaId?: string
}

// 登录响应
export interface LoginResponse {
  token: string
  refreshToken: string
  user: UserInfo
}

// 登录
export function login(data: LoginParams) {
  return request.post<LoginResponse>('/auth/login', data)
}

// 登出
export function logout() {
  return request.post<void>('/auth/logout')
}

// 获取用户信息
export function getUserInfo() {
  return request.get<UserInfo>('/auth/userinfo')
}

// 获取验证码
export function getCaptcha() {
  return request.get<{
    captchaId: string
    captchaImage: string
  }>('/auth/captcha')
}

// 刷新 Token
export function refreshToken(refreshToken: string) {
  return request.post<{
    token: string
    refreshToken: string
  }>('/auth/refresh', { refreshToken })
}

// 修改密码
export function changePassword(oldPassword: string, newPassword: string) {
  return request.post<void>('/auth/password', { oldPassword, newPassword })
}

// 更新用户信息
export function updateUserInfo(data: Partial<UserInfo>) {
  return request.put<UserInfo>('/auth/userinfo', data)
}

// 获取菜单
export function getMenus() {
  return request.get<{
    path: string
    name: string
    icon?: string
    children?: { path: string; name: string; icon?: string }[]
  }[]>('/auth/menus')
}
