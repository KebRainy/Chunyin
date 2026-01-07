import request from './request'

export const fetchUserProfile = (id) => {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

export const updateProfile = (data) => {
  return request({
    url: '/users/profile',
    method: 'put',
    data
  })
}

export const changePassword = (data) => {
  return request({
    url: '/users/password',
    method: 'put',
    data
  })
}

export const followUser = (id) => {
  return request({
    url: `/users/${id}/follow`,
    method: 'post'
  })
}

export const unfollowUser = (id) => {
  return request({
    url: `/users/${id}/follow`,
    method: 'delete'
  })
}

export const getFollowees = () => {
  return request({
    url: '/users/followees',
    method: 'get'
  })
}

export const updateMessagePolicy = (data) => {
  return request({
    url: '/users/message-policy',
    method: 'put',
    data
  })
}

export const getCollections = (params) => {
  return request({
    url: '/collections',
    method: 'get',
    params
  })
}

export const getFootprints = (params) => {
  return request({
    url: '/footprints',
    method: 'get',
    params
  })
}

export const recordFootprint = (data) => {
  return request({
    url: '/footprints',
    method: 'post',
    data
  })
}

export const blockRecommendedUser = (id) => {
  return request({
    url: `/users/${id}/block-recommend`,
    method: 'post'
  })
}
