import request from './request'

// 推荐 API
export const recommendApi = {
  // 获取推荐动态
  getRecommendedPosts: (page = 1, size = 10) => {
    return request({
      url: '/recommend/posts',
      method: 'get',
      params: { page, size }
    })
  },

  // 获取推荐用户
  getRecommendedUsers: (page = 1, size = 10) => {
    return request({
      url: '/recommend/users',
      method: 'get',
      params: { page, size }
    })
  },

  // 获取推荐酒吧
  getRecommendedBars: (page = 1, size = 10) => {
    return request({
      url: '/recommend/bars',
      method: 'get',
      params: { page, size }
    })
  },

  // 获取推荐酒饮
  getRecommendedBeverages: (page = 1, size = 10) => {
    return request({
      url: '/recommend/beverages',
      method: 'get',
      params: { page, size }
    })
  },

  // 获取相似动态
  getSimilarPosts: (postId, size = 5) => {
    return request({
      url: `/recommend/similar-posts/${postId}`,
      method: 'get',
      params: { size }
    })
  }
}

