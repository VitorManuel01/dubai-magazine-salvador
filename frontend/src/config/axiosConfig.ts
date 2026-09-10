// axiosConfig.ts
import axios from 'axios';
import { pertenceApi } from '../utils/destinoApi';

const enderecoConfigurado = import.meta.env.VITE_API_BASE_URL?.trim();
const enderecoPadrao = import.meta.env.DEV
    ? 'http://localhost:8081'
    : window.location.origin;
const exigirHttps = import.meta.env.VITE_REQUIRE_HTTPS === 'true';

export const APP_ENV = import.meta.env.VITE_APP_ENV || import.meta.env.MODE;

export const API_BASE_URL = (enderecoConfigurado || enderecoPadrao).replace(/\/$/, '');

const urlApi = new URL(API_BASE_URL, window.location.origin);
const destinoLocal = ['localhost', '127.0.0.1', '[::1]'].includes(urlApi.hostname);

if (exigirHttps && urlApi.protocol !== 'https:' && !destinoLocal) {
    throw new Error(`A API deve utilizar HTTPS no ambiente ${APP_ENV}.`);
}

axios.defaults.baseURL = API_BASE_URL;
axios.defaults.withCredentials = false;

axios.interceptors.request.use(config => {
    // A autenticação usa token Bearer, não cookies. Manter isto explícito em
    // cada requisição evita que outra configuração global reative credenciais.
    config.withCredentials = false;

    const destino = new URL(axios.getUri(config), window.location.origin);
    const token = sessionStorage.getItem('token');
    if (token && pertenceApi(destino, urlApi)) {
        config.headers.Authorization = `Bearer ${token}`;
    } else {
        // Uma URL absoluta externa nunca deve receber a sessão do funcionário.
        config.headers.delete('Authorization');
    }
    return config;
});

axios.interceptors.response.use(
    response => response,
    error => {
        const tokenAtual = sessionStorage.getItem('token');
        const autorizacaoEnviada = error?.config?.headers?.get?.('Authorization');
        if (error?.response?.status === 401 && tokenAtual
                && autorizacaoEnviada === `Bearer ${tokenAtual}`) {
            sessionStorage.removeItem('token');
            window.dispatchEvent(new Event('auth:unauthorized'));
        }
        return Promise.reject(error);
    }
);

export default axios;
