<template>
  <div class="editor-page">
    <el-card>
      <template #header>
        <div class="header">
          <h2>{{ isEdit ? '编辑条目' : '新建条目' }}</h2>
          <el-button @click="goBack">返回列表</el-button>
        </div>
      </template>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="简要概述条目" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="12"
            placeholder="可以使用 Markdown / 文本描述"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            {{ isEdit ? '更新条目' : '创建条目' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createWikiPage, fetchWikiPage, updateWikiPage } from '@/api/wiki'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const form = reactive({
  title: '',
  summary: '',
  content: ''
})

const isEdit = computed(() => !!route.params.id)

const loadPage = async () => {
  if (!isEdit.value) return
  const res = await fetchWikiPage(route.params.id)
  Object.assign(form, res.data)
}

const handleSubmit = async () => {
  loading.value = true
  try {
    if (isEdit.value) {
      await updateWikiPage(route.params.id, form)
      ElMessage.success('已提交修改，等待审核')
    } else {
      await createWikiPage(form)
      ElMessage.success('已提交，等待审核')
    }
    router.push('/wiki')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/wiki')
}

onMounted(() => {
  loadPage()
})
</script>

<style scoped>
.editor-page {
  max-width: 900px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
