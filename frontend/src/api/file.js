import request from './request'

export const deleteFile = (uuid) => {
  return request({
    url: `/files/${uuid}`,
    method: 'delete'
  })
}
