<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>📚 智能文献检索系统</h1>
        <p>基于 Elasticsearch 的全文搜索</p>
      </div>

      <el-form :model="loginForm" :rules="rules" ref="loginFormRef">
        <el-form-item prop="username">
          <el-input
              v-model="loginForm.username"
              placeholder="用户名"
              prefix-icon="User"
              size="large"
              @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              prefix-icon="Lock"
              size="large"
              @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
              type="primary"
              size="large"
              :loading="loading"
              @click="handleLogin"
              class="login-btn"
          >
            登录
          </el-button>
        </el-form-item>

        <div class="login-footer">
          <span>还没有账号？</span>
          <el-link type="primary" @click="goToRegister">立即注册</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import {ref, reactive} from 'vue'
import { userAPI } from '@/api'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {User, Lock} from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const rules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {min: 2, max: 20, message: '用户名长度在 2-20 个字符', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 3, max: 20, message: '密码长度在 3-20 个字符', trigger: 'blur'}
  ]
}

const handleLogin = async () => {
  try {
    const res = await userAPI.login(loginForm)
    if (res.code === 200) {
      const userData = res.data

      // ⭐ 存储时强制 role 转为数字
      const userToStore = {
        ...userData,
        role: Number(userData.role)
      }

      localStorage.setItem('token', userData.token)
      localStorage.setItem('user', JSON.stringify(userToStore))

      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch (e) {
    ElMessage.error('登录请求失败，请检查网络')
  }
}


const goToRegister = () => {
  router.push('/register')
}
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-image: url('https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=1920&q=80');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.35);
  }
}

.login-card {
  position: relative;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 24px;
  padding: 40px;
  width: 450px;
  max-width: 90%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;

  h1 {
    font-size: 1.8rem;
    color: #2d4a2d;
    margin-bottom: 10px;
  }

  p {
    color: #666;
    font-size: 0.9rem;
  }
}

.login-btn {
  width: 100%;
  background: linear-gradient(135deg, #2e7d32 0%, #1b5e20 100%);
  border: none;
  height: 44px;
  font-size: 1rem;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  color: #666;

  span {
    margin-right: 8px;
  }
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 8px 15px;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-button--primary) {
  --el-button-bg-color: #2e7d32;
  --el-button-border-color: #2e7d32;
  --el-button-hover-bg-color: #388e3c;
  --el-button-hover-border-color: #388e3c;
  --el-button-active-bg-color: #1b5e20;
}

:deep(.el-link) {
  color: #2e7d32;
}
</style>