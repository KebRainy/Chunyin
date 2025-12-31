import request from './request'

export const adminApi = {
  // 获取举报统计
  getReportStats: () => {
    return request({
      url: '/admin/moderation/stats',
      method: 'get'
    })
  },

  // 获取待处理举报列表
  getPendingReports: (page = 1, size = 20) => {
    return request({
      url: '/admin/moderation/reports/pending',
      method: 'get',
      params: { page, size }
    })
  },

  // 获取高风险举报列表
  getHighRiskReports: (page = 1, size = 20, minRiskLevel = 70) => {
    return request({
      url: '/admin/moderation/reports/high-risk',
      method: 'get',
      params: { page, size, minRiskLevel }
    })
  },

  // 获取所有举报列表
  getAllReports: (page = 1, size = 20, status = null) => {
    return request({
      url: '/admin/moderation/reports',
      method: 'get',
      params: { page, size, status }
    })
  },

  // 获取举报详情
  getReportDetail: (id) => {
    return request({
      url: `/admin/moderation/reports/${id}`,
      method: 'get'
    })
  },

  // 处理举报
  handleReport: (id, data) => {
    return request({
      url: `/admin/moderation/reports/${id}/handle`,
      method: 'post',
      data
    })
  },

  // 批量处理举报
  batchHandleReports: (reportIds, status, handleNote, action) => {
    return request({
      url: '/admin/moderation/reports/batch-handle',
      method: 'post',
      data: { reportIds, status, handleNote, action }
    })
  },

  // 禁言用户
  muteUser: (userId, days, reason) => {
    return request({
      url: `/admin/moderation/users/${userId}/mute`,
      method: 'post',
      data: { days, reason }
    })
  },

  // 封禁用户
  banUser: (userId, reason) => {
    return request({
      url: `/admin/moderation/users/${userId}/ban`,
      method: 'post',
      data: { reason }
    })
  }
}

// 举报状态
export const REPORT_STATUS = {
  PENDING: { value: 'PENDING', label: '待处理', type: 'warning' },
  UNDER_REVIEW: { value: 'UNDER_REVIEW', label: '审核中', type: 'primary' },
  CONFIRMED: { value: 'CONFIRMED', label: '已确认违规', type: 'danger' },
  DISMISSED: { value: 'DISMISSED', label: '已驳回', type: 'info' },
  PROCESSED: { value: 'PROCESSED', label: '已处理', type: 'success' }
}

// 处理动作
export const HANDLE_ACTIONS = [
  { value: 'NONE', label: '无操作' },
  { value: 'WARN', label: '警告用户' },
  { value: 'DELETE', label: '删除内容' },
  { value: 'BLOCK', label: '屏蔽内容' },
  { value: 'MUTE_3', label: '禁言3天' },
  { value: 'MUTE_7', label: '禁言7天' },
  { value: 'MUTE_30', label: '禁言30天' },
  { value: 'BAN', label: '封禁账号' }
]

