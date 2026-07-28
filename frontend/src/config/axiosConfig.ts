// axiosConfig.ts
import axios from 'axios';

axios.defaults.baseURL = 'http://localhost:8081'; // Defina a URL base
axios.defaults.withCredentials = true;

axios.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default axios;