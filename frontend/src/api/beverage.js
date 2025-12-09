import request from './request'

export const getBeverageList = (params) => {
  return request({
    url: '/beverages',
    method: 'get',
    params
  })
}

export const getBeverageDetail = (id) => {
  return request({
    url: `/beverages/${id}`,
    method: 'get'
  })
}

export const createBeverage = (data) => {
  return request({
    url: '/beverages',
    method: 'post',
    data
  })
}

export const updateBeverage = (id, data) => {
  return request({
    url: `/beverages/${id}`,
    method: 'put',
    data
  })
}

export const deleteBeverage = (id) => {
  return request({
    url: `/beverages/${id}`,
    method: 'delete'
  })
}
