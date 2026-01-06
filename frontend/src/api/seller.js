import request from './request'

// 提交商家认证申请
export const submitSellerApplication = (data) => {
  return request({
    url: '/seller/applications',
    method: 'post',
    data
  })
}

// 获取我的申请记录
export const getMySellerApplications = () => {
  return request({
    url: '/seller/applications/my',
    method: 'get'
  })
}

// 获取待审核申请（管理员）
export const getPendingSellerApplications = () => {
  return request({
    url: '/seller/applications/pending',
    method: 'get'
  })
}

// 审核申请（管理员）
export const reviewSellerApplication = (id, params) => {
  return request({
    url: `/seller/applications/${id}/review`,
    method: 'put',
    params
  })
}
