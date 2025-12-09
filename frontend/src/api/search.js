import request from './request'

export const searchAll = (params) => {
  return request({
    url: '/search',
    method: 'get',
    params
  })
}

