import { type FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios, { AxiosError } from 'axios';
import { Link } from 'react-router-dom';
import {
  type PaginaProdutosCandidatosVitrineLoja,
  type PaginaVitrinesLoja,
  type VitrineLoja,
  type VitrineLojaRequest,
} from '../interface/VitrineLoja';
import './VitrineLojaAdmin.css';

interface SecaoFormulario {
  titulo: string;
  conteudo: string;
  ordem: number;
}

interface OpcaoFormulario {
  produtoCodigoSantri: string;
  rotuloOpcao: string;
  ordem: number;
  imagensTexto: string;
  secoes: SecaoFormulario[];
}

interface VitrineFormulario {
  ativo: boolean;
  opcoes: OpcaoFormulario[];
}

const novaSecao = (ordem = 0): SecaoFormulario => ({
  titulo: '',
  conteudo: '',
  ordem,
});

const novaOpcao = (ordem = 0): OpcaoFormulario => ({
  produtoCodigoSantri: '',
  rotuloOpcao: '',
  ordem,
  imagensTexto: '',
  secoes: [novaSecao()],
});

const formularioInicial = (): VitrineFormulario => ({
  ativo: true,
  opcoes: [novaOpcao()],
});

function mensagemDaFalha(erro: unknown, padrao: string) {
  const dados = (erro as AxiosError<{ detail?: string; message?: string }>).response?.data;
  return dados?.detail || dados?.message || padrao;
}

function VitrineLojaAdmin() {
  const queryClient = useQueryClient();
  const [formulario, setFormulario] = useState<VitrineFormulario>(formularioInicial);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [buscaProduto, setBuscaProduto] = useState('');
  const [paginaProdutos, setPaginaProdutos] = useState(0);
  const [mensagem, setMensagem] = useState('');
  const [mensagemErro, setMensagemErro] = useState('');

  const vitrinesQuery = useQuery({
    queryKey: ['admin-vitrine-loja'],
    queryFn: async () => (
      await axios.get<PaginaVitrinesLoja>('/admin/vitrine-loja', {
        params: { pagina: 0, tamanho: 50 },
      })
    ).data,
  });

  const produtosQuery = useQuery({
    queryKey: ['produtos-para-vitrine', buscaProduto, paginaProdutos],
    queryFn: async () => (
      await axios.get<PaginaProdutosCandidatosVitrineLoja>(
        '/admin/vitrine-loja/produtos',
        {
        params: {
          ...(buscaProduto.trim() ? { busca: buscaProduto.trim() } : {}),
          pagina: paginaProdutos,
          tamanho: 60,
        },
        }
      )
    ).data,
  });

  const salvar = useMutation({
    mutationFn: async (dados: VitrineLojaRequest) => {
      if (editandoId === null) {
        return (await axios.post<VitrineLoja>('/admin/vitrine-loja', dados)).data;
      }
      return (await axios.put<VitrineLoja>(
        `/admin/vitrine-loja/${editandoId}`,
        dados
      )).data;
    },
    onSuccess: async () => {
      setFormulario(formularioInicial());
      setEditandoId(null);
      setMensagemErro('');
      setMensagem('Vitrine salva com sucesso.');
      await queryClient.invalidateQueries({ queryKey: ['admin-vitrine-loja'] });
      await queryClient.invalidateQueries({ queryKey: ['vitrine-loja'] });
    },
    onError: (erro) => {
      setMensagem('');
      setMensagemErro(mensagemDaFalha(
        erro,
        'Não foi possível salvar. Verifique os produtos, imagens e seções.'
      ));
    },
  });

  const excluir = useMutation({
    mutationFn: async (id: number) => {
      await axios.delete(`/admin/vitrine-loja/${id}`);
    },
    onSuccess: async () => {
      setMensagemErro('');
      setMensagem('Vitrine excluída.');
      await queryClient.invalidateQueries({ queryKey: ['admin-vitrine-loja'] });
      await queryClient.invalidateQueries({ queryKey: ['vitrine-loja'] });
    },
    onError: (erro) => {
      setMensagem('');
      setMensagemErro(mensagemDaFalha(erro, 'Não foi possível excluir a vitrine.'));
    },
  });

  const atualizarOpcao = (
    indice: number,
    campo: keyof Omit<OpcaoFormulario, 'secoes'>,
    valor: string | number
  ) => {
    setFormulario((atual) => ({
      ...atual,
      opcoes: atual.opcoes.map((opcao, posicao) => (
        posicao === indice ? { ...opcao, [campo]: valor } : opcao
      )),
    }));
  };

  const atualizarSecao = (
    indiceOpcao: number,
    indiceSecao: number,
    campo: keyof SecaoFormulario,
    valor: string | number
  ) => {
    setFormulario((atual) => ({
      ...atual,
      opcoes: atual.opcoes.map((opcao, posicaoOpcao) => {
        if (posicaoOpcao !== indiceOpcao) return opcao;
        return {
          ...opcao,
          secoes: opcao.secoes.map((secao, posicaoSecao) => (
            posicaoSecao === indiceSecao
              ? { ...secao, [campo]: valor }
              : secao
          )),
        };
      }),
    }));
  };

  const adicionarOpcao = () => {
    setFormulario((atual) => ({
      ...atual,
      opcoes: [...atual.opcoes, novaOpcao(atual.opcoes.length)],
    }));
  };

  const removerOpcao = (indice: number) => {
    setFormulario((atual) => ({
      ...atual,
      opcoes: atual.opcoes
        .filter((_, posicao) => posicao !== indice)
        .map((opcao, ordem) => ({ ...opcao, ordem })),
    }));
  };

  const adicionarSecao = (indiceOpcao: number) => {
    setFormulario((atual) => ({
      ...atual,
      opcoes: atual.opcoes.map((opcao, posicao) => (
        posicao === indiceOpcao
          ? { ...opcao, secoes: [...opcao.secoes, novaSecao(opcao.secoes.length)] }
          : opcao
      )),
    }));
  };

  const removerSecao = (indiceOpcao: number, indiceSecao: number) => {
    setFormulario((atual) => ({
      ...atual,
      opcoes: atual.opcoes.map((opcao, posicao) => {
        if (posicao !== indiceOpcao) return opcao;
        return {
          ...opcao,
          secoes: opcao.secoes
            .filter((_, secaoPosicao) => secaoPosicao !== indiceSecao)
            .map((secao, ordem) => ({ ...secao, ordem })),
        };
      }),
    }));
  };

  const enviar = (event: FormEvent) => {
    event.preventDefault();
    setMensagem('');
    setMensagemErro('');

    const dados: VitrineLojaRequest = {
      ativo: formulario.ativo,
      opcoes: formulario.opcoes.map((opcao, indice) => ({
        produtoCodigoSantri: opcao.produtoCodigoSantri.trim(),
        rotuloOpcao: opcao.rotuloOpcao.trim(),
        ordem: indice,
        imagens: opcao.imagensTexto
          .split(/\r?\n/)
          .map((imagem) => imagem.trim())
          .filter(Boolean),
        secoes: opcao.secoes.map((secao, ordem) => ({
          titulo: secao.titulo.trim(),
          conteudo: secao.conteudo.trim(),
          ordem,
        })),
      })),
    };
    salvar.mutate(dados);
  };

  const editar = (vitrine: VitrineLoja) => {
    setEditandoId(vitrine.id);
    setFormulario({
      ativo: vitrine.ativo,
      opcoes: vitrine.opcoes.map((opcao) => ({
        produtoCodigoSantri: opcao.produto.codigoSantri,
        rotuloOpcao: opcao.rotuloOpcao,
        ordem: opcao.ordem,
        imagensTexto: opcao.imagens.join('\n'),
        secoes: opcao.secoes.map((secao) => ({
          titulo: secao.titulo,
          conteudo: secao.conteudo,
          ordem: secao.ordem,
        })),
      })),
    });
    setMensagem('');
    setMensagemErro('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const cancelar = () => {
    setEditandoId(null);
    setFormulario(formularioInicial());
    setMensagem('');
    setMensagemErro('');
  };

  const paginaProdutosEncontrados = produtosQuery.data;
  const produtos = paginaProdutosEncontrados?.content ?? [];

  return (
    <div className="loja-admin-page">
      <header className="loja-admin-heading">
        <div>
          <span>Área administrativa</span>
          <h1>Vitrine da loja física</h1>
          <p>
            Cadastre os produtos escolhidos, suas variações, imagens e seções de
            apresentação. Os dados comerciais continuam vindo do inventário.
          </p>
        </div>
        <Link className="loja-admin-preview" to="/vitrine-loja">
          <i className="bi bi-display" />
          Ver vitrine
        </Link>
      </header>

      <section className="loja-admin-editor">
        <div className="loja-admin-section-heading">
          <div>
            <span>{editandoId === null ? 'Novo cadastro' : `Editando #${editandoId}`}</span>
            <h2>{editandoId === null ? 'Criar vitrine' : 'Atualizar vitrine'}</h2>
          </div>
          <label className="loja-admin-toggle">
            <input
              type="checkbox"
              checked={formulario.ativo}
              onChange={(event) => setFormulario((atual) => ({
                ...atual,
                ativo: event.target.checked,
              }))}
            />
            <span>Disponível para funcionários</span>
          </label>
        </div>

        <div className="loja-admin-product-search">
          <label htmlFor="busca-produto">Localizar produto do inventário</label>
          <div>
            <i className="bi bi-search" />
            <input
              id="busca-produto"
              type="search"
              value={buscaProduto}
              onChange={(event) => {
                setBuscaProduto(event.target.value);
                setPaginaProdutos(0);
              }}
              placeholder="Digite o nome, código Santri ou marca"
            />
          </div>
          <small>
            {produtosQuery.isFetching
              ? 'Procurando produtos...'
              : `Exibindo ${produtos.length} de ${
                paginaProdutosEncontrados?.totalElements ?? 0
              } produto(s), incluindo os ocultos no site`}
          </small>
          {(paginaProdutosEncontrados?.totalPages ?? 0) > 1 && (
            <div className="loja-admin-product-pagination">
              <button
                type="button"
                disabled={paginaProdutosEncontrados?.first}
                onClick={() => setPaginaProdutos((pagina) => Math.max(0, pagina - 1))}
              >
                <i className="bi bi-chevron-left" />
                Anterior
              </button>
              <span>
                Página {(paginaProdutosEncontrados?.number ?? 0) + 1} de{' '}
                {paginaProdutosEncontrados?.totalPages}
              </span>
              <button
                type="button"
                disabled={paginaProdutosEncontrados?.last}
                onClick={() => setPaginaProdutos((pagina) => pagina + 1)}
              >
                Próxima
                <i className="bi bi-chevron-right" />
              </button>
            </div>
          )}
        </div>

        <datalist id="produtos-inventario">
          {produtos.map((produto) => (
            <option key={produto.codigoSantri} value={produto.codigoSantri}>
              {produto.nomeExibidoSite} · {produto.categoriaCaminho} ·{' '}
              {produto.exibirNoSite ? 'visível no site' : 'oculto no site'}
            </option>
          ))}
        </datalist>

        <form onSubmit={enviar}>
          <div className="loja-admin-options">
            {formulario.opcoes.map((opcao, indiceOpcao) => (
              <article className="loja-admin-option" key={indiceOpcao}>
                <header>
                  <div>
                    <span>Variação {indiceOpcao + 1}</span>
                    <h3>{opcao.rotuloOpcao || 'Nova opção de produto'}</h3>
                  </div>
                  {formulario.opcoes.length > 1 && (
                    <button
                      type="button"
                      className="loja-admin-icon-button loja-admin-icon-button--danger"
                      onClick={() => removerOpcao(indiceOpcao)}
                      aria-label={`Remover variação ${indiceOpcao + 1}`}
                    >
                      <i className="bi bi-trash3" />
                    </button>
                  )}
                </header>

                <div className="loja-admin-fields">
                  <label>
                    <span>Código Santri do produto</span>
                    <input
                      list="produtos-inventario"
                      value={opcao.produtoCodigoSantri}
                      onChange={(event) => atualizarOpcao(
                        indiceOpcao,
                        'produtoCodigoSantri',
                        event.target.value
                      )}
                      placeholder="Selecione ou digite o código"
                      required
                    />
                  </label>
                  <label>
                    <span>Nome da opção</span>
                    <input
                      value={opcao.rotuloOpcao}
                      maxLength={100}
                      onChange={(event) => atualizarOpcao(
                        indiceOpcao,
                        'rotuloOpcao',
                        event.target.value
                      )}
                      placeholder="Ex.: Azul, Preto, 4 lugares"
                    />
                  </label>
                  <label className="loja-admin-field-wide">
                    <span>Imagens</span>
                    <textarea
                      value={opcao.imagensTexto}
                      rows={4}
                      onChange={(event) => atualizarOpcao(
                        indiceOpcao,
                        'imagensTexto',
                        event.target.value
                      )}
                      placeholder={'Uma URL ou caminho por linha\n/uploads/produtos/exemplo.webp'}
                    />
                    <small>
                      Informe uma imagem por linha. Se ficar vazio, será usada a imagem
                      principal do produto.
                    </small>
                  </label>
                </div>

                <div className="loja-admin-sections">
                  <div className="loja-admin-subheading">
                    <div>
                      <span>Conteúdo da página</span>
                      <h4>Seções descritivas</h4>
                    </div>
                    <button
                      type="button"
                      className="loja-admin-secondary-button"
                      onClick={() => adicionarSecao(indiceOpcao)}
                    >
                      <i className="bi bi-plus-lg" />
                      Adicionar seção
                    </button>
                  </div>

                  {opcao.secoes.length === 0 && (
                    <p className="loja-admin-inline-empty">
                      Um rascunho pode ficar sem seções, mas uma vitrine ativa precisa
                      de pelo menos uma.
                    </p>
                  )}

                  {opcao.secoes.map((secao, indiceSecao) => (
                    <div className="loja-admin-section-row" key={indiceSecao}>
                      <div className="loja-admin-section-number">
                        {String(indiceSecao + 1).padStart(2, '0')}
                      </div>
                      <label>
                        <span>Título</span>
                        <input
                          value={secao.titulo}
                          maxLength={180}
                          onChange={(event) => atualizarSecao(
                            indiceOpcao,
                            indiceSecao,
                            'titulo',
                            event.target.value
                          )}
                          placeholder="Ex.: Especificações do motor"
                          required={formulario.ativo}
                        />
                      </label>
                      <label className="loja-admin-section-content">
                        <span>Texto</span>
                        <textarea
                          value={secao.conteudo}
                          rows={4}
                          maxLength={10000}
                          onChange={(event) => atualizarSecao(
                            indiceOpcao,
                            indiceSecao,
                            'conteudo',
                            event.target.value
                          )}
                          placeholder="Informações que serão apresentadas ao cliente"
                          required={formulario.ativo}
                        />
                      </label>
                      <button
                        type="button"
                        className="loja-admin-icon-button loja-admin-icon-button--danger"
                        onClick={() => removerSecao(indiceOpcao, indiceSecao)}
                        aria-label={`Remover seção ${indiceSecao + 1}`}
                      >
                        <i className="bi bi-x-lg" />
                      </button>
                    </div>
                  ))}
                </div>
              </article>
            ))}
          </div>

          <button
            type="button"
            className="loja-admin-add-option"
            onClick={adicionarOpcao}
            disabled={formulario.opcoes.length >= 20}
          >
            <i className="bi bi-plus-circle" />
            Adicionar outra variação ou cor
          </button>

          <div className="loja-admin-form-actions">
            <button
              className="loja-admin-primary-button"
              type="submit"
              disabled={salvar.isPending}
            >
              {salvar.isPending ? 'Salvando...' : 'Salvar vitrine'}
            </button>
            {editandoId !== null && (
              <button
                className="loja-admin-secondary-button"
                type="button"
                onClick={cancelar}
              >
                Cancelar edição
              </button>
            )}
          </div>
        </form>

        {mensagem && <p className="loja-admin-feedback">{mensagem}</p>}
        {mensagemErro && (
          <p className="loja-admin-feedback loja-admin-feedback--error">{mensagemErro}</p>
        )}
      </section>

      <section className="loja-admin-list">
        <div className="loja-admin-section-heading">
          <div>
            <span>Cadastros existentes</span>
            <h2>Vitrines configuradas</h2>
          </div>
          <strong>{vitrinesQuery.data?.totalElements ?? 0} no total</strong>
        </div>

        {vitrinesQuery.isLoading && <p>Carregando vitrines...</p>}
        {vitrinesQuery.error && (
          <p className="loja-admin-feedback loja-admin-feedback--error">
            Não foi possível carregar os cadastros.
          </p>
        )}
        {!vitrinesQuery.isLoading && vitrinesQuery.data?.content.length === 0 && (
          <p className="loja-admin-empty">Nenhuma vitrine cadastrada.</p>
        )}

        <div className="loja-admin-list-grid">
          {vitrinesQuery.data?.content.map((vitrine) => {
            const primeiraOpcao = vitrine.opcoes[0];
            return (
              <article key={vitrine.id}>
                <div className="loja-admin-list-card__top">
                  <span className={vitrine.ativo ? 'is-active' : ''}>
                    {vitrine.ativo ? 'Ativa' : 'Rascunho'}
                  </span>
                  <small>#{vitrine.id}</small>
                </div>
                <h3>{primeiraOpcao?.produto.nomeExibidoSite ?? 'Sem produto'}</h3>
                <p>
                  {vitrine.opcoes.map((opcao) => opcao.rotuloOpcao).join(' · ')}
                </p>
                <small>
                  {vitrine.opcoes.length} variação(ões) ·{' '}
                  {vitrine.opcoes.reduce(
                    (total, opcao) => total + opcao.secoes.length,
                    0
                  )} seção(ões)
                </small>
                <div className="loja-admin-list-card__actions">
                  <button type="button" onClick={() => editar(vitrine)}>
                    <i className="bi bi-pencil" />
                    Editar
                  </button>
                  <button
                    type="button"
                    className="is-danger"
                    disabled={excluir.isPending}
                    onClick={() => {
                      const nome = primeiraOpcao?.produto.nomeExibidoSite ?? `#${vitrine.id}`;
                      if (window.confirm(`Excluir a vitrine “${nome}”?`)) {
                        excluir.mutate(vitrine.id);
                      }
                    }}
                  >
                    <i className="bi bi-trash3" />
                    Excluir
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      </section>
    </div>
  );
}

export default VitrineLojaAdmin;
