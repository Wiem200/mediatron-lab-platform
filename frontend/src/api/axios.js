import axios from 'axios';
import { getStoredUser } from '../utils/authStore';

const api = axios.create({
    baseURL: 'http://localhost:8080/api', // Your Spring Boot URL
});

// Interceptor to attach the JWT Token automatically
api.interceptors.request.use(
    (config) => {
        const user = getStoredUser();
        if (user && user.token) {
            config.headers.Authorization = `Bearer ${user.token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

export default api;
