import React, { useState } from "react";
import axios from "axios";
import { Link, useNavigate } from "react-router-dom";
import { jwtDecode } from "jwt-decode";
import { useDadosLoginMutate } from "../../hooks/useDadosLoginMutate";
import { JwtDadosUsuario } from "../../interface/JwtDadosUsuario";

interface ErroLogin {
  erro?: string;
}

const Login = () => {
  const navigate = useNavigate();
  const [codigoSantri, setCodigoSantri] = useState("");
  const [senha, setSenha] = useState("");
  const { mutate, isPending, error } = useDadosLoginMutate();
  const mensagemErro = axios.isAxiosError<ErroLogin>(error)
    ? error.response?.data?.erro ?? "Código Santri ou senha inválidos."
    : "Código Santri ou senha inválidos.";

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    mutate(
      { codigoSantri, senha },
      {
        onSuccess: ({ token }) => {
          const { funcao } = jwtDecode<JwtDadosUsuario>(token);
          navigate(
            funcao === "ROLE_ADMIN" ? "/minha-conta" : "/vitrine-loja",
            { replace: true }
          );
        },
      }
    );
  };

  return (
      <div className="login-page">
        <section className="login-card">
        <span className="login-card__eyebrow">Acesso interno</span>
        <h2>Entrar no sistema</h2>
        <p className="login-card__intro">
          Área exclusiva para administradores e funcionários da Dubai Magazine.
        </p>
        <form className="login-form" onSubmit={handleLogin}>
          <div className="login-field">
            <label htmlFor="login-identifier">Código Santri</label>
            <input
              id="login-identifier"
              type="text"
              value={codigoSantri}
              onChange={(e) => setCodigoSantri(e.target.value)}
              maxLength={50}
              autoComplete="username"
              required
            />
          </div>
          <div className="login-field">
            <label htmlFor="login-password">Senha</label>
            <input
              id="login-password"
              type="password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              autoComplete="current-password"
              required
            />
          </div>
          <button className="btn btn-primary login-submit" type="submit" disabled={isPending}>
            {isPending ? "Carregando..." : "Login"}
          </button>
          
          
          {error && (
            <p className="login-feedback login-feedback--error" role="alert">
              {mensagemErro}
            </p>
          )}

        </form>
        <Link className="btn btn-outline-secondary login-catalog-link" to="/produtos">
          <i className="bi bi-arrow-left" aria-hidden="true" />
          Voltar ao catálogo
        </Link>
        </section>
      </div>
  );
};

export default Login;
