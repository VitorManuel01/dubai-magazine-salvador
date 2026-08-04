import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useDadosProdutos } from '../hooks/useDadosProdutos';
import { useVitrinesHome } from '../hooks/useVitrinesHome';
import {
  IMAGEM_PRODUTO_PLACEHOLDER,
  resolverImagemProduto,
} from '../utils/resolverImagemProduto';
import DepoimentosClientes from '../components/home/DepoimentosClientes';
import { useCategoriasPrincipais } from '../hooks/useCategoriasPrincipais';



type PromoSlide = {
  codProd?: string;
  label: string;
  title: string;
  description: string;
  cta: string;
  image: string;
};

const CATEGORIAS_DESTAQUE = [
  { codigo: '034', icone: 'bi-tools' },
  { codigo: '036', icone: 'bi-plug' },
  { codigo: '042', icone: 'bi-lamp' },
  { codigo: '115', icone: 'bi-pc-display' },
  { codigo: '040', icone: 'bi-house-heart' },
  { codigo: '026', icone: 'bi-basket' },
  { codigo: '196', icone: 'bi-scooter' },
];


const Home: React.FC = () => {

  const {
    data: produtosSelecionados = [],
    isLoading: carregandoSelecionados,
    error: erroSelecionados,
  } = useDadosProdutos(undefined, 0, 'publico', undefined, true);
  const { data: vitrines = [] } = useVitrinesHome();
  const { data: categorias = [], isLoading: carregandoCategorias } =
    useCategoriasPrincipais('publico');

  const [activeSlide, setActiveSlide] = useState(0);
  const [vitrineAtiva, setVitrineAtiva] = useState(0);
  const [produtoVitrineAtivo, setProdutoVitrineAtivo] = useState(0);


  const promoSlides = useMemo<PromoSlide[]>(() => {
    // Usar placeholders estáticos para o carrossel promocional conforme solicitado
    const base = 'https://placehold.co/1366x450';
    return [
      {
        label: 'AURUM COLLECTION',
        title: 'Aurum Collection',
        description: 'Coleção exclusiva com design premium',
        cta: 'Ver coleção',
        image: `${base}?text=Banner+1&font=montserrat`,
      },
      {
        label: 'OFERTAS ESPECIAIS',
        title: 'Ofertas Imperdíveis',
        description: 'Descontos por tempo limitado',
        cta: 'Aproveitar',
        image: `${base}?text=Banner+2&font=montserrat`,
      },
      {
        label: 'LANÇAMENTOS',
        title: 'Novidades da temporada',
        description: 'Peças selecionadas com chegada recente',
        cta: 'Conhecer',
        image: `${base}?text=Banner+3&font=montserrat`,
      },
    ];
  }, []);

  useEffect(() => {
    setActiveSlide(0);
  }, [promoSlides.length]);

  useEffect(() => {
    if (promoSlides.length <= 1) return;

    const intervalId = window.setInterval(() => {
      setActiveSlide((current) => (current + 1) % promoSlides.length);
    }, 5000);

    return () => window.clearInterval(intervalId);
  }, [promoSlides.length]);

  useEffect(() => {
    setVitrineAtiva(0);
  }, [vitrines.length]);

  useEffect(() => {
    if (vitrines.length <= 1) return;
    const intervalId = window.setInterval(() => {
      setVitrineAtiva((atual) => (atual + 1) % vitrines.length);
    }, 7000);
    return () => window.clearInterval(intervalId);
  }, [vitrines.length]);

  const vitrine = vitrines[vitrineAtiva];

  useEffect(() => {
    setProdutoVitrineAtivo(0);
  }, [vitrine?.id]);

  useEffect(() => {
    const quantidadeProdutos = vitrine?.produtos.length ?? 0;
    if (quantidadeProdutos <= 1) return;

    const intervalId = window.setInterval(() => {
      setProdutoVitrineAtivo((atual) => (atual + 1) % quantidadeProdutos);
    }, 3200);

    return () => window.clearInterval(intervalId);
  }, [vitrine?.id, vitrine?.produtos.length]);

  const handlePreviousSlide = () => {
    setActiveSlide((current) => (current - 1 + promoSlides.length) % promoSlides.length);
  };

  const handleNextSlide = () => {
    setActiveSlide((current) => (current + 1) % promoSlides.length);
  };


  const featuredProducts = produtosSelecionados;
  const categoriasDestaque = CATEGORIAS_DESTAQUE.flatMap((destaque) => {
    const categoria = categorias.find((item) => item.codigo === destaque.codigo);
    return categoria ? [{ ...categoria, icone: destaque.icone }] : [];
  });
  const produtoVitrine = vitrine?.produtos.length
    ? vitrine.produtos[produtoVitrineAtivo % vitrine.produtos.length]
    : undefined;
  return (
    <div className="home-page">
      <section className="promo-carousel" aria-label="Banner promocional da página inicial">
        <button className="promo-carousel__nav promo-carousel__nav--left" type="button" onClick={handlePreviousSlide} aria-label="Banner anterior">
          <i className="bi bi-chevron-left" />
        </button>

        <div className="promo-carousel__viewport">
          {promoSlides.map((slide, index) => (
            <article
              key={`${slide.title}-${index}`}
              className={`promo-slide ${index === activeSlide ? 'promo-slide--active' : ''}`}
              aria-hidden={index !== activeSlide}
            >
              <div className="promo-slide__visual">
                <img src={slide.image} alt={slide.title} />
              </div>
            </article>
          ))}
        </div>

        <button className="promo-carousel__nav promo-carousel__nav--right" type="button" onClick={handleNextSlide} aria-label="Próximo banner">
          <i className="bi bi-chevron-right" />
        </button>

        <div className="promo-carousel__indicators" role="tablist" aria-label="Selecionar banner promocional">
          {promoSlides.map((slide, index) => (
            <button
              key={`${slide.title}-dot-${index}`}
              type="button"
              className={`promo-carousel__dot ${index === activeSlide ? 'promo-carousel__dot--active' : ''}`}
              onClick={() => setActiveSlide(index)}
              aria-label={`Ir para o banner ${index + 1}`}
              aria-pressed={index === activeSlide}
            />
          ))}
        </div>
      </section>

      <section className="store-benefits" aria-label="Diferenciais da Dubai Magazine">
        <article className="store-benefit">
          <i className="bi bi-credit-card-2-front" aria-hidden="true" />
          <div>
            <strong>Parcele suas compras</strong>
            <span>Em até 10x sem juros</span>
          </div>
        </article>
        <article className="store-benefit">
          <i className="bi bi-award" aria-hidden="true" />
          <div>
            <strong>Os melhores produtos</strong>
            <span>Você só encontra aqui</span>
          </div>
        </article>
        <Link className="store-benefit" to="/contato">
          <i className="bi bi-chat-dots" aria-hidden="true" />
          <div>
            <strong>Ficou com dúvida?</strong>
            <span>Entre em contato conosco!</span>
          </div>
        </Link>
      </section>

      <nav className="home-category-shortcuts" aria-label="Categorias em destaque">
        <div className="home-category-shortcuts__track">
          {categoriasDestaque.map((categoria) => (
            <Link
              className="home-category-shortcut"
              key={categoria.codigo}
              to={`/produtos?categoria=${encodeURIComponent(categoria.codigo)}`}
              title={`Ver produtos de ${categoria.nome}`}
            >
              <span className="home-category-shortcut__icon" aria-hidden="true">
                <i className={`bi ${categoria.icone}`} />
              </span>
              <span className="home-category-shortcut__name">{categoria.nome}</span>
            </Link>
          ))}
          {carregandoCategorias && Array.from({ length: 7 }, (_, indice) => (
            <span className="home-category-shortcut home-category-shortcut--loading" key={indice}>
              <span className="home-category-shortcut__icon" />
              <span className="home-category-shortcut__name">Carregando...</span>
            </span>
          ))}
        </div>
      </nav>

      {vitrine && (
        <section className="catalog-highlight" aria-live="polite" key={vitrine.id}>
          <div className="catalog-highlight__content">
            <span className="catalog-highlight__eyebrow">{vitrine.categoriaNome}</span>
            <h1>{vitrine.titulo}</h1>
            <p>{vitrine.descricao}</p>
            <div className="hero-actions">
              <Link
                className="btn btn-light"
                to={`/produtos?categoria=${encodeURIComponent(vitrine.categoriaCodigo)}`}
              >
                Ver categoria
              </Link>
            </div>
          </div>

          <div className="catalog-highlight__visual">
            {produtoVitrine ? (
              <div className="catalog-highlight-products">
                <article
                  className="catalog-highlight-product"
                  key={`${vitrine.id}-${produtoVitrine.categoriaCodigo}-${produtoVitrine.nomeExibidoSite}`}
                >
                  <img
                    src={resolverImagemProduto(produtoVitrine.imagemUrl)}
                    alt={produtoVitrine.nomeExibidoSite}
                    onError={(event) => {
                      event.currentTarget.src = IMAGEM_PRODUTO_PLACEHOLDER;
                    }}
                  />
                  <div>
                    <h2>{produtoVitrine.nomeExibidoSite}</h2>
                    <p>R$ {produtoVitrine.precoComIpi.toFixed(2)}</p>
                  </div>
                </article>
              </div>
            ) : (
              <div className="catalog-highlight-card catalog-highlight-card--empty">
                Nenhum produto visível nesta categoria.
              </div>
            )}
          </div>
        </section>
      )}

      <section className="home-section">
        <div className="home-section__header">
          <div>
            <span className="home-section__eyebrow">Produtos populares</span>
            <h2>Seleção da loja</h2>
          </div>
          <Link to="/produtos" className="home-section__link">
            Ver todos
          </Link>
        </div>

        <div className="home-grid">
          {carregandoSelecionados && (
            <p className="home-selection-status">Carregando seleção...</p>
          )}
          {erroSelecionados && (
            <p className="home-selection-status">Não foi possível carregar a seleção.</p>
          )}
          {!carregandoSelecionados && !erroSelecionados && featuredProducts.length === 0 && (
            <p className="home-selection-status">A seleção da loja está sendo preparada.</p>
          )}
          {featuredProducts.map((produto, index) => (
            <article
              className="mini-product-card"
              key={`${produto.categoriaCodigo}-${produto.nomeExibidoSite}-${index}`}
            >
              <img
                src={resolverImagemProduto(produto.imagemUrl)}
                alt={produto.nomeExibidoSite}
                onError={(event) => {
                  event.currentTarget.src = IMAGEM_PRODUTO_PLACEHOLDER;
                }}
              />
              <div className="mini-product-card__body">
                <h3>{produto.nomeExibidoSite}</h3>
                <p>R$ {produto.precoComIpi.toFixed(2)}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <DepoimentosClientes />

      <section className="instagram-callout" aria-label="Instagram da Dubai Magazine">
        <i className="bi bi-instagram" aria-hidden="true" />
        <div>
          <span>Siga-nos no Instagram</span>
          <a href="https://www.instagram.com/dubai.magazine/" target="_blank" rel="noreferrer">@dubai.magazine</a>
        </div>
      </section>
    </div>
  );
};

export default Home;
