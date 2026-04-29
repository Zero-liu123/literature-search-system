<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  min-height: 100vh;
  background-color: #f0f4ef;
  background-image:
    radial-gradient(circle at 1px 1px, rgba(100, 140, 90, 0.15) 1px, transparent 0);
  background-size: 28px 28px;
}
</style>