import { useMemo, useState } from 'react';
import { Produtos } from '../components/produtos/produtos';
import { useDadosProdutos } from '../hooks/useDadosProdutos';
import { CadastrarProdutos } from "../components/cadastros/inserirProdutos";
import { useAuth } from '../context/AuthProvider';
import "../styles/ProdutoList.css";

function ProdutoList() {
  const { data = [], isLoading, error } = useDadosProdutos();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
  const [priceDraft, setPriceDraft] = useState({ min: '', max: '' });
  const [appliedPriceRange, setAppliedPriceRange] = useState<{ min: number | null; max: number | null }>({
    min: null,
    max: null,
  });
  const [sortBy, setSortBy] = useState('featured');

  const { isAuthenticated, logout } = useAuth();

  const handleOpenModal = () => {
    setIsModalOpen((prev) => !prev);
  };

  const categories = useMemo(() => {
    return Array.from(new Set(data.map((produto) => produto.categoria).filter(Boolean)));
  }, [data]);

  const getPrice = (price: unknown) => {
    const value = Number(String(price));
    return Number.isFinite(value) ? value : 0;
  };

  const filteredProducts = useMemo(() => {
    const normalizedCategories = selectedCategories.length > 0 ? selectedCategories : [];

    const result = data.filter((produto) => {
      const price = getPrice(produto.preco);
      const matchesCategory = normalizedCategories.length === 0 || normalizedCategories.includes(produto.categoria);
      const matchesMin = appliedPriceRange.min === null || price >= appliedPriceRange.min;
      const matchesMax = appliedPriceRange.max === null || price <= appliedPriceRange.max;

      return matchesCategory && matchesMin && matchesMax;
    });

    return [...result].sort((left, right) => {
      const leftPrice = getPrice(left.preco);
      const rightPrice = getPrice(right.preco);

      switch (sortBy) {
        case 'price-asc':
          return leftPrice - rightPrice;
        case 'price-desc':
          return rightPrice - leftPrice;
        case 'stock-desc':
          return right.qtdEstoque - left.qtdEstoque;
        case 'name-asc':
          return left.nome.localeCompare(right.nome, 'pt-BR');
        default:
          return 0;
      }
    });
  }, [appliedPriceRange.max, appliedPriceRange.min, data, selectedCategories, sortBy]);

  const handleToggleCategory = (categoria: string) => {
    setSelectedCategories((prev) =>
      prev.includes(categoria) ? prev.filter((item) => item !== categoria) : [...prev, categoria]
    );
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
    setSelectedCategories([]);
    setPriceDraft({ min: '', max: '' });
    setAppliedPriceRange({ min: null, max: null });
    setSortBy('featured');
  }

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
          <p className="catalog-breadcrumb">Início / Esportes e lazer / Boias infláveis</p>
          <h1>Boias Infláveis</h1>
          <p className="catalog-description">
            Uma vitrine clean, com filtros rápidos, ordenação e cartões mais próximos da referência enviada.
          </p>
        </div>

        <div className="catalog-toolbar">
          <span className="catalog-results-count">Exibindo {filteredProducts.length} produtos</span>
          <label className="catalog-sort">
            <span>Ordenar por</span>
            <select value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
              <option value="featured">Mais vendidos</option>
              <option value="price-asc">Menor preço</option>
              <option value="price-desc">Maior preço</option>
              <option value="name-asc">Nome A-Z</option>
              <option value="stock-desc">Maior estoque</option>
            </select>
          </label>
        </div>
      </section>

      <section className="catalog-layout">
        <aside className="filter-card">
          <div className="filter-card__header">
            <span className="filter-card__eyebrow">Marca</span>
            <h2>Filtros</h2>
          </div>

          <div className="filter-group">
            <span className="filter-group__title">Categorias</span>
            <label className="filter-checkbox filter-checkbox--all">
              <input
                type="checkbox"
                checked={selectedCategories.length === 0}
                onChange={() => setSelectedCategories([])}
              />
              <span>Todas as categorias</span>
            </label>

            <div className="filter-list">
              {categories.map((categoria) => (
                <label className="filter-checkbox" key={categoria}>
                  <input
                    type="checkbox"
                    checked={selectedCategories.includes(categoria)}
                    onChange={() => handleToggleCategory(categoria)}
                  />
                  <span>{categoria}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="filter-group">
            <span className="filter-group__title">Preço</span>
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
              {selectedCategories.length > 0 || appliedPriceRange.min !== null || appliedPriceRange.max !== null
                ? 'Resultados filtrados'
                : 'Produtos em destaque'}
            </span>
            <span className="catalog-results__hint">Interface ajustada para ficar mais próxima da imagem.</span>
          </div>

          {filteredProducts.length === 0 ? (
            <div className="catalog-empty">Nenhum produto encontrado com os filtros atuais.</div>
          ) : (
            <div className="catalog-grid">
              {filteredProducts.map((dadosProdutos) => (
                <div className="catalog-grid__item" key={dadosProdutos.codProd}>
                  <Produtos
                    codProd={dadosProdutos.codProd}
                    nome={dadosProdutos.nome}
                    preco={dadosProdutos.preco}
                    qtdEstoque={dadosProdutos.qtdEstoque}
                    categoria={dadosProdutos.categoria}
                    imagemUrl={dadosProdutos.imagemUrl}
                  />
                </div>
              ))}
            </div>
          )}
        </section>
      </section>

      {isAuthenticated && (
        <div className="admin-actions">
          <button className="btn btn-primary me-2" onClick={handleOpenModal} type="button">
            Novo Cadastro
          </button>
          <button className="btn btn-secondary" onClick={logout} type="button">
            Logout
          </button>
        </div>
      )}

      {isModalOpen && <CadastrarProdutos closeModal={handleOpenModal} />}
    </div>
  );
}

export default ProdutoList;

