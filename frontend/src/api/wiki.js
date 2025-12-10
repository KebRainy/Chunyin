import request from './request'

export const fetchWikiPages = (params) => {
  return request({
    url: '/wiki/pages',
    method: 'get',
    params
  })
}

export const fetchWikiPage = (slug) => {
  return request({
    url: `/wiki/pages/${slug}`,
    method: 'get'
  })
}

export const createWikiPage = (data) => {
  return request({
    url: '/wiki/pages',
    method: 'post',
    data
  })
}

export const updateWikiPage = (id, data) => {
  return request({
    url: `/wiki/pages/${id}`,
    method: 'put',
    data
  })
}
