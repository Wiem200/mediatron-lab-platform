import api from './axios';

export const memberPublicService = {
  getMembers: () => api.get('/members/public'),
  getMemberById: (id) => api.get(`/members/public/${id}`),
};
