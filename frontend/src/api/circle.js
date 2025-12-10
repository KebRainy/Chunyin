import request from './request'

export const circleApi = {
  // 获取动态列表（分页）
  listPosts: (page = 1, pageSize = 12) => {
    return request({
      url: '/circle/posts',
      method: 'get',
      params: { page, pageSize }
    })
  },

  // 获取关注用户的动态列表（分页）
  listFeed: (page = 1, pageSize = 12) => {
    return request({
      url: '/circle/feed',
      method: 'get',
      params: { page, pageSize }
    })
  },

  // 发布新动态
  createPost: (data) => {
    return request({
      url: '/circle/posts',
      method: 'post',
      data
    })
  }
}

