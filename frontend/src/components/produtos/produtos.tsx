import { ChangeEvent, useEffect, useState } from 'react';
import 'bootstrap/dist/css/bootstrap.min.css';
import './produtos.css';
import { useAuth } from '../../context/AuthContext';
import { useAtualizarApresentacaoProduto } from '../../hooks/useAtualizarApresentacaoProduto';
import { useExcluirProduto } from '../../hooks/useExcluirProduto';
import {
  ehProdutoAdministrativo,
  ehProdutoInterno,
  ProdutoCatalogo,
} from '../../interface/DadosProdutos';
import {
  IMAGEM_PRODUTO_PLACEHOLDER,
  resolverImagemProduto,
} from '../../utils/resolverImagemProduto';
import { validarImagemProduto } from '../../utils/validacaoArquivos';

type ProdutoProps = ProdutoCatalogo & {
  limiteDestaquesAtingido?: boolean;
  carregandoLimiteDestaques?: boolean;
  selecionado?: boolean;
  onSelecionadoChange?: (codigoSantri: string, selecionado: boolean) => void;
};

const formatarMoeda = (valor: number) => new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
}).format(Number(valor));

const formatarDesconto = (valor: number) => Number(valor).toLocaleString('pt-BR', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});

export function Produtos(props: ProdutoProps) {
  const { funcao } = useAuth();
  const isAdmin = funcao === 'ROLE_ADMIN';
  const produtoInterno = ehProdutoInterno(props) ? props : null;
  const produtoAdministrativo = isAdmin && ehProdutoAdministrativo(props)
    ? props
    : null;
  const atualizarApresentacao = useAtualizarApresentacaoProduto();
  const excluirProduto = useExcluirProduto();
  const [isEditing, setIsEditing] = useState(false);
  const [nomeExibidoSite, setNomeExibidoSite] = useState(props.nomeExibidoSite);
  const [exibirNoSite, setExibirNoSite] = useState(
    produtoAdministrativo?.exibirNoSite ?? true
  );
  const [destaqueNaHome, setDestaqueNaHome] = useState(
    produtoAdministrativo?.destaqueNaHome ?? false
  );
  const [imagem, setImagem] = useState<File | undefined>();
  const [imagemHover, setImagemHover] = useState<File | undefined>();
  const [imagemPreview, setImagemPreview] = useState<string | null>(null);
  const [imagemHoverPreview, setImagemHoverPreview] = useState<string | null>(null);
  const [erroImagem, setErroImagem] = useState('');
  const destaqueOriginal = produtoAdministrativo?.destaqueNaHome ?? false;
  const disponivelParaDestaque =
    produtoAdministrativo?.disponivelUltimaImportacao ?? true;
  const precoPromocao = props.emPromocao ? props.precoPromocao : null;
  const emPromocao = precoPromocao != null;
  const bloquearNovoDestaque = !destaqueOriginal && (
    props.carregandoLimiteDestaques || props.limiteDestaquesAtingido
  );

  useEffect(() => {
    if (!exibirNoSite || !disponivelParaDestaque) setDestaqueNaHome(false);
  }, [disponivelParaDestaque, exibirNoSite]);

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

  useEffect(() => {
    if (!imagemHover) {
      setImagemHoverPreview(null);
      return;
    }
    const preview = URL.createObjectURL(imagemHover);
    setImagemHoverPreview(preview);
    return () => URL.revokeObjectURL(preview);
  }, [imagemHover]);

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

  const selecionarImagemHover = async (event: ChangeEvent<HTMLInputElement>) => {
    const selecionada = event.target.files?.[0];
    setErroImagem('');
    if (!selecionada) {
      setImagemHover(undefined);
      return;
    }
    const erroValidacao = await validarImagemProduto(selecionada);
    if (erroValidacao) {
      setImagemHover(undefined);
      setErroImagem(erroValidacao);
      event.target.value = '';
      return;
    }
    setImagemHover(selecionada);
  };

  const cancelar = () => {
    setNomeExibidoSite(props.nomeExibidoSite);
    setExibirNoSite(produtoAdministrativo?.exibirNoSite ?? true);
    setDestaqueNaHome(produtoAdministrativo?.destaqueNaHome ?? false);
    setImagem(undefined);
    setImagemHover(undefined);
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
        imagemHover,
      },
      {
        onSuccess: () => {
          setImagem(undefined);
          setImagemHover(undefined);
          setIsEditing(false);
        },
      }
    );
  };

  const excluir = () => {
    if (!produtoAdministrativo) return;
    const confirmou = window.confirm(
      `Excluir definitivamente "${produtoAdministrativo.nomeExibidoSite}"?\n\n`
      + 'O produto também será removido da vitrine da loja física e poderá ser cadastrado '
      + 'novamente em uma futura importação do Santri.',
    );
    if (confirmou) {
      excluirProduto.mutate(produtoAdministrativo.codigoSantri);
    }
  };

  return (
    <article className={`card produto-card ${produtoAdministrativo ? 'produto-card--administrativo' : ''} ${isEditing ? 'produto-card--editando' : ''} ${produtoAdministrativo && !produtoAdministrativo.exibirNoSite ? 'produto-card--oculto' : ''}`}>
      <div className="produto-image-shell">
        {produtoAdministrativo && props.onSelecionadoChange && (
          <label
            className={`produto-selecao${!produtoAdministrativo.disponivelUltimaImportacao ? ' produto-selecao--indisponivel' : ''}`}
            title={produtoAdministrativo.disponivelUltimaImportacao
              ? 'Selecionar produto'
              : 'Produto indisponível na última importação'}
          >
            <input
              type="checkbox"
              checked={props.selecionado ?? false}
              disabled={!produtoAdministrativo.disponivelUltimaImportacao}
              onChange={(event) => props.onSelecionadoChange?.(
                produtoAdministrativo.codigoSantri,
                event.target.checked,
              )}
            />
            <span>Selecionar</span>
          </label>
        )}
        <img
          src={imagemPreview ?? resolverImagemProduto(props.imagemUrl)}
          className="card-img-top img-fluid produto-img produto-img--principal"
          alt={props.nomeExibidoSite}
          onError={(event) => {
            event.currentTarget.src = IMAGEM_PRODUTO_PLACEHOLDER;
          }}
        />
        {(imagemHoverPreview || props.imagemHoverUrl) && (
          <img
            src={imagemHoverPreview ?? resolverImagemProduto(props.imagemHoverUrl)}
            className="card-img-top img-fluid produto-img produto-img--hover"
            alt={`${props.nomeExibidoSite} — segunda imagem`}
            onError={(event) => {
              event.currentTarget.style.display = 'none';
            }}
          />
        )}
        {produtoAdministrativo && (
          <span className={`produto-status ${produtoAdministrativo.exibirNoSite ? 'produto-status--visivel' : 'produto-status--oculto'}`}>
            {produtoAdministrativo.exibirNoSite ? 'Visível' : 'Oculto'}
          </span>
        )}
        {!produtoInterno && props.esgotado && (
          <span className="produto-esgotado">ESGOTADO</span>
        )}
      </div>

      <div className="card-body">
        <h5 className="card-title" title={props.nomeExibidoSite}>
          {props.nomeExibidoSite}
        </h5>
        <ul className={`list-unstyled produto-informacoes ${produtoAdministrativo ? 'produto-informacoes--administrativo' : ''} ${!produtoInterno && !emPromocao ? 'produto-informacoes--sem-promocao' : ''}`}>
          {produtoInterno && (
            <li className="produto-informacao">
              <strong>Código Santri:</strong>
              <span>{produtoInterno.codigoSantri}</span>
            </li>
          )}
          {produtoAdministrativo ? (
            <li className="produto-precos produto-precos--admin">
              <span>
                <strong>Preço de venda:</strong>{' '}
                {formatarMoeda(props.precoComIpi)}
              </span>
              {precoPromocao != null && (
                <strong className="produto-preco-promocao">
                  Preço promocional: {formatarMoeda(precoPromocao)}
                  {props.porcDesconto != null && ` (-${formatarDesconto(props.porcDesconto)}%)`}
                </strong>
              )}
            </li>
          ) : (
            <li className={`produto-precos produto-precos--catalogo ${
              !emPromocao
                ? 'produto-precos--sem-promocao'
                : ''
            }`}>
              <span className={emPromocao ? 'produto-preco-original--riscado' : 'produto-preco-original'}>
                {formatarMoeda(props.precoComIpi)}
              </span>
              {precoPromocao != null && (
                <strong className="produto-preco-promocao produto-preco-promocao--catalogo">
                  {formatarMoeda(precoPromocao)}
                  {props.porcDesconto != null && ` (-${formatarDesconto(props.porcDesconto)}%)`}
                </strong>
              )}
            </li>
          )}
          {produtoAdministrativo && (
            <li className="produto-informacao">
              <strong>Estoque:</strong>
              <span>{produtoAdministrativo.estoque} {produtoAdministrativo.unidadeVenda}</span>
            </li>
          )}
          {produtoInterno && (
            <li className="produto-meta produto-informacao">
              <strong>Marca:</strong>
              <span title={props.marca || 'Não informada'}>{props.marca || 'Não informada'}</span>
            </li>
          )}
          {produtoInterno && (
            <li className="produto-meta produto-informacao">
              <strong>Categoria:</strong>
              <span title={props.categoriaNome}>{props.categoriaNome || 'Não informada'}</span>
            </li>
          )}
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
              <span>Imagem principal do produto</span>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={selecionarImagem}
                disabled={atualizarApresentacao.isPending}
              />
              <small>JPG, PNG ou WEBP, até 5 MB.</small>
              {erroImagem && <small className="produto-admin-error">{erroImagem}</small>}
            </label>

            <label className="produto-image-input">
              <span>Imagem ao passar o mouse</span>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={selecionarImagemHover}
                disabled={atualizarApresentacao.isPending}
              />
              <small>JPG, PNG ou WEBP, até 5 MB. Ela substituirá visualmente a principal no hover.</small>
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
                disabled={
                  atualizarApresentacao.isPending
                  || bloquearNovoDestaque
                  || !exibirNoSite
                  || !disponivelParaDestaque
                }
              />
              <span>Exibir na Seleção da Loja</span>
            </label>
            {props.limiteDestaquesAtingido && !destaqueOriginal && (
              <small className="produto-admin-error">
                O limite de 3 produtos foi atingido. Desmarque um item atual antes de selecionar outro.
              </small>
            )}
            {!disponivelParaDestaque && (
              <small className="produto-admin-error">
                Produto indisponível na última importação; ele não pode aparecer na Seleção da Loja.
              </small>
            )}

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
          <div className="produto-admin-actions">
            <button
              className="btn btn-primary"
              type="button"
              onClick={() => setIsEditing(true)}
              disabled={excluirProduto.isPending}
            >
              <i className="bi bi-pencil-square me-1" />
              Editar
            </button>
            <button
              className="btn btn-danger"
              type="button"
              onClick={excluir}
              disabled={excluirProduto.isPending}
            >
              <i className="bi bi-trash me-1" />
              {excluirProduto.isPending ? 'Excluindo...' : 'Excluir'}
            </button>
          </div>
        )}
        {produtoAdministrativo && excluirProduto.isError && (
          <p className="produto-admin-error">Não foi possível excluir o produto.</p>
        )}
      </div>
    </article>
  );
}
