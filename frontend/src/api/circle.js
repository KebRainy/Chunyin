import request from './request'

export const fetchCirclePosts = (params) => {
  return request({
    url: '/circle/posts',
    method: 'get',
    params
  })
}

export const createCirclePost = (data) => {
  return request({
    url: '/circle/posts',
    method: 'post',
    data
  })
}

