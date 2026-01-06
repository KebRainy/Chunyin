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
#app {
  min-height: 100vh;
  position: relative;
}

.el-header {
  padding: 0;
  height: 60px !important;
}

.el-main {
  padding: 32px 24px;
  min-height: calc(100vh - 120px);
  position: relative;
}

.el-footer {
  min-height: auto !important;
  padding: 0 !important;
  background: transparent;
}
</style>
