import api from './axios';

export const memberService = {
  getAll: () => api.get('/members/all'),
  getById: (id) => api.get(`/members/${id}`),
  add: (data) => api.post('/members/add', data),
  update: (id, data) => api.put(`/members/update/${id}`, data),
  delete: (id, confirm = false) => api.delete(`/members/delete/${id}`, {
    params: { confirm },
  }),
  assignRole: (userId, roleName) => api.put(`/users/${userId}/assign-role`, roleName, {
    headers: { 'Content-Type': 'text/plain' },
  }),
};
