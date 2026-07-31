import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Produtos } from '../components/produtos/produtos';
import { useDadosProdutos } from '../hooks/useDadosProdutos';
import { useAuth } from '../context/AuthContext';
import { useCategoriasPrincipais } from '../hooks/useCategoriasPrincipais';
import { useSubcategorias } from '../hooks/useSubcategorias';
import '../styles/ProdutoList.css';

function ProdutoList() {
  const { funcao } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const categoriaCodigo = searchParams.get('categoria')?.trim() || undefined;
  const subcategoriaCodigo = searchParams.get('subcategoria')?.trim() || undefined;
  const busca = searchParams.get('busca')?.trim() || undefined;
  const categoriaFiltro = subcategoriaCodigo ?? categoriaCodigo;
  const [pagina, setPagina] = useState(0);
  const [priceDraft, setPriceDraft] = useState({ min: '', max: '' });
  const [appliedPriceRange, setAppliedPriceRange] = useState<{
    min: number | null;
    max: number | null;
  }>({ min: null, max: null });
  const [sortBy, setSortBy] = useState('featured');

  const {
    data = [],
    paginacao,
    isLoading,
    error,
  } = useDadosProdutos(categoriaFiltro, pagina, funcao || 'publico', busca);
  const { data: categoriasPrincipais = [] } =
    useCategoriasPrincipais(funcao || 'publico');
  const {
    data: subcategorias = [],
    isLoading: carregandoSubcategorias,
  } = useSubcategorias(categoriaCodigo, funcao || 'publico');

  const categoriaPrincipal = categoriasPrincipais.find(
    (categoria) => categoria.codigo === categoriaCodigo
  );
  const subcategoriaSelecionada = subcategorias.find(
    (categoria) => categoria.codigo === subcategoriaCodigo
  );
  const tituloCategoria = subcategoriaSelecionada?.nome
    ?? categoriaPrincipal?.nome
    ?? (categoriaCodigo ? `Categoria ${categoriaCodigo}` : 'Todos os produtos');
  const tituloPagina = busca
    ? `Resultados para “${busca}”`
    : tituloCategoria;

  useEffect(() => {
    setPagina(0);
    setPriceDraft({ min: '', max: '' });
    setAppliedPriceRange({ min: null, max: null });
    setSortBy('featured');
  }, [busca, categoriaCodigo, subcategoriaCodigo]);

  const getPrice = (price: unknown) => {
    const value = Number(String(price));
    return Number.isFinite(value) ? value : 0;
  };

  const filteredProducts = useMemo(() => {
    const result = data.filter((produto) => {
      const price = getPrice(produto.precoComIpi);
      const matchesMin = appliedPriceRange.min === null || price >= appliedPriceRange.min;
      const matchesMax = appliedPriceRange.max === null || price <= appliedPriceRange.max;
      return matchesMin && matchesMax;
    });

    return [...result].sort((left, right) => {
      const leftPrice = getPrice(left.precoComIpi);
      const rightPrice = getPrice(right.precoComIpi);

      switch (sortBy) {
        case 'price-asc':
          return leftPrice - rightPrice;
        case 'price-desc':
          return rightPrice - leftPrice;
        case 'stock-desc':
          return ('estoque' in right ? right.estoque : 0)
            - ('estoque' in left ? left.estoque : 0);
        case 'name-asc':
          return left.nomeExibidoSite.localeCompare(right.nomeExibidoSite, 'pt-BR');
        default:
          return 0;
      }
    });
  }, [appliedPriceRange.max, appliedPriceRange.min, data, sortBy]);

  const selecionarSubcategoria = (codigo?: string) => {
    const novosParametros = new URLSearchParams(searchParams);
    if (codigo) {
      novosParametros.set('subcategoria', codigo);
    } else {
      novosParametros.delete('subcategoria');
    }
    setSearchParams(novosParametros);
  };

  const handleApplyPrice = () => {
    const min = priceDraft.min === '' ? null : Number(priceDraft.min);
    const max = priceDraft.max === '' ? null : Number(priceDraft.max);

    setAppliedPriceRange({
      min: Number.isFinite(min ?? NaN) ? min : null,
      max: Number.isFinite(max ?? NaN) ? max : null,
    });
  };

  const handleClearFilters = () => {
    selecionarSubcategoria();
    setPriceDraft({ min: '', max: '' });
    setAppliedPriceRange({ min: null, max: null });
    setSortBy('featured');
  };

  if (isLoading) {
    return <div className="catalog-page">Carregando catálogo...</div>;
  }

  if (error) {
    return <div className="catalog-page">Erro ao carregar produtos: {(error as Error).message}</div>;
  }

  return (
    <div className="catalog-page">
      <section className="catalog-header">
        <div>
          <p className="catalog-breadcrumb">
            <Link to="/">Início</Link> / {tituloPagina}
          </p>
          <h1>{tituloPagina}</h1>
          <p className="catalog-description">
            {busca
              ? `Pesquisa em ${tituloCategoria.toLocaleLowerCase('pt-BR')}.`
              : subcategoriaSelecionada
              ? `Produtos de ${subcategoriaSelecionada.nome} e de seus grupos internos.`
              : categoriaPrincipal
                ? `Produtos de ${categoriaPrincipal.nome} e de todas as suas subcategorias.`
                : 'Explore os produtos disponíveis no catálogo da Dubai Magazine.'}
          </p>
        </div>

        <div className="catalog-toolbar">
          <span className="catalog-results-count">
            Exibindo {filteredProducts.length} de{' '}
            {(paginacao?.totalElements ?? 0).toLocaleString('pt-BR')} produtos
          </span>
          <label className="catalog-sort">
            <span>Ordenar esta página por</span>
            <select value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
              <option value="featured">Ordem do catálogo</option>
              <option value="price-asc">Menor preço</option>
              <option value="price-desc">Maior preço</option>
              <option value="name-asc">Nome A-Z</option>
              {funcao === 'ROLE_ADMIN' && (
                <option value="stock-desc">Maior estoque</option>
              )}
            </select>
          </label>
        </div>
      </section>

      <section className="catalog-layout">
        <aside className="filter-card">
          <div className="filter-card__header">
            <span className="filter-card__eyebrow">Catálogo</span>
            <h2>Filtros</h2>
          </div>

          <div className="filter-group">
            <span className="filter-group__title">Subcategorias</span>

            {!categoriaCodigo && (
              <p className="filter-empty">
                Selecione uma categoria no menu superior para ver suas subcategorias.
              </p>
            )}

            {categoriaCodigo && (
              <>
                <label className="filter-checkbox filter-checkbox--all">
                  <input
                    type="radio"
                    name="subcategoria"
                    checked={!subcategoriaCodigo}
                    onChange={() => selecionarSubcategoria()}
                  />
                  <span>Todas de {categoriaPrincipal?.nome ?? categoriaCodigo}</span>
                </label>

                <div className="filter-list">
                  {subcategorias.map((categoria) => (
                    <label className="filter-checkbox" key={categoria.codigo}>
                      <input
                        type="radio"
                        name="subcategoria"
                        checked={subcategoriaCodigo === categoria.codigo}
                        onChange={() => selecionarSubcategoria(categoria.codigo)}
                      />
                      <span>{categoria.nome}</span>
                    </label>
                  ))}
                  {carregandoSubcategorias && <span>Carregando...</span>}
                  {!carregandoSubcategorias && subcategorias.length === 0 && (
                    <p className="filter-empty">Nenhuma subcategoria cadastrada.</p>
                  )}
                </div>
              </>
            )}
          </div>

          <div className="filter-group">
            <span className="filter-group__title">Preço nesta página</span>
            <div className="price-grid">
              <input
                type="number"
                placeholder="De"
                value={priceDraft.min}
                onChange={(event) => setPriceDraft((prev) => ({ ...prev, min: event.target.value }))}
              />
              <input
                type="number"
                placeholder="Até"
                value={priceDraft.max}
                onChange={(event) => setPriceDraft((prev) => ({ ...prev, max: event.target.value }))}
              />
            </div>

            <div className="filter-actions">
              <button className="filter-button filter-button--primary" type="button" onClick={handleApplyPrice}>
                Aplicar
              </button>
              <button className="filter-button" type="button" onClick={handleClearFilters}>
                Limpar
              </button>
            </div>
          </div>
        </aside>

        <section className="catalog-results">
          <div className="catalog-results__bar">
            <span>
              {funcao === 'ROLE_ADMIN'
                ? 'Visão administrativa: produtos visíveis e ocultos'
                : 'Produtos disponíveis no site'}
            </span>
            <span className="catalog-results__hint">
              Página {(paginacao?.number ?? 0) + 1} de {Math.max(paginacao?.totalPages ?? 1, 1)}
            </span>
          </div>

          {filteredProducts.length === 0 ? (
            <div className="catalog-empty">
              {busca
                ? `Nenhum produto encontrado para “${busca}”.`
                : 'Nenhum produto encontrado com os filtros atuais.'}
            </div>
          ) : (
            <div className="catalog-grid">
              {filteredProducts.map((dadosProdutos, index) => (
                <div
                  className="catalog-grid__item"
                  key={'codigoSantri' in dadosProdutos
                    ? dadosProdutos.codigoSantri
                    : `${dadosProdutos.categoriaCodigo}-${dadosProdutos.nomeExibidoSite}-${index}`}
                >
                  <Produtos {...dadosProdutos} />
                </div>
              ))}
            </div>
          )}

          {(paginacao?.totalPages ?? 0) > 1 && (
            <nav className="catalog-pagination" aria-label="Paginação de produtos">
              <button
                type="button"
                onClick={() => setPagina((atual) => Math.max(0, atual - 1))}
                disabled={paginacao?.first}
              >
                <i className="bi bi-chevron-left" />
                Anterior
              </button>
              <span>
                Página {(paginacao?.number ?? 0) + 1} de {paginacao?.totalPages}
              </span>
              <button
                type="button"
                onClick={() => setPagina((atual) => atual + 1)}
                disabled={paginacao?.last}
              >
                Próxima
                <i className="bi bi-chevron-right" />
              </button>
            </nav>
          )}
        </section>
      </section>

      {funcao === 'ROLE_ADMIN' && (
        <div className="admin-actions">
          <Link className="btn btn-outline-primary" to="/admin/vitrine-loja">
            <i className="bi bi-display me-2" />
            Gerenciar vitrine da loja
          </Link>
          <Link className="btn btn-outline-primary" to="/admin/vitrines-home">
            <i className="bi bi-layout-text-window-reverse me-2" />
            Gerenciar vitrines
          </Link>
          <Link className="btn btn-outline-primary" to="/admin/importacao-produtos">
            <i className="bi bi-file-earmark-arrow-up me-2" />
            Importar relação de produtos
          </Link>
        </div>
      )}
    </div>
  );
}

export default ProdutoList;
