<template>
  <div class="home">
    <el-card class="welcome-card">
      <h1>欢迎来到酒饮平台</h1>
      <p>探索、分享、评论各种酒精饮料</p>
      <el-button type="primary" size="large" @click="goToBeverages">
        开始探索
      </el-button>
    </el-card>

    <div class="features">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover">
            <el-icon :size="48" color="#409eff"><Reading /></el-icon>
            <h3>Wiki 式编辑</h3>
            <p>用户共同维护酒精饮料信息库</p>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <el-icon :size="48" color="#67c23a"><ChatDotRound /></el-icon>
            <h3>自由评论</h3>
            <p>分享您的品鉴体验和感受</p>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <el-icon :size="48" color="#e6a23c"><ShoppingCart /></el-icon>
            <h3>购买链接</h3>
            <p>便捷获取购买渠道信息</p>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'

export default {
  name: 'Home',
  setup() {
    const router = useRouter()

    const goToBeverages = () => {
      router.push('/beverages')
    }

    return {
      goToBeverages
    }
  }
}
</script>

<style scoped>
.home {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-card {
  text-align: center;
  padding: 60px 20px;
  margin-bottom: 40px;
}

.welcome-card h1 {
  font-size: 48px;
  margin-bottom: 20px;
  color: #303133;
}

.welcome-card p {
  font-size: 20px;
  color: #606266;
  margin-bottom: 40px;
}

.features {
  margin-bottom: 40px;
}

.features .el-card {
  text-align: center;
  padding: 30px 20px;
}

.features h3 {
  margin: 20px 0 10px;
  color: #303133;
}

.features p {
  color: #909399;
}
</style>
