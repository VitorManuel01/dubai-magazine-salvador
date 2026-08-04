import { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';

type Depoimento = {
  nome: string;
  texto: string;
};

const CHAVE_DEPOIMENTOS = 'dubai-magazine:depoimentos-home';

const DEPOIMENTOS_INICIAIS: Depoimento[] = [
  { nome: 'Cliente Dubai Magazine', texto: 'Ótimo atendimento e variedade de produtos.' },
  { nome: 'Cliente Dubai Magazine', texto: 'Equipe atenciosa e pronta para ajudar.' },
  { nome: 'Cliente Dubai Magazine', texto: 'Uma experiência de compra prática e agradável.' },
];

function carregarDepoimentos(): Depoimento[] {
  try {
    const armazenados = localStorage.getItem(CHAVE_DEPOIMENTOS);
    if (!armazenados) return DEPOIMENTOS_INICIAIS;

    const dados = JSON.parse(armazenados) as unknown;
    if (!Array.isArray(dados) || dados.length !== 3) return DEPOIMENTOS_INICIAIS;

    return dados.map((item) => {
      const depoimento = item as Partial<Depoimento>;
      return {
        nome: String(depoimento.nome ?? '').slice(0, 80),
        texto: String(depoimento.texto ?? '').slice(0, 300),
      };
    });
  } catch {
    return DEPOIMENTOS_INICIAIS;
  }
}

export default function DepoimentosClientes() {
  const { funcao } = useAuth();
  const administrador = funcao === 'ROLE_ADMIN';
  const [depoimentos, setDepoimentos] = useState<Depoimento[]>(carregarDepoimentos);
  const [editando, setEditando] = useState(false);

  useEffect(() => {
    if (!administrador) setEditando(false);
  }, [administrador]);

  const atualizar = (indice: number, campo: keyof Depoimento, valor: string) => {
    const limite = campo === 'nome' ? 80 : 300;
    setDepoimentos((atuais) => atuais.map((item, itemIndice) => (
      itemIndice === indice ? { ...item, [campo]: valor.slice(0, limite) } : item
    )));
  };

  const salvar = () => {
    localStorage.setItem(CHAVE_DEPOIMENTOS, JSON.stringify(depoimentos));
    setEditando(false);
  };

  const cancelar = () => {
    setDepoimentos(carregarDepoimentos());
    setEditando(false);
  };

  return (
    <section className="testimonials-section" aria-labelledby="titulo-depoimentos">
      <div className="testimonials-section__header">
        <div>
          <span className="home-section__eyebrow">Experiências na loja</span>
          <h2 id="titulo-depoimentos">O que dizem nossos clientes</h2>
        </div>
        {administrador && !editando && (
          <button type="button" className="testimonials-edit" onClick={() => setEditando(true)}>
            <i className="bi bi-pencil" /> Editar depoimentos
          </button>
        )}
      </div>

      {administrador && editando && (
        <p className="testimonials-local-note">
          Estes textos ficam salvos somente neste navegador e não são enviados ao banco.
        </p>
      )}

      <div className="testimonials-grid">
        {depoimentos.map((depoimento, indice) => (
          <article className="testimonial-card" key={indice}>
            <i className="bi bi-quote testimonial-card__quote" aria-hidden="true" />
            {editando ? (
              <>
                <textarea
                  value={depoimento.texto}
                  maxLength={300}
                  aria-label={`Depoimento ${indice + 1}`}
                  onChange={(event) => atualizar(indice, 'texto', event.target.value)}
                />
                <input
                  value={depoimento.nome}
                  maxLength={80}
                  aria-label={`Nome do cliente ${indice + 1}`}
                  onChange={(event) => atualizar(indice, 'nome', event.target.value)}
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
          <button type="button" onClick={salvar}>Salvar</button>
          <button type="button" className="testimonials-actions__secondary" onClick={cancelar}>
            Cancelar
          </button>
        </div>
      )}
    </section>
  );
}
