import { type FormEvent, type ReactNode, useEffect, useState } from 'react';
import {
  BrowserRouter as Router,
  Route,
  Routes,
  Link,
  Navigate,
  useLocation,
  useNavigate,
} from 'react-router-dom';
import ProdutosList from './pages/ProdutosList';
import './App.css';
import 'bootstrap/dist/css/bootstrap.min.css';
import Login from './components/login/Login';
import Home from './pages/Home';
import ImportacaoProdutos from './pages/ImportacaoProdutos';
import VitrinesHomeAdmin from './pages/VitrinesHomeAdmin';
import VitrineLoja from './pages/VitrineLoja';
import VitrineLojaAdmin from './pages/VitrineLojaAdmin';
import MinhaConta from './pages/MinhaConta';
import FuncionariosAdmin from './pages/FuncionariosAdmin';
import { useAuth } from './context/AuthContext';
import { useCategoriasPrincipais } from './hooks/useCategoriasPrincipais';


function RotaAdmin({ children }: { children: ReactNode }) {
  const { isAuthenticated, funcao } = useAuth();

  if (!isAuthenticated || funcao !== 'ROLE_ADMIN') {
    return <Navigate to={isAuthenticated ? '/vitrine-loja' : '/admin'} replace />;
  }

  return children;
}

function RotaVitrineInterna({ children }: { children: ReactNode }) {
  const { isAuthenticated, funcao } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/admin" replace />;
  }
  if (funcao !== 'ROLE_ADMIN' && funcao !== 'ROLE_FUNCIONARIO') {
    return <Navigate to="/" replace />;
  }

  return children;
}

function AcessoInterno() {
  const { isAuthenticated, funcao } = useAuth();

  if (isAuthenticated) {
    return (
      <Navigate
        to={funcao === 'ROLE_ADMIN' ? '/minha-conta' : '/vitrine-loja'}
        replace
      />
    );
  }

  return <Login />;
}

function BarraBusca() {
  const { funcao } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [termo, setTermo] = useState('');

  useEffect(() => {
    if (location.pathname !== '/produtos') {
      setTermo('');
      return;
    }
    setTermo(new URLSearchParams(location.search).get('busca') ?? '');
  }, [location.pathname, location.search]);

  const pesquisar = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const parametros = location.pathname === '/produtos'
      ? new URLSearchParams(location.search)
      : new URLSearchParams();
    const termoNormalizado = termo.trim();

    if (termoNormalizado) {
      parametros.set('busca', termoNormalizado);
    } else {
      parametros.delete('busca');
    }

    navigate({
      pathname: '/produtos',
      search: parametros.toString(),
    });
  };

  return (
    <form className="search-bar" role="search" onSubmit={pesquisar}>
      <input
        type="search"
        placeholder={funcao === 'ROLE_ADMIN'
          ? 'Buscar por produto, código ou marca'
          : 'Buscar por produto ou marca'}
        aria-label="Buscar produtos"
        value={termo}
        onChange={(event) => setTermo(event.target.value)}
      />
      <button type="submit" aria-label="Pesquisar">
        <i className="bi bi-search" />
      </button>
    </form>
  );
}

function ConteudoAplicacao() {
  const { isAuthenticated, funcao, logout } = useAuth();
  const location = useLocation();
  const { data: categoriasPrincipais = [], isLoading: carregandoCategorias } =
    useCategoriasPrincipais(funcao || 'publico');
  const areaVitrine = location.pathname === '/vitrine-loja'
    || location.pathname.startsWith('/admin/vitrine-loja');
  const areaAcesso = location.pathname === '/admin';

  return (
      <div className={`site-shell${areaVitrine ? ' site-shell--internal' : ''}`}>
        {areaAcesso ? null : areaVitrine ? (
          <header className="internal-header">
            <div className="internal-header__inner">
              <Link className="brand-area" to="/vitrine-loja" aria-label="Dubai Magazine">
                <span className="brand-name">Dubai</span>
                <span className="brand-subtitle">MAGAZINE</span>
              </Link>
              <div className="internal-header__title">
                <span>Ambiente interno</span>
                <strong>Vitrine da loja física</strong>
              </div>
              <nav className="internal-header__actions" aria-label="Navegação interna">
                <Link to="/vitrine-loja">
                  <i className="bi bi-display" />
                  Consultar vitrine
                </Link>
                {funcao === 'ROLE_ADMIN' && (
                  <>
                    <Link to="/minha-conta">
                      <i className="bi bi-grid" />
                      Minha conta
                    </Link>
                    <Link to="/admin/vitrine-loja">
                      <i className="bi bi-sliders" />
                      Gerenciar
                    </Link>
                  </>
                )}
                <button type="button" onClick={logout}>
                  <i className="bi bi-box-arrow-right" />
                  Sair
                </button>
              </nav>
            </div>
          </header>
        ) : (
          <>
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

            <BarraBusca />

            <div className="header-actions">
              {isAuthenticated ? (
                <>
                  <Link
                    className="utility-link utility-link--account"
                    to={funcao === 'ROLE_ADMIN'
                      ? '/minha-conta'
                      : '/vitrine-loja'}
                    aria-label="Acessar minha conta"
                  >
                    <i className="bi bi-person-check-fill" />
                    <span>
                      <strong>Minha conta</strong>
                      <small>
                        {funcao === 'ROLE_ADMIN'
                          ? 'Administrador'
                          : funcao === 'ROLE_FUNCIONARIO'
                            ? 'Funcionário'
                            : 'Usuário conectado'}
                      </small>
                    </span>
                  </Link>
                  <button
                    className="utility-button utility-button--logout"
                    type="button"
                    onClick={logout}
                    aria-label="Sair da conta"
                    title="Sair da conta"
                  >
                    <i className="bi bi-box-arrow-right" />
                    <span>Sair</span>
                  </button>
                </>
              ) : (
                <button className="utility-button" type="button" aria-label="Favoritos">
                  <i className="bi bi-heart" />
                </button>
              )}

            </div>
          </div>

          <nav className="main-nav" aria-label="Categorias principais">
            <div className="main-nav__inner">
              <Link className="main-nav__link main-nav__link--all" to="/produtos">
                Todos
              </Link>
              {categoriasPrincipais.map((categoria) => (
                <Link
                  key={categoria.codigo}
                  className="main-nav__link"
                  to={`/produtos?categoria=${encodeURIComponent(categoria.codigo)}`}
                  title={categoria.caminho}
                >
                  {categoria.nome}
                </Link>
              ))}
              {carregandoCategorias && (
                <span className="main-nav__status">Carregando categorias...</span>
              )}
            </div>
          </nav>
        </header>
          </>
        )}


        <main className="page-main">
          <Routes>
            {/* Rota raiz agora abre a Home com o carrossel promocional */}
            <Route path="/" element={<Home />} />
            <Route path="/produtos" element={<ProdutosList />} />
            {/* manter /inicio como alias para a home; redireciona para / */}
            <Route path="/inicio" element={<Navigate to="/" replace />} />
            <Route path="/admin" element={<AcessoInterno />} />
            <Route path="/login" element={<Navigate to="/admin" replace />} />
            <Route
              path="/minha-conta"
              element={(
                <RotaAdmin>
                  <MinhaConta />
                </RotaAdmin>
              )}
            />
            <Route
              path="/admin/funcionarios"
              element={(
                <RotaAdmin>
                  <FuncionariosAdmin />
                </RotaAdmin>
              )}
            />
            <Route
              path="/admin/importacao-produtos"
              element={(
                <RotaAdmin>
                  <ImportacaoProdutos />
                </RotaAdmin>
              )}
            />
            <Route
              path="/admin/vitrines-home"
              element={(
                <RotaAdmin>
                  <VitrinesHomeAdmin />
                </RotaAdmin>
              )}
            />
            <Route
              path="/vitrine-loja"
              element={(
                <RotaVitrineInterna>
                  <VitrineLoja />
                </RotaVitrineInterna>
              )}
            />
            <Route
              path="/admin/vitrine-loja"
              element={(
                <RotaAdmin>
                  <VitrineLojaAdmin />
                </RotaAdmin>
              )}
            />
          </Routes>
        </main>

        {!areaVitrine && !areaAcesso && (
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
                  <Link className="footer-link" to="/produtos">Catálogo de produtos</Link>
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
        )}
      </div>
  );
}

function App() {
  return (
    <Router>
      <ConteudoAplicacao />
    </Router>
  );
}

export default App;
