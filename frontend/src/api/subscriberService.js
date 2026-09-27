import api from './axios';

export const subscriberService = {
  subscribe: (data) => api.post('/public/subscribe', data),
  getAllSubscribers: () => api.get('/admin/subscribers/all'),
  searchSubscribers: (query) => api.get('/admin/subscribers/search', { params: { email: query } }),
  deleteSubscriber: (id) => api.delete(`/admin/subscribers/${id}`),
  toggleStatus: (id, newStatus) => api.patch(`/admin/subscribers/${id}/status`, { status: newStatus }),
};
