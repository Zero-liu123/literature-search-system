module.exports = {
  devServer: {
    port: 8081,  // 前端改成 8081
    proxy: {
      '/api': {
        target: 'http://localhost:8080',//后端8080
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}