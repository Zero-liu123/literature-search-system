import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'  // ⭐ 补上这一行！
import HomeView from '../views/HomeView.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Admin from '../views/Admin.vue'
import Profile from '../views/Profile.vue'
import Review from '../views/Review.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'login',
    component: Login
  },
  {
    path: '/register',
    name: 'register',
    component: Register
  },
  {
    path: '/admin',
    name: 'admin',
    component: Admin,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/profile',
    name: 'profile',
    component: Profile,
    meta: { requiresAuth: true }
  },
  {
    path: '/review',
    name: 'review',
    component: Review,
    meta: { requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// ✅ 修复后的路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userStr = localStorage.getItem('user')

  // 需要登录
  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }

  // 需要管理员权限
  if (to.meta.requiresAdmin) {
    if (!userStr) {
      // ⭐ 没有用户信息时也要处理
      next('/login')
      return
    }
    try {
      const user = JSON.parse(userStr)
      if (user.role !== 2) {
        ElMessage.error('权限不足，仅管理员可访问') // ✅ 现在有导入了
        next('/')
        return
      }
    } catch (e) {
      next('/login')
      return
    }
  }

  next()
})

export default router
