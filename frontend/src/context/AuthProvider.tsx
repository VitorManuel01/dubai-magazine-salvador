import React, { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { useQueryClient } from '@tanstack/react-query';
import { JwtDadosUsuario } from '../interface/JwtDadosUsuario';
import { jwtDecode } from 'jwt-decode';
import { AuthContext } from './AuthContext';

interface AuthState {
    isAuthenticated: boolean;
    funcao: string;
    expiraEm?: number;
}

const getStoredAuth = (): AuthState => {
    // Remove a persistência usada por versões anteriores da aplicação.
    localStorage.removeItem('token');
    const token = sessionStorage.getItem('token');
    if (!token) {
        return { isAuthenticated: false, funcao: '' };
    }

    try {
        const decoded = jwtDecode<JwtDadosUsuario>(token);
        const valido = Number.isFinite(decoded.exp) && decoded.exp * 1000 > Date.now()
            && ['ROLE_ADMIN', 'ROLE_FUNCIONARIO'].includes(decoded.funcao);
        if (!valido) {
            sessionStorage.removeItem('token');
            return { isAuthenticated: false, funcao: '' };
        }
        return { isAuthenticated: true, funcao: decoded.funcao, expiraEm: decoded.exp * 1000 };
    } catch {
        sessionStorage.removeItem('token');
        return { isAuthenticated: false, funcao: '' };
    }
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [auth, setAuth] = useState<AuthState>(getStoredAuth);
    const queryClient = useQueryClient();

    const encerrarSessaoLocal = useCallback(() => {
        sessionStorage.removeItem('token');
        queryClient.clear();
        setAuth({ isAuthenticated: false, funcao: '' });
    }, [queryClient]);

    useEffect(() => {
        window.addEventListener('auth:unauthorized', encerrarSessaoLocal);
        return () => window.removeEventListener('auth:unauthorized', encerrarSessaoLocal);
    }, [encerrarSessaoLocal]);

    useEffect(() => {
        if (!auth.expiraEm) return;
        const timer = window.setTimeout(encerrarSessaoLocal, Math.max(0, auth.expiraEm - Date.now()));
        return () => window.clearTimeout(timer);
    }, [auth.expiraEm, encerrarSessaoLocal]);

    const login = (token: string) => {
        const decoded = jwtDecode<JwtDadosUsuario>(token);
        if (!Number.isFinite(decoded.exp) || decoded.exp * 1000 <= Date.now()
                || !['ROLE_ADMIN', 'ROLE_FUNCIONARIO'].includes(decoded.funcao)) {
            throw new Error('A API retornou uma sessão inválida.');
        }
        queryClient.clear();
        sessionStorage.setItem('token', token);
        setAuth({ isAuthenticated: true, funcao: decoded.funcao, expiraEm: decoded.exp * 1000 });
    };

    const logout = async () => {
        try {
            await axios.post('/auth/logout', undefined, { timeout: 10_000 });
        } catch {
            // A sessão local deve terminar mesmo se a API estiver indisponível.
        } finally {
            encerrarSessaoLocal();
        }
    };

    return (
        <AuthContext.Provider
            value={{
                isAuthenticated: auth.isAuthenticated,
                funcao: auth.funcao,
                login,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};
