import api from './axios';
import { setStoredUser } from '../utils/authStore';

const login = async (email, password) => {
  const response = await api.post('/auth/login', { email, password });
  if (response.data.success) {
    setStoredUser(response.data.data);
  }
  return response.data;
};

const register = async (email, password) => {
  const response = await api.post('/auth/register', { email, password });
  return response.data;
};

const forgotPassword = async (email) => {
  const response = await api.post('/auth/forgot-password', { email });
  return response.data;
};

const resetPassword = async (token, newPassword) => {
  const response = await api.post(`/auth/reset-password?token=${encodeURIComponent(token)}`, { newPassword });
  return response.data;
};

const logout = () => {
  setStoredUser(null);
};

export const authService = { login, register, forgotPassword, resetPassword, logout };
