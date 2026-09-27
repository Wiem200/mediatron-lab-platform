import api from './axios';

export const eventService = {
  getAll: () => api.get('/events/all'),
  getPendingValidation: () => api.get('/events/pending-validation'),
  validate: (id, action) => api.put(`/events/${id}/validate`, { action }),
  getPublic: () => api.get('/events/active'),
  add: (data) => api.post('/events/add', data),
  delete: (id) => api.delete(`/events/${id}`),
};
