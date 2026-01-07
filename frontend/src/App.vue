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
    <RAGFloatingWindow v-if="showRAGFloatingWindow" />
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
  },
  computed: {
    showRAGFloatingWindow() {
      return this.$route?.name !== 'WikiEditor'
    }
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
  background-color: transparent;
  position: relative;
  z-index: 1;
}

.el-header {
  padding: 0 !important;
  height: auto !important;
  min-height: 68px !important;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.05);
  background: transparent;
  position: relative;
  z-index: 100;
  width: 100%;
}

.el-main {
  padding: 32px 24px;
  min-height: calc(100vh - 120px);
  background-color: transparent;
  position: relative;
  z-index: 1;
}

.el-footer {
  height: auto !important;
  min-height: auto !important;
  padding: 0 !important;
  display: block;
  background: transparent;
  position: relative;
  z-index: 100;
  width: 100%;
}
</style>
