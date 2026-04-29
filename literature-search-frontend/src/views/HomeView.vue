<template>
  <div class="home-container">
    <!-- 顶部栏 -->
    <div class="top-bar">
      <div class="user-info">
        <span>{{ userInfo }}</span>
        <el-button text @click="goToProfile">个人资料</el-button>
        <el-button text @click="goToReview" v-if="isAdmin">文献审核</el-button>
        <el-button text @click="goToAdmin" v-if="isAdmin">用户管理</el-button>
        <el-button text @click="logout">退出登录</el-button>
      </div>
    </div>

    <!-- 头部 -->
    <div class="header">
      <h1>📚 智能文献检索系统</h1>
      <p>基于 Elasticsearch 的全文搜索 · 智能 · 高效</p>
    </div>

    <!-- 搜索卡片 -->
    <div class="card">
      <div class="search-box">
        <el-input
            v-model="keyword"
            placeholder="输入关键词搜索文献..."
            @keyup.enter="search"
            @input="handleSuggest"
            @focus="showSuggest = true"
            clearable
            size="large"
        />
        <el-button type="primary" @click="search" size="large">搜索</el-button>

        <!-- 自动补全下拉框 -->
        <ul v-if="showSuggest && suggestList.length > 0" class="suggest-box">
          <li
              v-for="(item, index) in suggestList"
              :key="index"
              @mousedown.prevent="selectSuggest(item)"
              :class="{ 'suggest-active': index === activeSuggestIndex }"
          >
            <span class="suggest-icon">🔍</span>
            <span v-html="highlightSuggest(item, keyword)"></span>
          </li>
        </ul>
      </div>

      <div class="filter-bar">
        <el-input v-model="filters.author" placeholder="作者筛选" style="width: 130px" clearable />
        <el-input v-model="filters.journal" placeholder="期刊筛选" style="width: 130px" clearable />
        <el-select v-model="filters.category" placeholder="分类筛选" style="width: 150px" clearable>
          <el-option
              v-for="cat in categoryOptions"
              :key="cat"
              :label="cat"
              :value="cat"
          />
        </el-select>
        <el-input-number v-model="filters.startYear" placeholder="起始年份" style="width: 120px" />
        <el-input-number v-model="filters.endYear" placeholder="结束年份" style="width: 120px" />
        <el-button @click="applyFilters">应用筛选</el-button>
        <el-button @click="resetFilters">重置筛选</el-button>
      </div>
    </div>

    <!-- 添加文献卡片（仅文献经略专员可见） -->
    <div class="card" v-if="userRole === 1">
      <h3>✏️ 添加新文献</h3>
      <el-form :model="newLiterature" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="标题">
              <el-input v-model="newLiterature.title" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者">
              <el-input v-model="newLiterature.authors" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期刊">
              <el-input v-model="newLiterature.journal" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发表年份">
              <el-input-number v-model="newLiterature.year" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="newLiterature.category" placeholder="请选择分类" clearable style="width: 100%">
                <el-option v-for="cat in categoryOptions" :key="cat" :label="cat" :value="cat" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="关键词">
              <el-input v-model="newLiterature.keywords" placeholder="多个关键词用分号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="摘要">
              <el-input v-model="newLiterature.abstractText" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="上传文件">
              <el-upload
                  :before-upload="handleFileUpload"
                  :show-file-list="false"
                  accept=".pdf,.doc,.docx,.txt"
              >
                <el-button>{{ fileUploaded ? '已选择文件' : '选择文件' }}</el-button>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="success" @click="addLiterature">添加文献</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 搜索结果卡片 -->
    <div class="card">
      <h3>📖 搜索结果（共 {{ total }} 篇）</h3>

      <div v-loading="loading">
        <div v-for="item in literatureList" :key="item.id" class="literature-item">
          <div
              class="literature-title"
              @click="viewDetail(item.id)"
              v-html="item.title">
          </div>


          <div class="literature-meta">
            <el-tag size="small">作者：{{ item.authors || item.author }}</el-tag>
            <el-tag size="small">年份：{{ item.publishYear || item.year }}</el-tag>
            <el-tag size="small">期刊：{{ item.journal }}</el-tag>
            <el-tag size="small">浏览量：{{ item.viewCount || 0 }}</el-tag>
          </div>
          <div
              class="literature-abstract"
              v-if="item.abstractText"
              v-html="item.abstractText">
          </div>
          <div class="item-actions">
            <el-button size="small" @click="viewDetail(item.id)">查看详情</el-button>
            <el-button
                size="small"
                :type="item.isFavorited ? 'warning' : 'default'"
                @click="toggleFavorite(item.id, item)"
            >
              {{ item.isFavorited ? '⭐ 已收藏' : '☆ 收藏' }}
            </el-button>
            <!-- 文献经略专员可以编辑自己上传的文献 -->
            <el-button
                v-if="(userRole === 1 && item.contributorId === currentUserId) || isAdmin"
                size="small"
                type="primary"
                @click="openEditDialog(item)"
            >
              ✏️ 编辑
            </el-button>
            <el-button size="small" type="danger" @click="deleteLiterature(item.id)" v-if="isAdmin">删除</el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && literatureList.length === 0" class="empty">
        暂无文献，请输入关键词搜索
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            @current-change="handlePageChange"
            layout="prev, pager, next"
        />
      </div>
    </div>

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
        <p><strong>浏览量：</strong> {{ currentDetail.viewCount || 0 }}</p>
        <p><strong>摘要：</strong></p>
        <p class="abstract-text">{{ currentDetail.abstractText }}</p>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑文献模态框（仅管理员） -->
    <el-dialog v-model="editVisible" title="编辑文献" width="700px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="editForm.authors" />
        </el-form-item>
        <el-form-item label="期刊">
          <el-input v-model="editForm.journal" />
        </el-form-item>
        <el-form-item label="发表年份">
          <el-input-number v-model="editForm.year" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="editForm.keywords" placeholder="多个关键词用分号分隔" />
        </el-form-item>
        <el-form-item label="DOI">
          <el-input v-model="editForm.doi" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editForm.category" placeholder="请选择分类" clearable>
            <el-option v-for="cat in categoryOptions" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="editForm.abstractText" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="updateLiterature" :loading="updateLoading">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>






<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { watch } from 'vue'
import { ElMessage } from 'element-plus'
import { literatureAPI, userAPI } from '@/api'
import axios from 'axios'

const router = useRouter()
const route = useRoute()


// 分类选项常量
const categoryOptions = [
  '计算机科学', '物理学', '化学', '生物学', '数学', '医学',
  '经济学', '管理学', '法学', '教育学', '文学', '历史学',
  '哲学', '艺术学', '心理学', '社会学', '政治学', '地理学',
  '环境科学', '其他'
]

// 用户信息
const userRole = ref(0)
const currentUserId = ref(null)
const userInfo = ref('')

// 搜索相关
const keyword = ref('')
const suggestList = ref([])
const showSuggest = ref(false)
const activeSuggestIndex = ref(-1)
let timer = null

const filters = ref({
  author: '',
  journal: '',
  category: '',
  startYear: null,
  endYear: null
})
const literatureList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const goToProfile = () => {
  router.push('/profile')
}

const goToReview = () => {
  router.push('/review')
}

const goToAdmin = () => {
  router.push('/admin')
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}



// 添加文献
const newLiterature = ref({
  title: '',
  authors: '',
  journal: '',
  year: null,
  keywords: '',
  abstractText: '',
  doi: '',
  category: ''
})
const selectedFile = ref(null)
const fileUploaded = ref(false)

// 详情模态框
const detailVisible = ref(false)
const currentDetail = ref(null)

// 编辑相关
const editVisible = ref(false)
const updateLoading = ref(false)
const editForm = ref({
  id: null,
  title: '',
  authors: '',
  journal: '',
  year: null,
  keywords: '',
  doi: '',
  category: '',
  abstractText: ''
})


// 搜索方法
const search = async () => {
  suggestList.value = []
  showSuggest.value = false
  const hasKeyword = keyword.value && keyword.value.trim()
  const hasFilters = filters.value.author ||
      filters.value.journal ||
      filters.value.category ||
      filters.value.startYear ||
      filters.value.endYear

  if (!hasKeyword && !hasFilters) {
    literatureList.value = []
    total.value = 0
    return
  }

  loading.value = true
  try {
    const params = {
      keyword: keyword.value || null,
      page: currentPage.value,
      size: pageSize.value
    }
    if (filters.value.author) params.author = filters.value.author
    if (filters.value.journal) params.journal = filters.value.journal
    if (filters.value.category) params.category = filters.value.category
    if (filters.value.startYear) params.startYear = filters.value.startYear
    if (filters.value.endYear) params.endYear = filters.value.endYear

    const res = await literatureAPI.search(params)
    if (res.code === 200) {
      const data = res.data
      let list = data.list || data.records || []
      total.value = data.total || list.length

      // 获取收藏状态
      const token = localStorage.getItem('token')
      if (token && list.length > 0) {
        try {
          const favRes = await literatureAPI.getFavorites()
          if (favRes.code === 200) {
            const favoriteIds = new Set(favRes.data.map(item => item.id))
            list = list.map(item => ({
              ...item,
              isFavorited: favoriteIds.has(item.id)
            }))
          }
        } catch (e) {
          console.log('获取收藏状态失败', e)
        }
      }

      literatureList.value = list

      // 保存搜索条件到 localStorage
      localStorage.setItem('lastKeyword', keyword.value)
      localStorage.setItem('lastFilters', JSON.stringify({
        author: filters.value.author,
        journal: filters.value.journal,
        category: filters.value.category,
        startYear: filters.value.startYear,
        endYear: filters.value.endYear
      }))
      localStorage.setItem('lastPage', currentPage.value)
    }

    // 保存搜索历史
    if (keyword.value && keyword.value.trim()) {
      await literatureAPI.saveSearchHistory(keyword.value.trim()).catch(e => console.log('保存历史失败', e))
    }
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  currentPage.value = 1
  search()
}

 const resetFilters= () => {
  keyword.value = ''
  filters.value = {
    author: '',
    journal: '',
    category: '',
    startYear: null,
    endYear: null
  }
  currentPage.value = 1
  literatureList.value = []
  total.value = 0

   // 清除保存的搜索条件
   localStorage.removeItem('lastKeyword')
   localStorage.removeItem('lastFilters')
   localStorage.removeItem('lastPage')
}

const handlePageChange = (page) => {
  currentPage.value = page
  search()
}

const handleFileUpload = (file) => {
  // 保存文件引用
  selectedFile.value = file

  // 如果是 PDF 文件，尝试解析
  if (file.type === 'application/pdf') {
    fileUploaded.value = true
    parsePdfFile(file)
  } else {
    // 非 PDF 文件，只填充文件名作为标题
    fileUploaded.value = true
    const nameWithoutExt = file.name.replace(/\.(pdf|doc|docx|txt)$/i, '')
    newLiterature.value.title = nameWithoutExt
    ElMessage.info('非PDF文件，已自动填充标题')
  }
  return false
}

// PDF 解析函数
const parsePdfFile = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await axios.post('/api/literature/parse-pdf', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data.code === 200) {
      const data = res.data.data
      if (data.title) newLiterature.value.title = data.title
      if (data.author) newLiterature.value.authors = data.author
      if (data.year) newLiterature.value.year = parseInt(data.year)
      if (data.keywords) newLiterature.value.keywords = data.keywords
      if (data.doi) newLiterature.value.doi = data.doi
      if (data.journal) newLiterature.value.journal = data.journal
      if (data.abstractText) newLiterature.value.abstractText = data.abstractText
      ElMessage.success('PDF解析成功，请确认信息')
    } else {
      // 解析失败，只填充文件名
      const nameWithoutExt = file.name.replace(/\.pdf$/i, '')
      newLiterature.value.title = nameWithoutExt
      ElMessage.warning('PDF解析失败，已自动填充标题，请手动填写其他信息')
    }
  } catch (error) {
    console.error('PDF解析失败', error)
    const nameWithoutExt = file.name.replace(/\.pdf$/i, '')
    newLiterature.value.title = nameWithoutExt
    ElMessage.warning('PDF解析失败，已自动填充标题')
  }
}

const addLiterature = async () => {
  if (!newLiterature.value.title || !newLiterature.value.authors) {
    ElMessage.warning('请填写标题和作者')
    return
  }

  const formData = new FormData()
  const literatureData = {
    title: newLiterature.value.title,
    authors: newLiterature.value.authors,
    journal: newLiterature.value.journal,
    publishYear: newLiterature.value.year,
    keywords: newLiterature.value.keywords,
    abstractText: newLiterature.value.abstractText,
    doi: newLiterature.value.doi,
    category: newLiterature.value.category
  }
  formData.append('literature', new Blob([JSON.stringify(literatureData)], { type: 'application/json' }))
  if (selectedFile.value) {
    formData.append('file', selectedFile.value)
  }

  try {
    const res = await literatureAPI.add(formData)
    if (res.code === 200) {
      ElMessage.success('添加成功')
      newLiterature.value = {
        title: '', authors: '', journal: '', year: null,
        keywords: '', abstractText: '', doi: '', category: ''
      }
      selectedFile.value = null
      fileUploaded.value = false
      search()
    } else {
      ElMessage.error(res.message || '添加失败')
    }
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

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

const deleteLiterature = async (id) => {
  try {
    await literatureAPI.delete(id)
    ElMessage.success('删除成功')
    search()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 切换收藏状态
const toggleFavorite = async (id, item) => {
  try {
    if (item.isFavorited) {
      await literatureAPI.removeFavorite(id)
      item.isFavorited = false
      ElMessage.success('已取消收藏')
    } else {
      await literatureAPI.addFavorite(id)
      item.isFavorited = true
      ElMessage.success('收藏成功')
    }
    // 不需要重新搜索，只更新当前项的状态
  } catch (error) {
    console.error('收藏操作失败', error)
    ElMessage.error('操作失败')
  }
}

// 打开编辑对话框
const openEditDialog = (item) => {
  editForm.value = {
    id: item.id,
    title: item.title || '',
    authors: item.authors || item.author || '',
    journal: item.journal || '',
    year: item.publishYear || item.year || null,
    keywords: item.keywords || '',
    doi: item.doi || '',
    category: item.category || '',
    abstractText: item.abstractText || ''
  }
  editVisible.value = true
}

// 更新文献
const updateLiterature = async () => {
  updateLoading.value = true
  try {
    const res = await literatureAPI.update(editForm.value.id, {
      title: editForm.value.title,
      authors: editForm.value.authors,
      journal: editForm.value.journal,
      publishYear: editForm.value.year,
      keywords: editForm.value.keywords,
      doi: editForm.value.doi,
      category: editForm.value.category,
      abstractText: editForm.value.abstractText,
      status: 0  // 编辑后状态变为待审核
    })
    if (res.code === 200) {
      ElMessage.success('修改成功，等待管理员审核')
      editVisible.value = false
      search()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (error) {
    ElMessage.error('更新失败')
  } finally {
    updateLoading.value = false
  }
}
const loadUserInfo = () => {
  const userStr = localStorage.getItem('user')
  if (userStr && userStr !== 'undefined' && userStr !== 'null') {
    try {
      const user = JSON.parse(userStr)
      // ⭐ 关键：强制转为数字！
      userRole.value = Number(user.role)
      currentUserId.value = user.id
      const roleMap = { 0: '普通用户', 1: '审核员', 2: '管理员' }
      userInfo.value = `${user.nickname || user.username} (${roleMap[userRole.value] || '未知'})`
    } catch (e) {
      console.error('解析用户信息失败:', e)
      localStorage.removeItem('user')
      router.push('/login')
    }
  }
}

// ⭐ isAdmin 的判断也要确保用数字比较
const isAdmin = computed(() => userRole.value === 2)

const fetchCurrentUser = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) return

    const res = await userAPI.getCurrent()
    if (res.code === 200 && res.data) {
      const user = res.data
      userRole.value = user.role
      currentUserId.value = user.id  // 添加这行
      const roleName = user.role === 0 ? '普通用户' : (user.role === 1 ? '文献经略专员' : '管理员')
      userInfo.value = `${user.nickname || user.username} (${roleName})`
      localStorage.setItem('user', JSON.stringify(user))
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

const handleSuggest = () => {
  if (!keyword.value || keyword.value.trim().length === 0) {
    suggestList.value = []
    showSuggest.value = false
    return
  }

  // 防抖（避免频繁请求）
  clearTimeout(timer)
  timer = setTimeout(async () => {
    try {
      const res = await axios.get('/api/search/suggest', {
        params: { prefix: keyword.value.trim() }
      })

      if (res.data.code === 200 && res.data.data && res.data.data.length > 0) {
        suggestList.value = res.data.data.slice(0, 5)  // 最多显示5条
        showSuggest.value = true
        activeSuggestIndex.value = -1
      } else {
        suggestList.value = []
        showSuggest.value = false
      }
    } catch (e) {
      console.log('suggest error', e)
      suggestList.value = []
      showSuggest.value = false
    }
  }, 300)
}

const selectSuggest = (item) => {
  keyword.value = item
  suggestList.value = []
  showSuggest.value = false
  search()
}

// 高亮建议中匹配的关键词
const highlightSuggest = (text, query) => {
  if (!query || !text) return text
  const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  return text.replace(regex, '<span style="color: #2e7d32; font-weight: bold;">$1</span>')
}






onMounted(() => {

  loadUserInfo()

  document.addEventListener('click', (e) => {
    if (!e.target.closest('.search-box')) {
      suggestList.value = []
      showSuggest.value = false
    }
  })


    // ⭐ 优先使用 URL keyword（解决“点击历史不刷新”）
    if (route.query.keyword) {
      keyword.value = route.query.keyword
      search()
      return
    }


    // 检查是否有重新提交的文献
  const resubmitData = localStorage.getItem('resubmitLiterature')
  if (resubmitData) {
    try {
      const data = JSON.parse(resubmitData)
      newLiterature.value = {
        title: data.title || '',
        authors: data.authors || '',
        journal: data.journal || '',
        year: data.year || null,
        keywords: data.keywords || '',
        abstractText: data.abstractText || '',
        doi: data.doi || ''
      }
      // 清除缓存
      localStorage.removeItem('resubmitLiterature')
      ElMessage.info('请修改文献信息后重新提交')
    } catch (e) {
      console.log('解析失败', e)
    }
  }




  // 恢复上次搜索条件
  const lastKeyword = localStorage.getItem('lastKeyword')
  const lastFilters = localStorage.getItem('lastFilters')
  const lastPage = localStorage.getItem('lastPage')

    if (!route.query.keyword && lastKeyword) {
    keyword.value = lastKeyword
    if (lastPage) {
      currentPage.value = parseInt(lastPage)
    }
    if (lastFilters) {
      try {
        const filtersData = JSON.parse(lastFilters)
        filters.value.author = filtersData.author || ''
        filters.value.journal = filtersData.journal || ''
        filters.value.category = filtersData.category || ''
        filters.value.startYear = filtersData.startYear || null
        filters.value.endYear = filtersData.endYear || null
      } catch (e) {
        console.log('恢复筛选条件失败', e)
      }
    }
    search()
  }
})




// ⭐ 监听路由变化（点击历史关键词触发）
watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword) {
    keyword.value = newKeyword
    currentPage.value = 1
    search()
  }
})

</script>

<style scoped lang="scss">
.home-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.top-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
  color: #2d4a2d;
}

.header {
  text-align: center;
  color: #2d4a2d;
  margin-bottom: 30px;

  h1 {
    font-size: 2.5rem;
    margin-bottom: 10px;
  }
}

.card {
  background: white;
  border-radius: 20px;
  padding: 30px;
  margin-bottom: 30px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.search-box {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  position: relative;
}

.filter-bar {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.literature-item {
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 12px;
  margin-bottom: 15px;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
  }
}

.literature-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
  cursor: pointer;

  &:hover {
    color: #2e7d32;
  }
}

.literature-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.literature-abstract {
  color: #666;
  font-size: 0.9rem;
  line-height: 1.6;
  margin-bottom: 12px;
}

.item-actions {
  display: flex;
  gap: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
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

em {
  color: red !important;
  font-weight: bold;
  font-style: normal;
}

.suggest-box {
  position: absolute;
  top: 48px;
  left: 0;
  right: 80px;           /* 不覆盖搜索按钮 */
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  z-index: 999;
  list-style: none;
  padding: 4px 0;
  margin: 0;
}

.suggest-box li {
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background 0.2s;
}

.suggest-box li:hover,
.suggest-box li.suggest-active {
  background: #f0f2ff;
}

.suggest-icon {
  color: #999;
  font-size: 12px;
}

:deep(.el-button--primary) {
  --el-button-bg-color: #2e7d32;
  --el-button-border-color: #2e7d32;
  --el-button-hover-bg-color: #388e3c;
  --el-button-hover-border-color: #388e3c;
  --el-button-active-bg-color: #1b5e20;
}

:deep(.el-button[text]) {
  color: #2d4a2d;

  &:hover {
    color: #2e7d32;
    background: rgba(46, 125, 50, 0.08);
  }
}

:deep(.el-pagination.is-background .el-pager li.is-active) {
  background-color: #2e7d32;
}

:deep(.el-pagination.is-background .el-pager li:hover) {
  color: #2e7d32;
}

</style>