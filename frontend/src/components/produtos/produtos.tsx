import { ChangeEvent, useEffect, useState } from 'react';
import 'bootstrap/dist/css/bootstrap.min.css';
import './produtos.css';
import { useAuth } from '../../context/AuthProvider';
import { useAtualizarApresentacaoProduto } from '../../hooks/useAtualizarApresentacaoProduto';
import {
  IMAGEM_PRODUTO_PLACEHOLDER,
  resolverImagemProduto,
} from '../../utils/resolverImagemProduto';

interface ProdutosProps {
  codigoSantri: string;
  descricao: string;
  nomeExibidoSite: string;
  ncm: string;
  unidade: string;
  marca: string;
  codigoOriginal: string;
  quantidade: number;
  precoVenda: number;
  precoVendaIva: number;
  categoriaCodigo: string;
  categoriaNome: string;
  categoriaCaminho: string;
  imagemUrl: string | null;
  exibirNoSite: boolean;
  destaqueNaHome: boolean;
}

export function Produtos(props: ProdutosProps) {
  const { funcao } = useAuth();
  const isAdmin = funcao === 'ROLE_ADMIN';
  const atualizarApresentacao = useAtualizarApresentacaoProduto();
  const [isEditing, setIsEditing] = useState(false);
  const [nomeExibidoSite, setNomeExibidoSite] = useState(props.nomeExibidoSite);
  const [exibirNoSite, setExibirNoSite] = useState(props.exibirNoSite);
  const [destaqueNaHome, setDestaqueNaHome] = useState(props.destaqueNaHome);
  const [imagem, setImagem] = useState<File | undefined>();
  const [imagemPreview, setImagemPreview] = useState<string | null>(null);

  useEffect(() => {
    setNomeExibidoSite(props.nomeExibidoSite);
    setExibirNoSite(props.exibirNoSite);
    setDestaqueNaHome(props.destaqueNaHome);
  }, [props.destaqueNaHome, props.exibirNoSite, props.nomeExibidoSite]);

  useEffect(() => {
    if (!imagem) {
      setImagemPreview(null);
      return;
    }
    const preview = URL.createObjectURL(imagem);
    setImagemPreview(preview);
    return () => URL.revokeObjectURL(preview);
  }, [imagem]);

  const selecionarImagem = (event: ChangeEvent<HTMLInputElement>) => {
    setImagem(event.target.files?.[0]);
  };

  const cancelar = () => {
    setNomeExibidoSite(props.nomeExibidoSite);
    setExibirNoSite(props.exibirNoSite);
    setDestaqueNaHome(props.destaqueNaHome);
    setImagem(undefined);
    setIsEditing(false);
    atualizarApresentacao.reset();
  };

  const salvar = () => {
    atualizarApresentacao.mutate(
      {
        codigoSantri: props.codigoSantri,
        nomeExibidoSite,
        exibirNoSite,
        destaqueNaHome,
        imagem,
      },
      {
        onSuccess: () => {
          setImagem(undefined);
          setIsEditing(false);
        },
      }
    );
  };

  return (
    <article className={`card produto-card h-100 ${!props.exibirNoSite ? 'produto-card--oculto' : ''}`}>
      <div className="produto-image-shell">
        <img
          src={imagemPreview ?? resolverImagemProduto(props.imagemUrl)}
          className="card-img-top img-fluid produto-img"
          alt={props.nomeExibidoSite}
          onError={(event) => {
            event.currentTarget.src = IMAGEM_PRODUTO_PLACEHOLDER;
          }}
        />
        {isAdmin && (
          <span className={`produto-status ${props.exibirNoSite ? 'produto-status--visivel' : 'produto-status--oculto'}`}>
            {props.exibirNoSite ? 'Visível' : 'Oculto'}
          </span>
        )}
      </div>

      <div className="card-body">
        <h5 className="card-title">{props.nomeExibidoSite}</h5>
        <ul className="list-unstyled">
          <li><strong>Preço:</strong> R$ {Number(props.precoVenda).toFixed(2)}</li>
          <li><strong>Estoque:</strong> {props.quantidade} {props.unidade}</li>
          <li><strong>Marca:</strong> {props.marca || 'Não informada'}</li>
          <li><strong>Categoria:</strong> {props.categoriaNome}</li>
        </ul>

        {isAdmin && isEditing && (
          <div className="produto-admin-editor">
            <label className="produto-name-input">
              <span>Nome exibido no site</span>
              <input
                type="text"
                value={nomeExibidoSite}
                maxLength={500}
                onChange={(event) => setNomeExibidoSite(event.target.value)}
                disabled={atualizarApresentacao.isPending}
              />
              <small>
                Descrição no Santri: {props.descricao}. Deixe vazio para voltar ao nome do Santri.
              </small>
            </label>

            <label className="produto-image-input">
              <span>Imagem do produto</span>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={selecionarImagem}
                disabled={atualizarApresentacao.isPending}
              />
              <small>JPG, PNG ou WEBP, até 5 MB.</small>
            </label>

            <label className="produto-visibility">
              <input
                type="checkbox"
                checked={exibirNoSite}
                onChange={(event) => setExibirNoSite(event.target.checked)}
                disabled={atualizarApresentacao.isPending}
              />
              <span>Exibir este produto no site</span>
            </label>

            <label className="produto-visibility">
              <input
                type="checkbox"
                checked={destaqueNaHome}
                onChange={(event) => setDestaqueNaHome(event.target.checked)}
                disabled={atualizarApresentacao.isPending}
              />
              <span>Exibir na Seleção da Loja</span>
            </label>

            {atualizarApresentacao.error && (
              <p className="produto-admin-error">Não foi possível salvar a apresentação.</p>
            )}

            <div className="produto-admin-actions">
              <button
                className="btn btn-success"
                type="button"
                onClick={salvar}
                disabled={atualizarApresentacao.isPending}
              >
                {atualizarApresentacao.isPending ? 'Salvando...' : 'Salvar'}
              </button>
              <button
                className="btn btn-secondary"
                type="button"
                onClick={cancelar}
                disabled={atualizarApresentacao.isPending}
              >
                Cancelar
              </button>
            </div>
          </div>
        )}

        {isAdmin && !isEditing && (
          <button
            className="btn btn-primary"
            type="button"
            onClick={() => setIsEditing(true)}
          >
            <i className="bi bi-pencil-square me-1" />
            Editar apresentação
          </button>
        )}
      </div>
    </article>
  );
}
