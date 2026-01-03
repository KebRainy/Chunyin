import request from './request'

/**
 * 创建活动
 */
export const createActivity = (data) => {
  return request({
    url: '/activities',
    method: 'post',
    data
  })
}

/**
 * 根据酒类标签推荐酒吧
 */
export const recommendBarsByAlcoholIds = (alcoholIds, latitude, longitude, limit = 10) => {
  // 严格验证参数
  if (!alcoholIds) {
    return Promise.reject(new Error('请至少选择一个酒类标签'))
  }
  if (!Array.isArray(alcoholIds)) {
    return Promise.reject(new Error('alcoholIds必须是数组'))
  }
  if (alcoholIds.length === 0) {
    return Promise.reject(new Error('请至少选择一个酒类标签'))
  }
  
  // 过滤掉无效值
  const validIds = alcoholIds.filter(id => id != null && id !== undefined && !isNaN(id))
  if (validIds.length === 0) {
    return Promise.reject(new Error('请至少选择一个有效的酒类标签'))
  }
  
  // 使用axios的params配置，会自动处理数组参数为 alcoholIds=1&alcoholIds=2 格式
  const params = {
    alcoholIds: validIds, // axios会自动将数组转换为重复的参数
    limit: limit
  }
  
  // 只有当值不为null和undefined时才添加
  if (latitude != null && latitude !== undefined && !isNaN(latitude)) {
    params.latitude = latitude
  }
  if (longitude != null && longitude !== undefined && !isNaN(longitude)) {
    params.longitude = longitude
  }
  
  return request({
    url: '/activities/bars/recommend',
    method: 'get',
    params: params
  })
}

/**
 * 根据酒类推荐酒吧（保留用于兼容）
 * @deprecated 使用 recommendBarsByAlcoholIds 代替
 */
export const recommendBarsByBeverage = (beverageId, latitude, longitude, limit = 10) => {
  return request({
    url: '/activities/bars/recommend-by-beverage',
    method: 'get',
    params: { beverageId, latitude, longitude, limit }
  })
}

/**
 * 搜索酒吧（模糊搜索）
 */
export const searchBars = (keyword, limit = 20) => {
  return request({
    url: '/activities/bars/search',
    method: 'get',
    params: { keyword, limit }
  })
}

/**
 * 获取我发起的活动
 */
export const getMyCreatedActivities = (page = 1, size = 10) => {
  return request({
    url: '/activities/my-created',
    method: 'get',
    params: { page, size }
  })
}

/**
 * 获取推荐的活动
 */
export const getRecommendedActivities = (barId, beverageId, page = 1, size = 10) => {
  return request({
    url: '/activities/recommended',
    method: 'get',
    params: { barId, beverageId, page, size }
  })
}

/**
 * 获取我参与的活动
 */
export const getMyParticipatedActivities = (page = 1, size = 10) => {
  return request({
    url: '/activities/my-participated',
    method: 'get',
    params: { page, size }
  })
}

/**
 * 获取活动详情
 */
export const getActivityById = (id) => {
  return request({
    url: `/activities/${id}`,
    method: 'get'
  })
}

/**
 * 参与活动
 */
export const joinActivity = (id) => {
  return request({
    url: `/activities/${id}/join`,
    method: 'post'
  })
}

/**
 * 取消参与活动
 */
export const cancelJoinActivity = (id) => {
  return request({
    url: `/activities/${id}/cancel`,
    method: 'post'
  })
}

/**
 * 审核活动（管理员）
 */
export const reviewActivity = (id, data) => {
  return request({
    url: `/activities/${id}/review`,
    method: 'post',
    data
  })
}

/**
 * 获取待审核的活动列表（管理员）
 */
export const getPendingActivities = (page = 1, size = 10) => {
  return request({
    url: '/activities/pending',
    method: 'get',
    params: { page, size }
  })
}

