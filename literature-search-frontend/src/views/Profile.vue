<template>
  <div class="profile-container">
    <!-- 顶部栏 -->
    <div class="top-bar">
      <div class="user-info">
        <span>{{ userInfo }}</span>
        <el-button text @click="goToHome">返回首页</el-button>
        <el-button text v-if="isAdmin" @click="goToAdmin">用户管理</el-button>
        <el-button text @click="logout">退出登录</el-button>
      </div>
    </div>

    <div class="header">
      <h1>👤 个人资料</h1>
      <p>管理您的个人信息</p>
    </div>

    <div class="card">
      <el-tabs v-model="activeTab" @tab-click="onTabChange">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="info">
          <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="100px">
            <el-form-item label="头像">
              <div class="avatar-section">
                <el-avatar :size="80" :src="avatarPreview || profileForm.avatar" class="avatar-preview">
                  {{ (profileForm.nickname || profileForm.username || '?')[0].toUpperCase() }}
                </el-avatar>
                <el-upload
                  :before-upload="handleAvatarUpload"
                  :show-file-list="false"
                  accept="image/jpeg,image/png,image/gif,image/webp"
                >
                  <el-button size="small" style="margin-left: 16px">更换头像</el-button>
                </el-upload>
              </div>
            </el-form-item>

            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>

            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>

            <el-form-item label="角色">
              <el-tag :type="getRoleType(profileForm.role)">{{ getRoleName(profileForm.role) }}</el-tag>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="updateProfile" :loading="updateLoading">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" />
            </el-form-item>

            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="changePassword" :loading="passwordLoading">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 我的贡献（仅文献经略专员可见） -->
        <el-tab-pane label="我的贡献" name="contributions" v-if="userRole === 1">
          <div v-loading="contributionsLoading">
            <el-table :data="contributionsList" border stripe>
              <el-table-column prop="title" label="文献标题" width="300" />
              <el-table-column prop="authors" label="作者" width="150" />
              <el-table-column prop="journal" label="期刊" width="150" />
              <el-table-column prop="status" label="审核状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)">
                    {{ getStatusName(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="{ row }">
                  <el-button size="small" @click="viewDetail(row.id)">查看</el-button>
                  <el-button
                      v-if="row.status === 2"
                      size="small"
                      type="primary"
                      @click="resubmitLiterature(row)"
                  >
                    重新提交
                  </el-button>
                  <el-button
                      v-if="row.status === 2"
                      size="small"
                      type="danger"
                      @click="deleteContribution(row)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div v-if="contributionsList.length === 0" class="empty">
              暂无贡献记录
            </div>
          </div>
        </el-tab-pane>

        <!-- 收藏文献 -->
        <el-tab-pane label="我的收藏" name="favorites">
          <div v-loading="favoritesLoading">
            <div v-for="item in favoritesList" :key="item.id" class="favorite-item">
              <div class="favorite-title" @click="viewDetail(item.id)">
                {{ item.title }}
              </div>
              <div class="favorite-info">
                <div class="info-row">
                  <span class="info-label">作者：</span>
                  <span class="info-value">{{ item.authors || item.author }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">年份：</span>
                  <span class="info-value">{{ item.publishYear || item.year }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">期刊：</span>
                  <span class="info-value">{{ item.journal }}</span>
                </div>
              </div>
              <div class="favorite-actions">
                <el-button size="small" type="danger" @click="removeFavorite(item.id)">取消收藏</el-button>
              </div>
            </div>

            <div v-if="favoritesList.length === 0" class="empty">
              暂无收藏文献
            </div>
          </div>
        </el-tab-pane>

        <!-- 检索历史 -->
        <el-tab-pane label="检索历史" name="history">
          <div v-loading="historyLoading">
            <div v-for="item in historyList" :key="item.id" class="history-item">
              <div class="history-keyword" @click="searchAgain(item.keyword)">
                🔍 {{ item.keyword }}
              </div>
              <div class="history-time">{{ item.searchTime }}</div>
            </div>

            <div v-if="historyList.length === 0" class="empty">
              暂无检索历史
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 详情模态框 -->
    <el-dialog v-model="detailVisible" title="文献详情" width="700px">
      <div v-if="currentDetail">
        <p><strong>标题：</strong> {{ currentDetail.title }}</p>
        <p><strong>作者：</strong> {{ currentDetail.authors || currentDetail.author }}</p>
        <p><strong>期刊：</strong> {{ currentDetail.journal }}</p>
        <p><strong>年份：</strong> {{ currentDetail.publishYear || currentDetail.year }}</p>
        <p><strong>关键词：</strong> {{ currentDetail.keywords }}</p>
        <p><strong>摘要：</strong></p>
        <p class="abstract-text">{{ currentDetail.abstractText }}</p>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userAPI, literatureAPI } from '@/api'

const router = useRouter()

// 用户信息
const userRole = ref(0)
const userInfo = ref('')
const isAdmin = computed(() => userRole.value === 2)
const activeTab = ref('info')

// 基本信息表单
const profileFormRef = ref(null)
const profileForm = ref({
  username: '',
  nickname: '',
  email: '',
  role: 0,
  avatar: ''
})
const updateLoading = ref(false)
const avatarPreview = ref('')
const selectedAvatarFile = ref(null)

const handleAvatarUpload = (file) => {
  avatarPreview.value = URL.createObjectURL(file)
  selectedAvatarFile.value = file
  return false
}

const profileRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 20, message: '昵称长度在 1-20 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

// 修改密码表单
const passwordFormRef = ref(null)
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordLoading = ref(false)

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.value.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 重新提交文献（跳转到首页并预填信息）
const resubmitLiterature = (item) => {
  // 保存要重新提交的文献信息到 localStorage
  localStorage.setItem('resubmitLiterature', JSON.stringify({
    id: item.id,
    title: item.title,
    authors: item.authors,
    journal: item.journal,
    year: item.publishYear,
    keywords: item.keywords,
    abstractText: item.abstractText,
    doi: item.doi,
    category: item.category
  }))
  router.push('/')
  ElMessage.info('请在首页修改后重新提交')
}

// 删除贡献（删除自己上传的被驳回的文献）
const deleteContribution = async (item) => {
  try {
    await ElMessageBox.confirm(`确定要永久删除文献 "${item.title}" 吗？此操作不可恢复！`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })

    const res = await literatureAPI.delete(item.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadContributions() // 刷新列表
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}


const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 3, max: 20, message: '密码长度在 3-20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 我的贡献
const contributionsList = ref([])
const contributionsLoading = ref(false)

// 收藏文献
const favoritesList = ref([])
const favoritesLoading = ref(false)

// 检索历史
const historyList = ref([])
const historyLoading = ref(false)

// 详情模态框
const detailVisible = ref(false)
const currentDetail = ref(null)

// 获取角色名称
const getRoleName = (role) => {
  if (role === 0) return '普通用户'
  if (role === 1) return '文献经略专员'
  if (role === 2) return '管理员'
  return '未知'
}

const getRoleType = (role) => {
  if (role === 0) return 'info'
  if (role === 1) return 'warning'
  if (role === 2) return 'danger'
  return ''
}

const getStatusName = (status) => {
  if (status === 0) return '待审核'
  if (status === 1) return '已通过'
  if (status === 2) return '已驳回'
  return '未知'
}

const getStatusType = (status) => {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return ''
}

// 加载用户信息
const loadUserInfo = async () => {
  const userStr = localStorage.getItem('user')

  // ✅ 过滤所有无效值
  if (!userStr ||
      userStr === 'undefined' ||
      userStr === 'null') {
    // 从后端重新获取
    await fetchCurrentUser()
    return
  }

  try {
    const user = JSON.parse(userStr)
    userRole.value = Number(user.role)  // ✅ 强制转数字
    profileForm.value = {
      username: user.username || '',
      nickname: user.nickname || '',
      email: user.email || '',
      role: Number(user.role),  // ✅ 强制转数字
      avatar: user.avatar || ''
    }
    const roleName = getRoleName(userRole.value)
    userInfo.value = `${user.nickname || user.username} (${roleName})`
  } catch (e) {
    console.error('解析用户信息失败:', e)
    localStorage.removeItem('user')
    await fetchCurrentUser()
  }
}

// ✅ 新增 fetchCurrentUser（从后端获取）
const fetchCurrentUser = async () => {
  try {
    const res = await userAPI.getCurrent()
    if (res.code === 200 && res.data) {
      const user = res.data
      userRole.value = Number(user.role)
      profileForm.value = {
        username: user.username || '',
        nickname: user.nickname || '',
        email: user.email || '',
        role: Number(user.role),
        avatar: user.avatar || ''
      }
      const roleName = getRoleName(userRole.value)
      userInfo.value = `${user.nickname || user.username} (${roleName})`
      // ✅ 同步更新 localStorage
      localStorage.setItem('user', JSON.stringify({
        ...user,
        role: Number(user.role)
      }))
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    router.push('/login')
  }
}

// ✅ 修复 onMounted：加 await
onMounted(async () => {
  await loadUserInfo()  // ✅ 等待用户信息加载完成

  // ✅ 现在 userRole 已经有正确的值了
  loadFavorites()
  loadHistory()

  if (userRole.value === 1) {  // ✅ 现在能正确判断了
    loadContributions()
  }
})

// 更新个人资料
const updateProfile = async () => {
  if (!profileFormRef.value) return

  await profileFormRef.value.validate(async (valid) => {
    if (!valid) return

    updateLoading.value = true
    try {
      const res = await userAPI.updateProfile({
        nickname: profileForm.value.nickname,
        email: profileForm.value.email,
        avatar: selectedAvatarFile.value
      })
      if (res.code === 200) {
        const userStr = localStorage.getItem('user')
        if (userStr) {
          const user = JSON.parse(userStr)
          user.nickname = profileForm.value.nickname
          user.email = profileForm.value.email
          if (res.data?.avatar) user.avatar = res.data.avatar
          localStorage.setItem('user', JSON.stringify(user))
        }
        selectedAvatarFile.value = null
        avatarPreview.value = ''
        ElMessage.success('资料更新成功')
        loadUserInfo()
      } else {
        ElMessage.error(res.message || '更新失败')
      }
    } catch (error) {
      ElMessage.error('更新失败')
    } finally {
      updateLoading.value = false
    }
  })
}

// 修改密码
const changePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return

    passwordLoading.value = true
    try {
      const res = await userAPI.changePassword({
        oldPassword: passwordForm.value.oldPassword,
        newPassword: passwordForm.value.newPassword
      })
      if (res.code === 200) {
        ElMessage.success('密码修改成功，请重新登录')
        setTimeout(() => {
          logout()
        }, 1500)
      } else {
        ElMessage.error(res.message || '修改失败')
      }
    } catch (error) {
      ElMessage.error('修改失败')
    } finally {
      passwordLoading.value = false
    }
  })
}

// 加载我的贡献
const loadContributions = async () => {
  contributionsLoading.value = true
  try {
    // 调用获取贡献列表的API
    const res = await literatureAPI.getMyContributions()
    if (res.code === 200) {
      contributionsList.value = res.data || []
    }
  } catch (error) {
    console.error('加载贡献列表失败', error)
  } finally {
    contributionsLoading.value = false
  }
}

// 加载收藏列表
const loadFavorites = async () => {
  favoritesLoading.value = true
  try {
    const res = await literatureAPI.getFavorites()
    if (res.code === 200) {
      favoritesList.value = res.data || []
    }
  } catch (error) {
    console.error('加载收藏列表失败', error)
    favoritesList.value = []
  } finally {
    favoritesLoading.value = false
  }
}



// 取消收藏
const removeFavorite = async (id) => {
  try {
    const res = await literatureAPI.removeFavorite(id)
    if (res.code === 200) {
      ElMessage.success('已取消收藏')
      loadFavorites()
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 加载检索历史
const loadHistory = async () => {
  historyLoading.value = true
  try {
    const res = await literatureAPI.getSearchHistory()
    if (res.code === 200) {
      historyList.value = res.data || []
    }
  } catch (error) {
    console.error('加载检索历史失败', error)
    historyList.value = []
  } finally {
    historyLoading.value = false
  }
}

// 再次搜索
const searchAgain = (keyword) => {
  router.push({
    path: '/',
    query: {
      keyword: keyword
    }
  })
}

// 查看详情
const viewDetail = async (id) => {
  try {
    const res = await literatureAPI.getDetail(id)
    if (res.code === 200) {
      currentDetail.value = res.data
      detailVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

// 返回首页
const goToHome = () => {
  router.push('/')
}

// 用户管理
const goToAdmin = () => {
  router.push('/admin')
}

// 退出登录
const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}

// 标签页切换时加载对应数据
const onTabChange = (tab) => {
  if (tab.props.name === 'contributions') {
    loadContributions()
  } else if (tab.props.name === 'favorites') {
    loadFavorites()
  } else if (tab.props.name === 'history') {
    loadHistory()
  }
}
</script>

<style scoped lang="scss">
.profile-container {
  max-width: 1200px;
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

.favorite-item {
  padding: 20px;
  border-bottom: 1px solid #eee;
  transition: all 0.3s;

  &:hover {
    background: #f8f9fa;
  }
}

.favorite-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  cursor: pointer;

  &:hover {
    color: #667eea;
  }
}

.favorite-info {
  margin-bottom: 12px;
}

.info-row {
  margin-bottom: 6px;
  font-size: 0.9rem;
}

.info-label {
  color: #999;
  width: 45px;
  display: inline-block;
}

.info-value {
  color: #555;
}

.favorite-actions {
  text-align: right;
  padding-top: 8px;
}

.history-item {
  padding: 15px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;

  &:hover {
    background: #f8f9fa;
  }
}

.history-keyword {
  font-size: 1rem;
  color: #1a73e8;

  &:hover {
    text-decoration: underline;
  }
}

.history-time {
  font-size: 0.8rem;
  color: #999;
}

.empty {
  text-align: center;
  padding: 40px;
  color: #999;
}

.avatar-section {
  display: flex;
  align-items: center;
}

.avatar-preview {
  flex-shrink: 0;
  font-size: 1.5rem;
  background: #667eea;
  color: white;
}

.abstract-text {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 8px;
  line-height: 1.6;
}

:deep(.el-tabs__header) {
  margin-bottom: 30px;
}
</style>