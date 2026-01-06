<template>
  <div class="seller-register">
    <el-card class="register-card">
      <template #header>
        <h2>商家资质认证</h2>
        <p class="subtitle">成为认证商家后，您可以注册和管理您的酒吧</p>
      </template>

      <!-- 状态展示区 -->
      <div v-if="existingApplication" class="status-alert">
        <el-alert v-if="existingApplication.status === 'PENDING'" title="您的商家认证申请正在审核中" type="info"
          description="请耐心等待管理员审核，通常需要1-3个工作日。" show-icon :closable="false" />
        <el-alert v-else-if="existingApplication.status === 'APPROVED'" title="恭喜！您已通过商家认证" type="success"
          description="您现在可以去注册您的酒吧了。" show-icon :closable="false">
          <div style="margin-top: 10px">
            <el-button type="primary" size="small" @click="$router.push('/bars/register')">去注册酒吧</el-button>
          </div>
        </el-alert>
        <el-alert v-else-if="existingApplication.status === 'REJECTED'" title="申请被拒绝" type="error"
          :description="`拒绝原因: ${existingApplication.reviewNote || '未填写'}`" show-icon :closable="false">
          <div style="margin-top: 10px">
            <el-button type="primary" size="small" @click="resetForm">重新申请</el-button>
          </div>
        </el-alert>
      </div>

      <!-- 申请表单 -->
      <el-form v-if="!existingApplication || (existingApplication.status === 'REJECTED' && isResubmitting)"
        :model="form" :rules="rules" ref="formRef" label-width="120px" class="register-form">
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入您的真实姓名" />
        </el-form-item>

        <el-form-item label="身份证号" prop="idCardNumber">
          <el-input v-model="form.idCardNumber" placeholder="请输入您的身份证号码" />
        </el-form-item>

        <el-form-item label="营业执照URL" prop="licenseImageUrl">
          <el-input v-model="form.licenseImageUrl" placeholder="请输入营业执照图片链接（可选）" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitForm">提交申请</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitSellerApplication, getMySellerApplications } from '@/api/seller'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const existingApplication = ref(null)
const isResubmitting = ref(false)

const form = ref({
  realName: '',
  idCardNumber: '',
  licenseImageUrl: ''
})

const rules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  idCardNumber: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '身份证格式不正确', trigger: 'blur' }
  ]
}

const checkStatus = async () => {
  try {
    const res = await getMySellerApplications()
    if (res.code === 200 && res.data && res.data.length > 0) {
      // 取最新的一条
      existingApplication.value = res.data[0]
    }
  } catch (error) {
    console.error('Failed to check application status', error)
  }
}

const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const res = await submitSellerApplication(form.value)
        if (res.code === 200) {
          ElMessage.success('申请提交成功')
          isResubmitting.value = false
          await checkStatus()
        }
      } catch (error) {
        // 错误已经在 request 拦截器中处理
      } finally {
        submitting.value = false
      }
    }
  })
}

const resetForm = () => {
  isResubmitting.value = true
  form.value = {
    realName: '',
    idCardNumber: '',
    licenseImageUrl: ''
  }
}

onMounted(() => {
  checkStatus()
})
</script>

<style scoped>
.seller-register {
  max-width: 800px;
  margin: 40px auto;
  padding: 0 20px;
}

.subtitle {
  color: #666;
  font-size: 14px;
  margin-top: 5px;
}

.status-alert {
  margin-bottom: 20px;
}

.register-form {
  margin-top: 20px;
}
</style>
