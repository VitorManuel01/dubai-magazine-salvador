import React, { createContext, useContext, useState } from 'react';
import { AuthContextType } from "../interface/AuthContextType"
import { JwtDadosUsuario } from '../interface/JwtDadosUsuario';
import { jwtDecode } from 'jwt-decode';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthState {
    isAuthenticated: boolean;
    funcao: string;
}

const getStoredAuth = (): AuthState => {
    const token = localStorage.getItem('token');
    if (!token) {
        return { isAuthenticated: false, funcao: '' };
    }

    try {
        const decoded = jwtDecode<JwtDadosUsuario>(token);
        const expirado = decoded.exp !== undefined && decoded.exp * 1000 <= Date.now();
        if (expirado) {
            localStorage.removeItem('token');
            return { isAuthenticated: false, funcao: '' };
        }
        return { isAuthenticated: true, funcao: decoded.funcao };
    } catch {
        localStorage.removeItem('token');
        return { isAuthenticated: false, funcao: '' };
    }
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [auth, setAuth] = useState<AuthState>(getStoredAuth);

    const login = (token: string) => {
        localStorage.setItem('token', token);
        const tokenDecodificado = jwtDecode<JwtDadosUsuario>(token);
        setAuth({ isAuthenticated: true, funcao: tokenDecodificado.funcao });
    };

    const logout = () => {
        localStorage.removeItem('token');
        setAuth({ isAuthenticated: false, funcao: '' });
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

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
