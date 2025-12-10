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

