import request from './request'

/**
 * 获取所有酒类标签列表
 */
export const getAlcoholList = () => {
  return request({
    url: '/alcohols',
    method: 'get'
  })
}

