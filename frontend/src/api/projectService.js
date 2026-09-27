import api from './axios';

export const projectService = {
  getAll: () => api.get('/projects/all'),
  getPublic: () => api.get('/projects'),
  getPublicById: (id) => api.get(`/projects/public/${id}`),
  getById: (id) => api.get(`/projects/${id}`),
  getPending: () => api.get('/projects/pending'),
  validate: (id, data) => api.put(`/projects/${id}/validate`, data),
  add: (data) => api.post('/projects/add', data),
  update: (id, data) => api.put(`/projects/update/${id}`, data),
  delete: (id, confirm = false) => api.delete(`/projects/${id}`, { params: { confirm } }),
};
