import request from './request'

export const fetchTodayQuestion = () => {
  return request({
    url: '/daily-question/today',
    method: 'get'
  })
}

export const answerDailyQuestion = (questionId, optionIndex) => {
  return request({
    url: `/daily-question/${questionId}/answer`,
    method: 'post',
    data: { optionIndex }
  })
}
