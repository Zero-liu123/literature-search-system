import axios from 'axios'

const request = axios.create({
    baseURL: '/api',
    timeout: 30000
})

// 请求拦截器：自动添加 token
request.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

// 响应拦截器：统一处理错误
request.interceptors.response.use(
    response => {
        return response.data
    },
    error => {
        console.error('请求失败:', error)
        return Promise.reject(error)
    }
)

// ========== 文献相关 API ==========
export const literatureAPI = {
    // 搜索文献
    search(params) {
        return request.post('/literature/search', params)
    },
    // 添加文献
    add(formData) {
        return request.post('/literature/add', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },
    // 获取详情
    getDetail(id) {
        return request.get(`/literature/${id}`)
    },
    // 删除文献
    delete(id) {
        return request.delete(`/literature/${id}`)
    },
    // ✅ 从 userAPI 移过来！
    update(id, data) {
        return request.put(`/literature/${id}`, data)
    },
    // 获取我的贡献列表
    getMyContributions() {
        return request.get('/literature/my-contributions')
    },
    // 获取收藏列表
    getFavorites() {
        return request.get('/literature/favorites')
    },
    // 添加收藏
    addFavorite(id) {
        return request.post(`/literature/favorite/${id}`)
    },
    // 取消收藏
    removeFavorite(id) {
        return request.delete(`/literature/favorite/${id}`)
    },
    // 获取检索历史
    getSearchHistory() {
        return request.get('/literature/search-history')
    },
    // 保存检索历史
    saveSearchHistory(keyword) {
        return request.post('/literature/search-history/save', { keyword })
    },
}

// ========== 用户相关 API ==========
export const userAPI = {
    // ✅ 新增 login
    login(data) {
        return request.post('/user/login', data)
    },
    // ✅ 新增 register
    register(data) {
        return request.post('/user/register', data)
    },
    // 获取当前用户信息
    getCurrent() {
        return request.get('/user/current')
    },
    // 获取用户列表（管理员）
    getUserList() {
        return request.get('/user/list')
    },
    // 修改角色
    updateRole(userId, role) {
        return request.put(`/user/${userId}/role?role=${role}`)
    },
    // 禁用用户
    disableUser(userId) {
        return request.put(`/user/${userId}/disable`)
    },
    // 启用用户
    enableUser(userId) {
        return request.put(`/user/${userId}/enable`)
    },
    // 删除用户
    deleteUser(userId) {
        return request.delete(`/user/${userId}/permanent`)
    },
    // 修改密码
    changePassword(data) {
        return request.put('/user/password', data)
    },
    // 更新个人资料
    updateProfile(data) {
        const formData = new FormData()
        formData.append('nickname', data.nickname)
        if (data.email) formData.append('email', data.email)
        if (data.avatar) formData.append('avatar', data.avatar)
        return request.put('/user/profile', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },
    // 重置密码
    resetPassword(userId) {
        return request.put(`/user/${userId}/reset-password`)
    },
}
