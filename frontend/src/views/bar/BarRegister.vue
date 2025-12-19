<template>
  <div class="bar-register">
    <el-card class="register-card">
      <template #header>
        <h2>注册酒吧</h2>
        <p class="subtitle">提交您的酒吧信息，待管理员审核通过后将在平台展示</p>
      </template>

      <el-form 
        :model="form" 
        :rules="rules" 
        ref="formRef"
        label-width="120px"
      >
        <el-form-item label="酒吧名称" prop="name">
          <el-input 
            v-model="form.name" 
            placeholder="请输入酒吧名称"
            maxlength="100"
          />
        </el-form-item>

        <el-form-item label="省份" prop="province">
          <el-input 
            v-model="form.province" 
            placeholder="请输入省份"
            maxlength="50"
          />
        </el-form-item>

        <el-form-item label="城市" prop="city">
          <el-input 
            v-model="form.city" 
            placeholder="请输入城市"
            maxlength="50"
          />
        </el-form-item>

        <el-form-item label="区/县" prop="district">
          <el-input 
            v-model="form.district" 
            placeholder="请输入区/县（可选）"
            maxlength="50"
          />
        </el-form-item>

        <el-form-item label="详细地址" prop="address">
          <el-input 
            v-model="form.address" 
            placeholder="请输入详细地址"
            type="textarea"
            :rows="2"
            maxlength="200"
          />
        </el-form-item>

        <el-form-item label="营业时间">
          <el-row :gutter="10">
            <el-col :span="12">
              <el-form-item prop="openingTime">
                <el-time-select
                  v-model="form.openingTime"
                  start="00:00"
                  step="00:30"
                  end="23:30"
                  placeholder="开始时间"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item prop="closingTime">
                <el-time-select
                  v-model="form.closingTime"
                  start="00:00"
                  step="00:30"
                  end="23:30"
                  placeholder="结束时间"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="联系电话" prop="contactPhone">
          <el-input 
            v-model="form.contactPhone" 
            placeholder="请输入联系电话"
            maxlength="20"
          />
        </el-form-item>

        <el-form-item label="主营酒类" prop="mainBeverages">
          <el-input 
            v-model="form.mainBeverages" 
            placeholder="例如：威士忌、鸡尾酒、精酿啤酒等"
            maxlength="200"
          />
        </el-form-item>

        <el-form-item label="酒吧简介" prop="description">
          <el-input 
            v-model="form.description" 
            type="textarea"
            :rows="4"
            placeholder="请介绍您的酒吧特色、环境、氛围等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            提交申请
          </el-button>
          <el-button @click="$router.back()">
            取消
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="my-bars-card" v-if="myBars.length > 0">
      <template #header>
        <h3>我的酒吧申请</h3>
      </template>
      
      <el-table :data="myBars" stripe>
        <el-table-column prop="name" label="酒吧名称" />
        <el-table-column prop="city" label="城市" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING'" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.status === 'APPROVED'" type="success">已通过</el-tag>
            <el-tag v-else-if="row.status === 'REJECTED'" type="danger">已拒绝</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              text
              @click="$router.push(`/bars/${row.id}`)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { registerBar, getMyApplications } from '@/api/bar'
import { ElMessage } from 'element-plus'

export default {
  name: 'BarRegister',
  data() {
    return {
      form: {
        name: '',
        province: '',
        city: '',
        district: '',
        address: '',
        openingTime: '',
        closingTime: '',
        contactPhone: '',
        mainBeverages: '',
        description: ''
      },
      rules: {
        name: [
          { required: true, message: '请输入酒吧名称', trigger: 'blur' }
        ],
        province: [
          { required: true, message: '请输入省份', trigger: 'blur' }
        ],
        city: [
          { required: true, message: '请输入城市', trigger: 'blur' }
        ],
        address: [
          { required: true, message: '请输入详细地址', trigger: 'blur' }
        ],
        contactPhone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' }
        ]
      },
      submitting: false,
      myBars: []
    }
  },
  mounted() {
    this.loadMyBars()
  },
  methods: {
    async handleSubmit() {
      const valid = await this.$refs.formRef.validate().catch(() => false)
      if (!valid) return
      
      this.submitting = true
      try {
        await registerBar(this.form)
        ElMessage.success('提交成功，请等待管理员审核')
        
        // 重置表单
        this.$refs.formRef.resetFields()
        
        // 重新加载我的酒吧列表
        await this.loadMyBars()
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error(error.message || '提交失败')
      } finally {
        this.submitting = false
      }
    },
    
    async loadMyBars() {
      try {
        const { data } = await getMyApplications()
        this.myBars = data || []
      } catch (error) {
        console.error('加载我的酒吧申请列表失败:', error)
      }
    },
    
    formatDate(dateTime) {
      if (!dateTime) return ''
      const date = new Date(dateTime)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
  }
}
</script>

<style scoped>
.bar-register {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.register-card {
  margin-bottom: 20px;
}

.subtitle {
  margin: 5px 0 0 0;
  color: #999;
  font-size: 14px;
}

.my-bars-card {
  margin-top: 20px;
}

h2, h3 {
  margin: 0;
}
</style>

