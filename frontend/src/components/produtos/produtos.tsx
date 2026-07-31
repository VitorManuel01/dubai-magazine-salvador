import { ChangeEvent, useEffect, useState } from 'react';
import 'bootstrap/dist/css/bootstrap.min.css';
import './produtos.css';
import { useAuth } from '../../context/AuthContext';
import { useAtualizarApresentacaoProduto } from '../../hooks/useAtualizarApresentacaoProduto';
import {
  ehProdutoAdministrativo,
  ProdutoCatalogo,
} from '../../interface/DadosProdutos';
import {
  IMAGEM_PRODUTO_PLACEHOLDER,
  resolverImagemProduto,
} from '../../utils/resolverImagemProduto';
import { validarImagemProduto } from '../../utils/validacaoArquivos';

export function Produtos(props: ProdutoCatalogo) {
  const { funcao } = useAuth();
  const isAdmin = funcao === 'ROLE_ADMIN';
  const produtoAdministrativo = isAdmin && ehProdutoAdministrativo(props)
    ? props
    : null;
  const atualizarApresentacao = useAtualizarApresentacaoProduto();
  const [isEditing, setIsEditing] = useState(false);
  const [nomeExibidoSite, setNomeExibidoSite] = useState(props.nomeExibidoSite);
  const [exibirNoSite, setExibirNoSite] = useState(
    produtoAdministrativo?.exibirNoSite ?? true
  );
  const [destaqueNaHome, setDestaqueNaHome] = useState(
    produtoAdministrativo?.destaqueNaHome ?? false
  );
  const [imagem, setImagem] = useState<File | undefined>();
  const [imagemPreview, setImagemPreview] = useState<string | null>(null);
  const [erroImagem, setErroImagem] = useState('');

  useEffect(() => {
    setNomeExibidoSite(props.nomeExibidoSite);
    setExibirNoSite(produtoAdministrativo?.exibirNoSite ?? true);
    setDestaqueNaHome(produtoAdministrativo?.destaqueNaHome ?? false);
  }, [
    produtoAdministrativo?.destaqueNaHome,
    produtoAdministrativo?.exibirNoSite,
    props.nomeExibidoSite,
  ]);

  useEffect(() => {
    if (!imagem) {
      setImagemPreview(null);
      return;
    }
    const preview = URL.createObjectURL(imagem);
    setImagemPreview(preview);
    return () => URL.revokeObjectURL(preview);
  }, [imagem]);

  const selecionarImagem = async (event: ChangeEvent<HTMLInputElement>) => {
    const selecionada = event.target.files?.[0];
    setErroImagem('');
    if (!selecionada) {
      setImagem(undefined);
      return;
    }
    const erroValidacao = await validarImagemProduto(selecionada);
    if (erroValidacao) {
      setImagem(undefined);
      setErroImagem(erroValidacao);
      event.target.value = '';
      return;
    }
    setImagem(selecionada);
  };

  const cancelar = () => {
    setNomeExibidoSite(props.nomeExibidoSite);
    setExibirNoSite(produtoAdministrativo?.exibirNoSite ?? true);
    setDestaqueNaHome(produtoAdministrativo?.destaqueNaHome ?? false);
    setImagem(undefined);
    setErroImagem('');
    setIsEditing(false);
    atualizarApresentacao.reset();
  };

  const salvar = () => {
    if (!produtoAdministrativo) return;
    atualizarApresentacao.mutate(
      {
        codigoSantri: produtoAdministrativo.codigoSantri,
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
    <article className={`card produto-card h-100 ${produtoAdministrativo && !produtoAdministrativo.exibirNoSite ? 'produto-card--oculto' : ''}`}>
      <div className="produto-image-shell">
        <img
          src={imagemPreview ?? resolverImagemProduto(props.imagemUrl)}
          className="card-img-top img-fluid produto-img"
          alt={props.nomeExibidoSite}
          onError={(event) => {
            event.currentTarget.src = IMAGEM_PRODUTO_PLACEHOLDER;
          }}
        />
        {produtoAdministrativo && (
          <span className={`produto-status ${produtoAdministrativo.exibirNoSite ? 'produto-status--visivel' : 'produto-status--oculto'}`}>
            {produtoAdministrativo.exibirNoSite ? 'Visível' : 'Oculto'}
          </span>
        )}
      </div>

      <div className="card-body">
        <h5 className="card-title">{props.nomeExibidoSite}</h5>
        <ul className="list-unstyled">
          <li><strong>Preço:</strong> R$ {Number(props.precoComIpi).toFixed(2)}</li>
          {produtoAdministrativo && (
            <li><strong>Estoque:</strong> {produtoAdministrativo.estoque} {produtoAdministrativo.unidadeVenda}</li>
          )}
          <li><strong>Marca:</strong> {props.marca || 'Não informada'}</li>
          <li><strong>Categoria:</strong> {props.categoriaNome}</li>
        </ul>

        {produtoAdministrativo && isEditing && (
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
                Nome no Santri: {produtoAdministrativo.nome}. Deixe vazio para voltar ao nome do Santri.
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
              {erroImagem && <small className="produto-admin-error">{erroImagem}</small>}
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

        {produtoAdministrativo && !isEditing && (
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
