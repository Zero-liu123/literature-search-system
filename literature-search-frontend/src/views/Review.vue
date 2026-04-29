<template>
  <div class="review-container">
    <!-- 顶部栏 -->
    <div class="top-bar">
      <div class="user-info">
        <span>{{ userInfo }}</span>
        <el-button text @click="goToHome">返回首页</el-button>
        <el-button text @click="logout">退出登录</el-button>
      </div>
    </div>

    <div class="header">
      <h1>📋 文献审核</h1>
      <p>审核文献经略专员提交的文献</p>
    </div>

    <div class="card">
      <el-tabs v-model="activeTab" @tab-change="loadPendingList">
        <el-tab-pane label="待审核" name="pending">
          <div v-loading="loading">
            <el-table :data="pendingList" border stripe>
              <el-table-column prop="title" label="标题" width="300" show-overflow-tooltip />
              <el-table-column prop="authors" label="作者" width="150" />
              <el-table-column prop="journal" label="期刊" width="150" />
              <el-table-column prop="contributorName" label="提交人" width="120" />
              <el-table-column prop="createTime" label="提交时间" width="160" />
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" @click="viewDetail(row.id)">查看详情</el-button>
                  <el-button size="small" type="success" @click="approve(row)">通过</el-button>
                  <el-button size="small" type="danger" @click="reject(row)">驳回</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div v-if="pendingList.length === 0 && !loading" class="empty">
              暂无待审核文献
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="已审核" name="reviewed">
          <div v-loading="reviewedLoading">
            <el-table :data="reviewedList" border stripe>
              <el-table-column prop="title" label="标题" width="300" show-overflow-tooltip />
              <el-table-column prop="authors" label="作者" width="150" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                    {{ row.status === 1 ? '已通过' : '已驳回' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="reviewRemark" label="驳回理由" width="200" show-overflow-tooltip />
              <el-table-column prop="reviewTime" label="审核时间" width="160" />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button size="small" @click="viewDetail(row.id)">查看详情</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div v-if="reviewedList.length === 0 && !reviewedLoading" class="empty">
              暂无已审核文献
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 驳回对话框 -->
    <el-dialog v-model="rejectVisible" title="驳回理由" width="500px">
      <el-input
          v-model="rejectRemark"
          type="textarea"
          :rows="4"
          placeholder="请输入驳回理由..."
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确定驳回</el-button>
      </template>
    </el-dialog>

    <!-- 详情模态框 -->
    <el-dialog v-model="detailVisible" title="文献详情" width="700px">
      <div v-if="currentDetail">
        <p><strong>标题：</strong> {{ currentDetail.title }}</p>
        <p><strong>作者：</strong> {{ currentDetail.authors || currentDetail.author }}</p>
        <p><strong>期刊：</strong> {{ currentDetail.journal }}</p>
        <p><strong>年份：</strong> {{ currentDetail.publishYear || currentDetail.year }}</p>
        <p><strong>关键词：</strong> {{ currentDetail.keywords }}</p>
        <p><strong>DOI：</strong> {{ currentDetail.doi || '无' }}</p>
        <p><strong>分类：</strong> {{ currentDetail.category }}</p>
        <p><strong>提交人：</strong> {{ currentDetail.contributorName }}</p>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const router = useRouter()

// 用户信息
const userInfo = ref('')
const activeTab = ref('pending')

// 待审核列表
const pendingList = ref([])
const loading = ref(false)

// 已审核列表
const reviewedList = ref([])
const reviewedLoading = ref(false)

// 驳回相关
const rejectVisible = ref(false)
const rejectRemark = ref('')
const currentLiterature = ref(null)

// 详情相关
const detailVisible = ref(false)
const currentDetail = ref(null)

// 加载待审核列表
const loadPendingList = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const res = await axios.get('/api/literature/pending', {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (res.data.code === 200) {
      pendingList.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载待审核列表失败', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 加载已审核列表
const loadReviewedList = async () => {
  reviewedLoading.value = true
  try {
    const token = localStorage.getItem('token')
    const res = await axios.get('/api/literature/reviewed', {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (res.data.code === 200) {
      reviewedList.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载已审核列表失败', error)
  } finally {
    reviewedLoading.value = false
  }
}

// 通过审核
const approve = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要通过文献 "${row.title}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })

    const token = localStorage.getItem('token')
    const res = await axios.put(`/api/literature/review/${row.id}?status=1`, {}, {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (res.data.code === 200) {
      ElMessage.success('审核通过')
      loadPendingList()
      loadReviewedList()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 驳回（打开对话框）
const reject = (row) => {
  currentLiterature.value = row
  rejectRemark.value = ''
  rejectVisible.value = true
}

// 确认驳回
const confirmReject = async () => {
  if (!rejectRemark.value.trim()) {
    ElMessage.warning('请填写驳回理由')
    return
  }

  try {
    const token = localStorage.getItem('token')
    const res = await axios.put(`/api/literature/review/${currentLiterature.value.id}?status=2&remark=${encodeURIComponent(rejectRemark.value)}`, {}, {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (res.data.code === 200) {
      ElMessage.success('已驳回')
      rejectVisible.value = false
      loadPendingList()
      loadReviewedList()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 查看详情
const viewDetail = async (id) => {
  try {
    const token = localStorage.getItem('token')
    const res = await axios.get(`/api/literature/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (res.data.code === 200) {
      currentDetail.value = res.data.data
      detailVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

// 加载用户信息
const loadUserInfo = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
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
  loadPendingList()
  loadReviewedList()
})
</script>

<style scoped lang="scss">
.review-container {
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

.empty {
  text-align: center;
  padding: 40px;
  color: #999;
}

.abstract-text {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 8px;
  line-height: 1.6;
}
</style>