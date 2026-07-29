import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useDadosLoginMutate } from "../../hooks/useDadosLoginMutate";
import { CadastrarClientes } from "../cadastros/cadastrarCliente";

const Login = () => {
  const navigate = useNavigate();
  const [emailOrLogin, setEmailOrLogin] = useState("");
  const [senha, setSenha] = useState("");
  const { mutate, isPending, error, isSuccess } = useDadosLoginMutate();
  const [isModalOpen, setIsModalOpen] = useState(false);

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    mutate(
      { emailOrLogin, senha },
      {
        onSuccess: () => navigate("/", { replace: true }),
      }
    );
  }
  const handleOpenModal = () => {
    setIsModalOpen(prev => !prev)
  }

  return (
      <div className="login-page">
        <section className="login-card">
        <h2>Acesse sua conta</h2>
        <p className="login-card__intro">Entre para gerenciar os produtos da loja.</p>
        <form className="login-form" onSubmit={handleLogin}>
          <div className="login-field">
            <label htmlFor="login-identifier">E-mail ou login</label>
            <input
              id="login-identifier"
              type="text"
              value={emailOrLogin}
              onChange={(e) => setEmailOrLogin(e.target.value)}
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
          
          
          {isSuccess && <p className="login-feedback login-feedback--success">Login realizado com sucesso!</p>}
          {error && <p className="login-feedback login-feedback--error">Erro ao realizar login</p>}

        </form>
        <button className="btn btn-outline-primary login-register" onClick={handleOpenModal}>Criar cadastro</button>
        {isModalOpen && <CadastrarClientes closeModal={handleOpenModal} />}
        </section>
      </div>
  );
};

export default Login;
