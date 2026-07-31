import { type FormEvent, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { Link } from 'react-router-dom';
import { useTodasCategorias } from '../hooks/useTodasCategorias';
import { useVitrinesHome } from '../hooks/useVitrinesHome';
import { VitrineHome, VitrineHomeRequest } from '../interface/VitrineHome';
import './VitrinesHomeAdmin.css';

const formularioInicial: VitrineHomeRequest = {
  categoriaCodigo: '',
  titulo: '',
  descricao: '',
  ordem: 0,
  ativo: true,
};

function VitrinesHomeAdmin() {
  const queryClient = useQueryClient();
  const { data: vitrines = [], isLoading } = useVitrinesHome(true);
  const { data: categorias = [] } = useTodasCategorias();
  const [formulario, setFormulario] = useState<VitrineHomeRequest>(formularioInicial);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [mensagem, setMensagem] = useState('');

  const salvar = useMutation({
    mutationFn: async (dados: VitrineHomeRequest) => {
      if (editandoId === null) {
        return (await axios.post<VitrineHome>('/admin/vitrines-home', dados)).data;
      }
      return (await axios.put<VitrineHome>(`/admin/vitrines-home/${editandoId}`, dados)).data;
    },
    onSuccess: async () => {
      setFormulario(formularioInicial);
      setEditandoId(null);
      setMensagem('Vitrine salva com sucesso.');
      await queryClient.invalidateQueries({ queryKey: ['vitrines-home'] });
    },
    onError: () => setMensagem('Não foi possível salvar a vitrine. Verifique os dados.'),
  });

  const excluir = useMutation({
    mutationFn: async (id: number) => {
      await axios.delete(`/admin/vitrines-home/${id}`);
    },
    onSuccess: async () => {
      setMensagem('Vitrine excluída.');
      await queryClient.invalidateQueries({ queryKey: ['vitrines-home'] });
    },
    onError: () => setMensagem('Não foi possível excluir a vitrine.'),
  });

  const enviar = (event: FormEvent) => {
    event.preventDefault();
    setMensagem('');
    salvar.mutate(formulario);
  };

  const editar = (vitrine: VitrineHome) => {
    setEditandoId(vitrine.id);
    setFormulario({
      categoriaCodigo: vitrine.categoriaCodigo,
      titulo: vitrine.titulo,
      descricao: vitrine.descricao,
      ordem: vitrine.ordem,
      ativo: vitrine.ativo,
    });
    setMensagem('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const cancelar = () => {
    setEditandoId(null);
    setFormulario(formularioInicial);
    setMensagem('');
  };

  return (
    <div className="vitrine-admin-page">
      <header className="vitrine-admin-heading">
        <div>
          <span>Área administrativa</span>
          <h1>Vitrines da página inicial</h1>
          <p>
            Configure os cards por categoria. Os produtos exibidos são atualizados
            automaticamente e respeitam a visibilidade do site.
          </p>
        </div>
        <div className="vitrine-admin-heading__actions">
          <Link className="btn btn-outline-primary" to="/admin/importacao-produtos">
            Importar relação de produtos
          </Link>
          <Link className="btn btn-outline-secondary" to="/">
            Ver página inicial
          </Link>
        </div>
      </header>

      <section className="vitrine-admin-editor">
        <h2>{editandoId === null ? 'Nova vitrine' : 'Editar vitrine'}</h2>
        <form onSubmit={enviar}>
          <label>
            <span>Categoria em amostra</span>
            <select
              value={formulario.categoriaCodigo}
              onChange={(event) => setFormulario((atual) => ({
                ...atual,
                categoriaCodigo: event.target.value,
              }))}
              required
            >
              <option value="">Selecione uma categoria</option>
              {categorias.map((categoria) => (
                <option key={categoria.codigo} value={categoria.codigo}>
                  {categoria.caminho}
                </option>
              ))}
            </select>
          </label>

          <label>
            <span>Título</span>
            <input
              value={formulario.titulo}
              maxLength={180}
              onChange={(event) => setFormulario((atual) => ({
                ...atual,
                titulo: event.target.value,
              }))}
              required
            />
          </label>

          <label className="vitrine-admin-editor__wide">
            <span>Descrição</span>
            <textarea
              value={formulario.descricao}
              maxLength={1000}
              rows={4}
              onChange={(event) => setFormulario((atual) => ({
                ...atual,
                descricao: event.target.value,
              }))}
              required
            />
          </label>

          <label>
            <span>Ordem de exibição</span>
            <input
              type="number"
              min={0}
              value={formulario.ordem}
              onChange={(event) => setFormulario((atual) => ({
                ...atual,
                ordem: Number(event.target.value),
              }))}
            />
          </label>

          <label className="vitrine-admin-active">
            <input
              type="checkbox"
              checked={formulario.ativo}
              onChange={(event) => setFormulario((atual) => ({
                ...atual,
                ativo: event.target.checked,
              }))}
            />
            <span>Card ativo na página inicial</span>
          </label>

          <div className="vitrine-admin-editor__actions">
            <button className="btn btn-primary" type="submit" disabled={salvar.isPending}>
              {salvar.isPending ? 'Salvando...' : 'Salvar vitrine'}
            </button>
            {editandoId !== null && (
              <button className="btn btn-secondary" type="button" onClick={cancelar}>
                Cancelar
              </button>
            )}
          </div>
        </form>
        {mensagem && <p className="vitrine-admin-message">{mensagem}</p>}
      </section>

      <section className="vitrine-admin-list">
        <div className="vitrine-admin-list__heading">
          <h2>Cards cadastrados</h2>
          <span>{vitrines.length} no total</span>
        </div>

        {isLoading && <p>Carregando vitrines...</p>}
        {!isLoading && vitrines.length === 0 && (
          <p className="vitrine-admin-empty">Nenhuma vitrine cadastrada.</p>
        )}

        <div className="vitrine-admin-grid">
          {vitrines.map((vitrine) => (
            <article className="vitrine-admin-card" key={vitrine.id}>
              <div className="vitrine-admin-card__top">
                <span>{vitrine.categoriaNome}</span>
                <strong>{vitrine.ativo ? 'Ativo' : 'Inativo'}</strong>
              </div>
              <h3>{vitrine.titulo}</h3>
              <p>{vitrine.descricao}</p>
              <small>
                Ordem {vitrine.ordem} · {vitrine.produtos.length} produtos em amostra
              </small>
              <div className="vitrine-admin-card__actions">
                <button className="btn btn-outline-primary" type="button" onClick={() => editar(vitrine)}>
                  Editar
                </button>
                <button
                  className="btn btn-outline-danger"
                  type="button"
                  disabled={excluir.isPending}
                  onClick={() => {
                    if (window.confirm(`Excluir a vitrine “${vitrine.titulo}”?`)) {
                      excluir.mutate(vitrine.id);
                    }
                  }}
                >
                  Excluir
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}

export default VitrinesHomeAdmin;
