import { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import {
  useAtualizarDepoimentosHome,
  useDepoimentosHome,
} from '../../hooks/useDepoimentosHome';
import { DepoimentoHome } from '../../interface/DepoimentoHome';

export default function DepoimentosClientes() {
  const { funcao } = useAuth();
  const administrador = funcao === 'ROLE_ADMIN';
  const { data: depoimentos = [], isLoading, isError } = useDepoimentosHome();
  const atualizarDepoimentos = useAtualizarDepoimentosHome();
  const [rascunho, setRascunho] = useState<DepoimentoHome[]>([]);
  const [editando, setEditando] = useState(false);

  useEffect(() => {
    if (!editando) setRascunho(depoimentos);
  }, [depoimentos, editando]);

  useEffect(() => {
    if (!administrador) setEditando(false);
  }, [administrador]);

  const iniciarEdicao = () => {
    setRascunho(depoimentos.map((depoimento) => ({ ...depoimento })));
    setEditando(true);
    atualizarDepoimentos.reset();
  };

  const atualizar = (
    posicao: number,
    campo: 'nome' | 'texto',
    valor: string
  ) => {
    const limite = campo === 'nome' ? 80 : 300;
    setRascunho((atuais) => atuais.map((item) => (
      item.posicao === posicao
        ? { ...item, [campo]: valor.slice(0, limite) }
        : item
    )));
  };

  const salvar = () => {
    atualizarDepoimentos.mutate(rascunho, {
      onSuccess: () => setEditando(false),
    });
  };

  const cancelar = () => {
    setRascunho(depoimentos);
    setEditando(false);
    atualizarDepoimentos.reset();
  };

  const exibidos = editando ? rascunho : depoimentos;
  const possuiCamposVazios = rascunho.some(
    (depoimento) => !depoimento.nome.trim() || !depoimento.texto.trim()
  );

  return (
    <section className="testimonials-section" aria-labelledby="titulo-depoimentos">
      <div className="testimonials-section__header">
        <div>
          <span className="home-section__eyebrow">Experiências na loja</span>
          <h2 id="titulo-depoimentos">O que dizem nossos clientes</h2>
        </div>
        {administrador && !editando && depoimentos.length === 3 && (
          <button type="button" className="testimonials-edit" onClick={iniciarEdicao}>
            <i className="bi bi-pencil" /> Editar depoimentos
          </button>
        )}
      </div>

      {isLoading && <p className="testimonials-local-note">Carregando depoimentos...</p>}
      {isError && (
        <p className="testimonials-local-note" role="alert">
          Não foi possível carregar os depoimentos.
        </p>
      )}

      <div className="testimonials-grid">
        {exibidos.map((depoimento) => (
          <article className="testimonial-card" key={depoimento.posicao}>
            <i className="bi bi-quote testimonial-card__quote" aria-hidden="true" />
            {editando ? (
              <>
                <textarea
                  value={depoimento.texto}
                  maxLength={300}
                  required
                  aria-label={`Depoimento ${depoimento.posicao}`}
                  onChange={(event) => atualizar(
                    depoimento.posicao,
                    'texto',
                    event.target.value
                  )}
                />
                <input
                  value={depoimento.nome}
                  maxLength={80}
                  required
                  aria-label={`Nome do cliente ${depoimento.posicao}`}
                  onChange={(event) => atualizar(
                    depoimento.posicao,
                    'nome',
                    event.target.value
                  )}
                />
              </>
            ) : (
              <>
                <p>“{depoimento.texto}”</p>
                <strong>{depoimento.nome}</strong>
              </>
            )}
          </article>
        ))}
      </div>

      {editando && (
        <div className="testimonials-actions">
          <button
            type="button"
            disabled={atualizarDepoimentos.isPending || possuiCamposVazios}
            onClick={salvar}
          >
            {atualizarDepoimentos.isPending ? 'Salvando...' : 'Salvar'}
          </button>
          <button
            type="button"
            className="testimonials-actions__secondary"
            disabled={atualizarDepoimentos.isPending}
            onClick={cancelar}
          >
            Cancelar
          </button>
          {atualizarDepoimentos.isError && (
            <span className="testimonials-local-note" role="alert">
              Não foi possível salvar os depoimentos. Tente novamente.
            </span>
          )}
        </div>
      )}
    </section>
  );
}
