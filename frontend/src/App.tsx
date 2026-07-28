import { BrowserRouter as Router, Route, Routes, Link, Navigate } from 'react-router-dom';
import ProdutosList from './pages/ProdutosList';
import './App.css';
import 'bootstrap/dist/css/bootstrap.min.css';
import Login from './components/login/Login';
import Home from './pages/Home';


const menuLinks = [
  { label: 'Esportes e Lazer', to: '/produtos' },
  { label: 'Cozinha', to: '/produtos' },
  { label: 'Ferramenta e Construção', to: '/produtos' },
  { label: 'Vestuário e Acessórios', to: '/produtos' },
  { label: 'Eletrônicos', to: '/produtos' },
  { label: 'Saúde', to: '/produtos' },
  { label: 'Informática', to: '/produtos' },
  { label: 'Papelaria', to: '/produtos' },
];

function App() {


  return (
    <Router>
      <div className="site-shell">
        <div className="top-strip">
          <div className="top-strip__inner">
            <div className="top-strip__group">
              <span className="top-strip__item"><i className="bi bi-whatsapp" /> WhatsApp</span>
              <span className="top-strip__item"><i className="bi bi-telephone" /> (11) 4668-1234</span>
              <span className="top-strip__item"><i className="bi bi-envelope" /> loja@dubaimagazine.com.br</span>
            </div>
            <div className="top-strip__center">
              <span className="top-strip__item"><i className="bi bi-truck" /> Retirada na loja</span>
            </div>
            <div className="top-strip__group top-strip__group--social">
              <span className="top-strip__item"><i className="bi bi-facebook" /></span>
              <span className="top-strip__item"><i className="bi bi-instagram" /></span>
              <span className="top-strip__item"><i className="bi bi-youtube" /></span>
              <span className="top-strip__item"><i className="bi bi-tiktok" /></span>
            </div>
          </div>
        </div>

        <header className="site-header">
          <div className="site-header__inner">
            <Link className="brand-area" to="/" aria-label="Dubai Magazine">
              <span className="brand-name">Dubai</span>
              <span className="brand-subtitle">MAGAZINE</span>
            </Link>

            <div className="search-bar" role="search">
              <input type="search" placeholder="Buscar" aria-label="Buscar produtos" />
              <button type="button" aria-label="Pesquisar">
                <i className="bi bi-search" />
              </button>
            </div>

            <div className="header-actions">
              <Link className="utility-link utility-link--account" to="/login" aria-label="Minha conta e login">
                <i className="bi bi-person-circle" />
                <span>
                  <strong>Minha conta</strong>
                  <small>Cadastre-se | Fazer login</small>
                </span>
              </Link>
              <button className="utility-button" type="button" aria-label="Favoritos">
                <i className="bi bi-heart" />
              </button>

            </div>
          </div>

          <nav className="main-nav" aria-label="Categorias principais">
            <div className="main-nav__inner">
              {menuLinks.map((item) => (
                <Link key={item.label} className="main-nav__link" to={item.to}>
                  {item.label}
                </Link>
              ))}
            </div>
          </nav>
        </header>


        <main className="page-main">
          <Routes>
            {/* Rota raiz agora abre a Home com o carrossel promocional */}
            <Route path="/" element={<Home />} />
            <Route path="/produtos" element={<ProdutosList />} />
            {/* manter /inicio como alias para a home; redireciona para / */}
            <Route path="/inicio" element={<Navigate to="/" replace />} />
            <Route path="/login" element={<Login />} />
          </Routes>
        </main>

        <footer className="site-footer">
          <div className="site-footer__inner">
            <div className="footer-top">
              <div className="footer-brand">
                <Link className="brand-area" to="/" aria-label="Dubai Magazine - página inicial">
                  <span className="brand-name">Dubai</span>
                  <span className="brand-subtitle">MAGAZINE</span>
                </Link>
                <div className="footer-social" style={{ marginTop: '12px' }}>
                  <span className="footer-meta"><i className="bi bi-instagram" /> Instagram</span>
                  <span className="footer-meta"><i className="bi bi-facebook" /> Facebook</span>
                  <span className="footer-meta"><i className="bi bi-youtube" /> YouTube</span>
                  <span className="footer-meta"><i className="bi bi-tiktok" /> TikTok</span>
                </div>
              </div>

              <div className="footer-newsletter">
                <h3>Receba nossas novidades por e-mail</h3>
                <div className="footer-newsletter__fields">
                  <input type="text" placeholder="Nome completo" aria-label="Nome completo" />
                  <input type="text" placeholder="Fone/WhatsApp" aria-label="Fone ou WhatsApp" />
                  <input type="email" placeholder="E-mail" aria-label="E-mail" />
                  <button type="button">OK</button>
                </div>
              </div>

              <div className="footer-contact">
                <div className="footer-col">
                  <h4>Entre em contato</h4>
                  <p className="footer-meta"><i className="bi bi-whatsapp" /> (11) 4668-1234</p>
                  <p className="footer-meta"><i className="bi bi-telephone" /> (11) 97528-0101</p>
                  <p className="footer-meta"><i className="bi bi-envelope" /> atendimento@dubaimagazine.com.br</p>
                  <p className="footer-meta"><i className="bi bi-geo-alt" /> R. São Miguel, 1188 - Itapecerica</p>
                  <p className="footer-meta"><i className="bi bi-clock" /> Seg a Sex: 08:00 às 18:30</p>
                </div>
              </div>
            </div>

            <div className="footer-columns">
              <div className="footer-col">
                <h4>Navegação</h4>
                <div className="footer-links">
                  <Link className="footer-link" to="/produtos">Esportes e Lazer</Link>
                  <Link className="footer-link" to="/produtos">Casa e Jardinagem</Link>
                  <Link className="footer-link" to="/produtos">Ferramenta e Construção</Link>
                  <Link className="footer-link" to="/produtos">Vestuário e Acessórios</Link>
                </div>
              </div>

              <div className="footer-col">
                <h4>Institucional</h4>
                <div className="footer-links">
                  <Link className="footer-link" to="/login">Área do cliente</Link>
                  <Link className="footer-link" to="/login">Política de Privacidade</Link>
                  <Link className="footer-link" to="/login">Trocas e Devoluções</Link>
                  <Link className="footer-link" to="/login">Contato</Link>
                </div>
              </div>

            </div>
          </div>

          <div className="footer-bottom">
            <div className="footer-bottom__inner">
              <span>Dubai Magazine · Tudo que você precisa em um só lugar.</span>
              <span>© 2026 · Layout inspirado na referência enviada.</span>
            </div>
          </div>
        </footer>
      </div>
    </Router>
  );
}

export default App;
