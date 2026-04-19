import request from '@/utils/request'

// 获取待办列表
export function getTodoList(params) {
  return request({
    url: '/todos',
    method: 'get',
    params
  })
}

// 创建待办
export function createTodo(data) {
  return request({
    url: '/todos',
    method: 'post',
    data
  })
}

// 更新待办
export function updateTodo(id, data) {
  return request({
    url: `/todos/${id}`,
    method: 'put',
    data
  })
}

// 删除待办
export function deleteTodo(id) {
  return request({
    url: `/todos/${id}`,
    method: 'delete'
  })
}

// 完成待办
export function completeTodo(id) {
  return request({
    url: `/todos/${id}/complete`,
    method: 'put'
  })
}

// Export alias for getTodos - compatibility with component imports
export { getTodoList as getTodos }
