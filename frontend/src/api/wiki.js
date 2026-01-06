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

export const fetchWikiHistory = (slug) => {
  return request({
    url: `/wiki/pages/${slug}/history`,
    method: 'get'
  })
}

export const fetchWikiRevisionDetail = (slug, revisionId) => {
  return request({
    url: `/wiki/pages/${slug}/history/${revisionId}`,
    method: 'get'
  })
}

export const restoreWikiRevision = (slug, revisionId) => {
  return request({
    url: `/wiki/pages/${slug}/history/${revisionId}/restore`,
    method: 'post'
  })
}

export const fetchWikiDiscussions = (slug) => {
  return request({
    url: `/wiki/pages/${slug}/discussions`,
    method: 'get'
  })
}

export const createWikiDiscussion = (slug, data) => {
  return request({
    url: `/wiki/pages/${slug}/discussions`,
    method: 'post',
    data
  })
}

export const fetchWikiStats = () => {
  return request({
    url: '/wiki/pages/stats',
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

export const favoriteWikiPage = (id) => {
  return request({
    url: `/wiki/pages/${id}/favorite`,
    method: 'post'
  })
}

/**
 * 获取待审核的Wiki页面列表（管理员）
 */
export const getPendingWikiPages = (page = 1, pageSize = 10) => {
  return request({
    url: '/wiki/pages/pending',
    method: 'get',
    params: { page, pageSize }
  })
}

/**
 * 审核Wiki页面（管理员）
 */
export const reviewWikiPage = (id, data) => {
  return request({
    url: `/wiki/pages/${id}/review`,
    method: 'post',
    data
  })
}