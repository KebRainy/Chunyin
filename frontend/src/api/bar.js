import request from './request'

/**
 * 注册酒吧（提交申请）
 */
export const registerBar = (data) => {
  return request({
    url: '/bars/register',
    method: 'post',
    data
  })
}

/**
 * 获取酒吧详情
 */
export const getBarDetail = (id) => {
  return request({
    url: `/bars/${id}`,
    method: 'get'
  })
}

/**
 * 搜索附近的酒吧
 */
export const searchNearbyBars = (params) => {
  return request({
    url: '/bars/nearby',
    method: 'get',
    params
  })
}

/**
 * 按城市搜索酒吧
 */
export const searchBarsByCity = (city) => {
  return request({
    url: `/bars/city/${city}`,
    method: 'get'
  })
}

/**
 * 按名称搜索酒吧
 */
export const searchBarsByName = (params) => {
  return request({
    url: '/bars/search',
    method: 'get',
    params
  })
}

/**
 * 获取当前用户提交的酒吧申请列表
 */
export const getMyApplications = () => {
  return request({
    url: '/bars/applications/my',
    method: 'get'
  })
}

/**
 * 审核酒吧申请（管理员功能）
 */
export const reviewBarApplication = (id, params) => {
  return request({
    url: `/bars/applications/${id}/review`,
    method: 'put',
    params
  })
}

/**
 * 添加酒吧评价
 */
export const addBarReview = (data) => {
  return request({
    url: '/bars/reviews',
    method: 'post',
    data
  })
}

/**
 * 获取酒吧的评价列表
 */
export const getBarReviews = (barId) => {
  return request({
    url: `/bars/${barId}/reviews`,
    method: 'get'
  })
}

/**
 * 删除评价
 */
export const deleteBarReview = (reviewId) => {
  return request({
    url: `/bars/reviews/${reviewId}`,
    method: 'delete'
  })
}

