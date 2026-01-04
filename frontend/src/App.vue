<template>
  <div id="app">
    <el-container>
      <el-header>
        <Header />
      </el-header>
      <el-main>
        <router-view />
      </el-main>
      <el-footer>
        <Footer />
      </el-footer>
    </el-container>
    <!-- RAG悬浮窗 -->
    <RAGFloatingWindow />
  </div>
</template>

<script>
import Header from './components/layout/Header.vue'
import Footer from './components/layout/Footer.vue'
import RAGFloatingWindow from './components/rag/RAGFloatingWindow.vue'

export default {
  name: 'App',
  components: {
    Header,
    Footer,
    RAGFloatingWindow
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-height: 100vh;
  background-color: #fff;
}

.el-header {
  padding: 0;
  height: 60px !important;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.05);
  background: #fff;
}

.el-main {
  padding: 32px 24px;
  min-height: calc(100vh - 120px);
  background-color: #fff;
}

.el-footer {
  height: 60px !important;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fff;
  color: #909399;
  border-top: 1px solid #f0f0f0;
}
</style>
