import { type FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios, { AxiosError } from 'axios';
import { Link } from 'react-router-dom';
import './MinhaConta.css';

interface FuncionarioForm {
  codigoSantri: string;
  senha: string;
  confirmarSenha: string;
  nomeFuncionario: string;
  CPF: string;
  sexo: string;
  dataNascimento: string;
  CEP: string;
  bairro: string;
  telefone: string;
}

interface FuncionarioResumo {
  id: string;
  codigoSantri: string;
  nomeFuncionario: string;
  funcao: string;
  ativo: boolean;
}

interface AlteracaoStatusFuncionario {
  id: string;
  ativo: boolean;
}

const CHAVE_FUNCIONARIOS = ['funcionarios'] as const;

const listarFuncionarios = async (): Promise<FuncionarioResumo[]> => {
  const resposta = await axios.get<FuncionarioResumo[]>('/funcionario');
  return resposta.data;
};

const atualizarStatusFuncionario = async ({
  id,
  ativo,
}: AlteracaoStatusFuncionario): Promise<FuncionarioResumo> => {
  const resposta = await axios.put<FuncionarioResumo>(`/funcionario/${id}/status`, { ativo });
  return resposta.data;
};

const estadoInicial: FuncionarioForm = {
  codigoSantri: '',
  senha: '',
  confirmarSenha: '',
  nomeFuncionario: '',
  CPF: '',
  sexo: '',
  dataNascimento: '',
  CEP: '',
  bairro: '',
  telefone: '',
};

const apenasNumeros = (valor: string, limite: number) =>
  valor.replace(/\D/g, '').slice(0, limite);

function FuncionariosAdmin() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<FuncionarioForm>(estadoInicial);
  const [enviando, setEnviando] = useState(false);
  const [sucesso, setSucesso] = useState('');
  const [erro, setErro] = useState('');
  const [erroStatus, setErroStatus] = useState('');

  const funcionariosQuery = useQuery({
    queryKey: CHAVE_FUNCIONARIOS,
    queryFn: listarFuncionarios,
  });

  const statusMutation = useMutation({
    mutationFn: atualizarStatusFuncionario,
    onMutate: () => setErroStatus(''),
    onSuccess: (funcionarioAtualizado) => {
      queryClient.setQueryData<FuncionarioResumo[]>(CHAVE_FUNCIONARIOS, (funcionarios = []) =>
        funcionarios.map((funcionario) =>
          funcionario.id === funcionarioAtualizado.id ? funcionarioAtualizado : funcionario
        )
      );
    },
    onError: () => {
      setErroStatus('Não foi possível alterar o acesso do funcionário. Tente novamente.');
    },
  });

  const alterar = (campo: keyof FuncionarioForm, valor: string) => {
    setForm((atual) => ({ ...atual, [campo]: valor }));
  };

  const cadastrar = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSucesso('');
    setErro('');

    if (form.senha !== form.confirmarSenha) {
      setErro('As senhas informadas não coincidem.');
      return;
    }

    setEnviando(true);
    try {
      const dados = {
        codigoSantri: form.codigoSantri,
        senha: form.senha,
        nomeFuncionario: form.nomeFuncionario,
        CPF: form.CPF,
        sexo: form.sexo,
        dataNascimento: form.dataNascimento || null,
        CEP: form.CEP,
        bairro: form.bairro,
        telefone: form.telefone,
      };
      await axios.post('/funcionario', {
        ...dados,
      });
      await queryClient.invalidateQueries({ queryKey: CHAVE_FUNCIONARIOS });
      setForm(estadoInicial);
      setSucesso('Funcionário cadastrado com segurança.');
    } catch (falha) {
      const status = (falha as AxiosError).response?.status;
      setErro(
        status === 409
          ? 'Já existe um usuário com esse código Santri.'
          : 'Não foi possível cadastrar. Confira os campos e tente novamente.'
      );
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="employee-page">
      <Link className="employee-heading__back" to="/minha-conta">
        <i className="bi bi-arrow-left" />
        Voltar para minha conta
      </Link>

      <header className="employee-heading">
        <span>Área administrativa</span>
        <h1>Funcionários</h1>
        <p>Cadastre pessoas e controle quem pode acessar a consulta interna.</p>
      </header>

      <section className="employee-list-card" aria-labelledby="employee-list-title">
        <div className="employee-list-heading">
          <div>
            <span>Controle de acesso</span>
            <h2 id="employee-list-title">Funcionários cadastrados</h2>
          </div>
          <button
            className="btn btn-outline-primary"
            type="button"
            onClick={() => funcionariosQuery.refetch()}
            disabled={funcionariosQuery.isFetching}
          >
            {funcionariosQuery.isFetching ? 'Atualizando...' : 'Atualizar lista'}
          </button>
        </div>

        {funcionariosQuery.isLoading && (
          <p className="employee-list-message" role="status">Carregando funcionários...</p>
        )}

        {funcionariosQuery.isError && (
          <p className="employee-feedback employee-feedback--error" role="alert">
            Não foi possível carregar os funcionários.
          </p>
        )}

        {erroStatus && (
          <p className="employee-feedback employee-feedback--error" role="alert">
            {erroStatus}
          </p>
        )}

        {funcionariosQuery.data?.length === 0 && (
          <p className="employee-list-message">Nenhum funcionário cadastrado.</p>
        )}

        {!!funcionariosQuery.data?.length && (
          <div className="employee-table-wrap">
            <table className="employee-table">
              <thead>
                <tr>
                  <th scope="col">Nome</th>
                  <th scope="col">Código Santri</th>
                  <th scope="col">Função</th>
                  <th scope="col">Status</th>
                  <th scope="col"><span className="visually-hidden">Ação</span></th>
                </tr>
              </thead>
              <tbody>
                {funcionariosQuery.data.map((funcionario) => {
                  const alterandoEste = statusMutation.isPending
                    && statusMutation.variables?.id === funcionario.id;

                  return (
                    <tr key={funcionario.id}>
                      <td data-label="Nome">{funcionario.nomeFuncionario}</td>
                      <td data-label="Código Santri">{funcionario.codigoSantri}</td>
                      <td data-label="Função">
                        {funcionario.funcao === 'funcionario' ? 'Funcionário' : funcionario.funcao}
                      </td>
                      <td data-label="Status">
                        <span className={`employee-status employee-status--${funcionario.ativo ? 'active' : 'inactive'}`}>
                          {funcionario.ativo ? 'Ativo' : 'Inativo'}
                        </span>
                      </td>
                      <td className="employee-table__action">
                        <button
                          className={`btn btn-sm ${funcionario.ativo ? 'btn-outline-danger' : 'btn-outline-success'}`}
                          type="button"
                          disabled={statusMutation.isPending}
                          onClick={() => statusMutation.mutate({
                            id: funcionario.id,
                            ativo: !funcionario.ativo,
                          })}
                        >
                          {alterandoEste
                            ? 'Salvando...'
                            : funcionario.ativo ? 'Desativar' : 'Ativar'}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="employee-form-card">
        <div className="employee-form-heading">
          <span>Novo acesso</span>
          <h2>Cadastrar funcionário</h2>
          <p>O funcionário terá acesso somente à consulta da vitrine da loja física.</p>
        </div>
        <form className="employee-form" onSubmit={cadastrar}>
          <div className="employee-field employee-field--wide">
            <label htmlFor="employee-name">Nome completo</label>
            <input
              id="employee-name"
              value={form.nomeFuncionario}
              onChange={(event) => alterar('nomeFuncionario', event.target.value)}
              minLength={2}
              maxLength={120}
              autoComplete="name"
              required
            />
          </div>

          <div className="employee-field">
            <label htmlFor="employee-login">Código Santri</label>
            <input
              id="employee-login"
              value={form.codigoSantri}
              onChange={(event) => alterar('codigoSantri', event.target.value)}
              maxLength={50}
              pattern="[A-Za-z0-9._-]+"
              autoComplete="username"
              required
            />
            <small>Use exatamente o código cadastrado para o funcionário no Santri.</small>
          </div>

          <div className="employee-field">
            <label htmlFor="employee-password">Senha</label>
            <input
              id="employee-password"
              type="password"
              value={form.senha}
              onChange={(event) => alterar('senha', event.target.value)}
              minLength={12}
              maxLength={72}
              autoComplete="new-password"
              required
            />
            <small>Mínimo de 12 caracteres, com maiúscula, minúscula, número e símbolo.</small>
          </div>

          <div className="employee-field">
            <label htmlFor="employee-password-confirm">Confirmar senha</label>
            <input
              id="employee-password-confirm"
              type="password"
              value={form.confirmarSenha}
              onChange={(event) => alterar('confirmarSenha', event.target.value)}
              minLength={12}
              maxLength={72}
              autoComplete="new-password"
              required
            />
          </div>

          <div className="employee-field">
            <label htmlFor="employee-cpf">CPF</label>
            <input
              id="employee-cpf"
              value={form.CPF}
              onChange={(event) => alterar('CPF', apenasNumeros(event.target.value, 11))}
              inputMode="numeric"
              minLength={11}
              maxLength={11}
              required
            />
          </div>

          <div className="employee-field">
            <label htmlFor="employee-birth">Data de nascimento</label>
            <input
              id="employee-birth"
              type="date"
              value={form.dataNascimento}
              onChange={(event) => alterar('dataNascimento', event.target.value)}
            />
          </div>

          <div className="employee-field">
            <label htmlFor="employee-sex">Sexo</label>
            <select
              id="employee-sex"
              value={form.sexo}
              onChange={(event) => alterar('sexo', event.target.value)}
            >
              <option value="">Não informado</option>
              <option value="F">Feminino</option>
              <option value="M">Masculino</option>
              <option value="O">Outro</option>
            </select>
          </div>

          <div className="employee-field">
            <label htmlFor="employee-cep">CEP</label>
            <input
              id="employee-cep"
              value={form.CEP}
              onChange={(event) => alterar('CEP', apenasNumeros(event.target.value, 8))}
              inputMode="numeric"
              maxLength={8}
            />
          </div>

          <div className="employee-field">
            <label htmlFor="employee-neighborhood">Bairro</label>
            <input
              id="employee-neighborhood"
              value={form.bairro}
              onChange={(event) => alterar('bairro', event.target.value)}
              maxLength={120}
            />
          </div>

          <div className="employee-field employee-field--wide">
            <label htmlFor="employee-phone">Telefone</label>
            <input
              id="employee-phone"
              value={form.telefone}
              onChange={(event) => alterar('telefone', apenasNumeros(event.target.value, 11))}
              inputMode="tel"
              maxLength={11}
            />
          </div>

          {sucesso && (
            <p className="employee-feedback employee-feedback--success" role="status">
              {sucesso}
            </p>
          )}
          {erro && (
            <p className="employee-feedback employee-feedback--error" role="alert">
              {erro}
            </p>
          )}

          <div className="employee-form__actions">
            <button className="btn btn-primary" type="submit" disabled={enviando}>
              {enviando ? 'Cadastrando...' : 'Cadastrar funcionário'}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

export default FuncionariosAdmin;
