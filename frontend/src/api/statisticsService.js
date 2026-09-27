import api from './axios';

export const statisticsService = {
  getSummary: () => api.get('/statistics/summary'),
};
