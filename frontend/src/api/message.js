import request from './request'

export const fetchConversations = () => {
  return request({
    url: '/messages',
    method: 'get'
  })
}

export const fetchConversationWith = (userId) => {
  return request({
    url: `/messages/with/${userId}`,
    method: 'get'
  })
}

export const sendMessage = (userId, data) => {
  return request({
    url: `/messages/with/${userId}`,
    method: 'post',
    data
  })
}

