import request from './request'

// 举报原因选项（与后端 ReportReason 枚举对应）
export const REPORT_REASONS = [
  { value: 'SPAM', label: '垃圾广告' },
  { value: 'ABUSE', label: '辱骂攻击' },
  { value: 'PORNOGRAPHY', label: '色情低俗' },
  { value: 'ILLEGAL', label: '违法违规' },
  { value: 'FRAUD', label: '欺诈信息' },
  { value: 'MISINFORMATION', label: '虚假信息' },
  { value: 'HARASSMENT', label: '骚扰行为' },
  { value: 'OTHER', label: '其他原因' }
]

// 举报 API
export const reportApi = {
  // 提交举报
  submitReport: (data) => {
    return request({
      url: '/reports',
      method: 'post',
      data
    })
  },

  // 获取我的举报列表
  getMyReports: (page = 1, size = 20) => {
    return request({
      url: '/reports/my',
      method: 'get',
      params: { page, size }
    })
  },

  // 获取举报详情
  getReportDetail: (id) => {
    return request({
      url: `/reports/${id}`,
      method: 'get'
    })
  }
}

