// axiosConfig.ts
import axios from 'axios';

const enderecoConfigurado = import.meta.env.VITE_API_BASE_URL?.trim();
const enderecoPadrao = import.meta.env.DEV
    ? 'http://localhost:8081'
    : window.location.origin;

export const API_BASE_URL = (enderecoConfigurado || enderecoPadrao).replace(/\/$/, '');

const urlApi = new URL(API_BASE_URL, window.location.origin);
const destinoLocal = ['localhost', '127.0.0.1', '[::1]'].includes(urlApi.hostname);

if (import.meta.env.PROD && urlApi.protocol !== 'https:' && !destinoLocal) {
    throw new Error('A API deve utilizar HTTPS fora do ambiente local.');
}

axios.defaults.baseURL = API_BASE_URL;
axios.defaults.withCredentials = false;

axios.interceptors.request.use(config => {
    // A autenticação usa token Bearer, não cookies. Manter isto explícito em
    // cada requisição evita que outra configuração global reative credenciais.
    config.withCredentials = false;

    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default axios;
