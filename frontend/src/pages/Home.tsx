import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useDadosProdutos } from '../hooks/useDadosProdutos';



type PromoSlide = {
  codProd?: string;
  label: string;
  title: string;
  description: string;
  cta: string;
  image: string;
};


const Home: React.FC = () => {

  const { data: produtos = [], isLoading, error } = useDadosProdutos();

  const [activeSlide, setActiveSlide] = useState(0);


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
  }, [produtos]);

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

  const handlePreviousSlide = () => {
    setActiveSlide((current) => (current - 1 + promoSlides.length) % promoSlides.length);
  };

  const handleNextSlide = () => {
    setActiveSlide((current) => (current + 1) % promoSlides.length);
  };


  const featuredProducts = produtos.slice(0, 4);
  const spotlight = produtos[0];
  return (
    <div className="home-page">
      <section className="promo-carousel" aria-label="Banner promocional da página inicial">
        <button className="promo-carousel__nav promo-carousel__nav--left" type="button" onClick={handlePreviousSlide} aria-label="Banner anterior">
          <i className="bi bi-chevron-left" />
        </button>

        <div className="promo-carousel__viewport">
          <div className="promo-ribbon" aria-hidden="true">
            <div className="promo-ribbon__inner">
              <span className="ribbon-brand">Dubai</span>
              <span className="ribbon-title">AURUM COLLECTION</span>
            </div>
          </div>
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

      <section className="catalog-highlight">
        <div className="catalog-highlight__content">
          <span className="catalog-highlight__eyebrow">{spotlight ? 'Boias infláveis · verão 2026' : 'Catálogo em destaque'}</span>
          <h1>Catálogo limpo, azul forte e foco total no produto</h1>
          <p>
            Ajustamos o frontend para ficar muito mais próximo do layout da referência: barra superior,
            navegação fina, banner em carrossel e catálogo em destaque logo abaixo.
          </p>
          <div className="hero-actions">
            <Link className="btn btn-light" to="/produtos">
              Ver catálogo
            </Link>

          </div>
          {error && <p className="catalog-highlight__error">Erro ao carregar produtos: {error.message}</p>}
          {isLoading && <p className="catalog-highlight__loading">Carregando catálogo...</p>}
        </div>

        <div className="catalog-highlight__visual">
          {spotlight ? (
            <article className="catalog-highlight-card">
              <img src={spotlight.imagemUrl} alt={spotlight.nome} />
              <div>
                <h2>{spotlight.nome}</h2>
                <p>R$ {spotlight.preco.toFixed(2)}</p>
                <span>{spotlight.qtdEstoque} em estoque</span>
              </div>
            </article>
          ) : (
            <div className="catalog-highlight-card catalog-highlight-card--empty">Catálogo em destaque</div>
          )}
        </div>
      </section>

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
          {featuredProducts.map((produto) => (
            <article className="mini-product-card" key={produto.codProd}>
              <img src={produto.imagemUrl} alt={produto.nome} />
              <div className="mini-product-card__body">
                <h3>{produto.nome}</h3>
                <p>R$ {produto.preco.toFixed(2)}</p>
                <small>{produto.qtdEstoque} em estoque</small>

              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
};

export default Home;
