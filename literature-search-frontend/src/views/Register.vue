<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h1>📝 用户注册</h1>
        <p>加入智能文献检索系统</p>
      </div>

      <el-form :model="registerForm" :rules="rules" ref="registerFormRef">
        <el-form-item prop="username">
          <el-input
              v-model="registerForm.username"
              placeholder="用户名"
              prefix-icon="User"
              size="large"
          />
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
              v-model="registerForm.nickname"
              placeholder="昵称"
              prefix-icon="UserFilled"
              size="large"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
              v-model="registerForm.email"
              placeholder="邮箱"
              prefix-icon="Message"
              size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="密码"
              prefix-icon="Lock"
              size="large"
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="确认密码"
              prefix-icon="Lock"
              size="large"
          />
        </el-form-item>

        <el-form-item>
          <el-button
              type="primary"
              size="large"
              :loading="loading"
              @click="handleRegister"
              class="register-btn"
          >
            注册
          </el-button>
        </el-form-item>

        <div class="register-footer">
          <span>已有账号？</span>
          <el-link type="primary" @click="goToLogin">立即登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

// 验证确认密码
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2-20 个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 20, message: '昵称长度在 1-20 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 3, max: 20, message: '密码长度在 3-20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const response = await axios.post('/api/user/register', {
        username: registerForm.username,
        nickname: registerForm.nickname,
        email: registerForm.email,
        password: registerForm.password
      })

      if (response.data.code === 200) {
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } else {
        ElMessage.error(response.data.message || '注册失败')
      }
    } catch (error) {
      console.error('注册失败:', error)
      ElMessage.error('注册失败，请重试')
    } finally {
      loading.value = false
    }
  })
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped lang="scss">
.register-container {
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

.register-card {
  position: relative;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 24px;
  padding: 40px;
  width: 500px;
  max-width: 90%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.register-header {
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

.register-btn {
  width: 100%;
  background: linear-gradient(135deg, #2e7d32 0%, #1b5e20 100%);
  border: none;
  height: 44px;
  font-size: 1rem;
}

.register-footer {
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