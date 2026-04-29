<template>
  <div class="admin-container">
    <!-- 顶部栏 -->
    <div class="top-bar">
      <div class="user-info">
        <span>{{ userInfo }}</span>
        <el-button text @click="goToHome">返回首页</el-button>
        <el-button text @click="logout">退出登录</el-button>
      </div>
    </div>

    <div class="header">
      <h1>👥 用户管理</h1>
      <p>修改用户角色（仅管理员可见）</p>
    </div>

    <div class="card">
      <el-table :data="userList" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column label="当前角色" width="120">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">{{ getRoleName(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="修改角色" width="180">
          <template #default="{ row }">
            <el-select
                v-model="row.newRole"
                :disabled="isOnlyAdmin(row)"
                size="small"
                @change="onRoleChange(row)"
            >
              <el-option label="普通用户" :value="0" />
              <el-option label="文献经略专员" :value="1" />
              <el-option label="管理员" :value="2" />
            </el-select>
            <el-tooltip v-if="isOnlyAdmin(row)" content="至少保留一名管理员，不可修改">
              <el-icon style="margin-left: 5px; color: #ff9800;"><Warning /></el-icon>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button
                type="primary"
                size="small"
                @click="updateUserRole(row)"
                :disabled="isOnlyAdmin(row) || row.newRole === undefined || row.newRole === row.role"
            >
              保存
            </el-button>
            <el-button
                v-if="row.id !== currentUserId"
                type="warning"
                size="small"
                @click="resetPassword(row)"
                :disabled="isOnlyAdmin(row)"
            >
              重置密码
            </el-button>
            <el-button
                v-if="row.id !== currentUserId"
                :type="row.status === 0 ? 'warning' : 'success'"
                size="small"
                @click="toggleUserStatus(row)"
                :disabled="isOnlyAdmin(row)"
            >
              {{ row.status === 0 ? '禁用' : '启用' }}
            </el-button>
            <el-button
                v-if="row.id !== currentUserId"
                type="danger"
                size="small"
                @click="deleteUser(row)"
                :disabled="isOnlyAdmin(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'
import { userAPI } from '@/api'

const router = useRouter()
const userList = ref([])
const loading = ref(false)
const currentUserId = ref(null)
const userInfo = ref('')

// 获取角色名称
const getRoleName = (role) => {
  if (role === 0) return '普通用户'
  if (role === 1) return '文献经略专员'
  if (role === 2) return '管理员'
  return '未知'
}

// 获取角色标签类型
const getRoleType = (role) => {
  if (role === 0) return 'info'
  if (role === 1) return 'warning'
  if (role === 2) return 'danger'
  return ''
}

// 判断是否是唯一管理员
const isOnlyAdmin = (user) => {
  const adminCount = userList.value.filter(u => u.role === 2).length
  return user.role === 2 && adminCount === 1
}

// 角色变化时记录新角色
const onRoleChange = (row) => {
  // newRole 已经通过 v-model 绑定
}

// 加载用户列表
const loadUsers = async () => {
  loading.value = true
  try {
    const res = await userAPI.getUserList()
    if (res.code === 200) {
      userList.value = res.data.map(user => ({
        ...user,
        newRole: user.role
      }))
    }
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 更新用户角色
const updateUserRole = async (row) => {
  if (row.newRole === row.role) {
    ElMessage.info('角色未改变')
    return
  }

  try {
    const res = await userAPI.updateRole(row.id, row.newRole)
    if (res.code === 200) {
      ElMessage.success('角色修改成功')
      row.role = row.newRole
      loadUsers() // 刷新列表
    } else {
      ElMessage.error(res.message || '修改失败')
      row.newRole = row.role // 恢复原值
    }
  } catch (error) {
    ElMessage.error('修改失败')
    row.newRole = row.role
  }
}

// 切换用户状态（禁用/启用）
const toggleUserStatus = async (row) => {
  const action = row.status === 0 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户 "${row.username}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    let res
    if (row.status === 0) {
      res = await userAPI.disableUser(row.id)
    } else {
      res = await userAPI.enableUser(row.id)
    }

    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      loadUsers()
    } else {
      ElMessage.error(res.message || `${action}失败`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

// 删除用户
const deleteUser = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要永久删除用户 "${row.username}" 吗？此操作不可恢复！`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })

    const res = await userAPI.deleteUser(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadUsers()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 重置密码
const resetPassword = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要将用户 "${row.username}" 的密码重置为 123456 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await userAPI.resetPassword(row.id)
    if (res.code === 200) {
      ElMessage.success('密码已重置为 123456')
    } else {
      ElMessage.error(res.message || '重置失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重置失败')
    }
  }
}

// 加载当前用户信息
const loadUserInfo = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    currentUserId.value = user.id
    const roleName = user.role === 0 ? '普通用户' : (user.role === 1 ? '文献经略专员' : '管理员')
    userInfo.value = `${user.nickname || user.username} (${roleName})`
  }
}

// 返回首页
const goToHome = () => {
  router.push('/')
}

// 退出登录
const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(() => {
  loadUserInfo()
  loadUsers()
})
</script>

<style scoped lang="scss">
.admin-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.top-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
  color: white;
}

.header {
  text-align: center;
  color: white;
  margin-bottom: 30px;

  h1 {
    font-size: 2rem;
    margin-bottom: 10px;
  }
}

.card {
  background: white;
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}
</style>