import api from './axios';

export const adminDelegationService = {
  getStatus: () => api.get('/admin-delegation/status'),
  activate: () => api.post('/admin-delegation/activate'),
  deactivate: () => api.post('/admin-delegation/deactivate'),
};
