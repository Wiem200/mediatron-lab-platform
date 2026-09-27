import api from './axios';

export const phdService = {
  getAll: () => api.get('/phd-students/all'),
  getPublicationOptions: () => api.get('/phd-students/publication-options'),
  add: (data) => api.post('/phd-students/add', data),
  update: (id, data) => api.put(`/phd-students/update/${id}`, data),
  delete: (id, confirm = false) => api.delete(`/phd-students/${id}`, { params: { confirm } }),
};
