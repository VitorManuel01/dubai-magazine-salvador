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
import {
  Contato,
  PoliticaPrivacidade,
  QuemSomos,
  TrocasDevolucoes,
} from './pages/Institucional';
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
                <img className="brand-logo" src="/assets/branding/DubaiMagazine_Principal_Azul.png" alt="Dubai Magazine" />
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
              <span className="top-strip__item"><i className="bi bi-whatsapp" /> (71) 99629-3392</span>
              <a className="top-strip__item" href="tel:+557131839000"><i className="bi bi-telephone" /> (71) 3183-9000</a>
              <a className="top-strip__item" href="mailto:Contato@dubaimagazine.com.br"><i className="bi bi-envelope" /> Contato@dubaimagazine.com.br</a>
            </div>
            <div className="top-strip__center">
              <a className="top-strip__item" href="https://maps.app.goo.gl/8wzibo5BrmwbmK9H8" target="_blank" rel="noreferrer">
                <i className="bi bi-truck" /> Retirada na loja
              </a>
            </div>
            <div className="top-strip__group top-strip__group--social">
              <a className="top-strip__item" href="https://www.instagram.com/dubai.magazine/" target="_blank" rel="noreferrer" aria-label="Instagram da Dubai Magazine">
                <i className="bi bi-instagram" /> @dubai.magazine
              </a>
            </div>
          </div>
        </div>

        <header className="site-header">
          <div className="site-header__inner">
            <Link className="brand-area" to="/" aria-label="Dubai Magazine">
              <img className="brand-logo" src="/assets/branding/DubaiMagazine_Principal_Azul.png" alt="Dubai Magazine" />
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
            <Route path="/quem-somos" element={<QuemSomos />} />
            <Route path="/politica-de-privacidade" element={<PoliticaPrivacidade />} />
            <Route path="/contato" element={<Contato />} />
            <Route path="/trocas-e-devolucoes" element={<TrocasDevolucoes />} />
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
            <div className="footer-main">
              <div className="footer-brand">
                <Link className="brand-area" to="/" aria-label="Dubai Magazine - página inicial">
                  <img className="brand-logo" src="/assets/branding/DubaiMagazine_Principal_Azul.png" alt="Dubai Magazine" />
                </Link>
                <div className="footer-social" style={{ marginTop: '12px' }}>
                  <a className="footer-meta" href="https://www.instagram.com/dubai.magazine/" target="_blank" rel="noreferrer">
                    <i className="bi bi-instagram" /> @dubai.magazine
                  </a>
                </div>
              </div>

              <div className="footer-col footer-navigation">
                <h4>Navegação</h4>
                <div className="footer-links">
                  <Link className="footer-link" to="/produtos">Todos os produtos</Link>
                  {categoriasPrincipais.map((categoria) => (
                    <Link className="footer-link" key={categoria.codigo} to={`/produtos?categoria=${encodeURIComponent(categoria.codigo)}`}>
                      {categoria.nome}
                    </Link>
                  ))}
                </div>
              </div>

              <div className="footer-col footer-institutional">
                <h4>Institucional</h4>
                <div className="footer-links">
                  <Link className="footer-link" to="/quem-somos">Quem somos</Link>
                  <Link className="footer-link" to="/politica-de-privacidade">Política de privacidade</Link>
                  <Link className="footer-link" to="/contato">Contato</Link>
                  <Link className="footer-link" to="/trocas-e-devolucoes">Trocas e devoluções</Link>
                </div>
              </div>

              <div className="footer-contact">
                <div className="footer-col">
                  <h4>Entre em contato</h4>
                  <p className="footer-meta"><i className="bi bi-whatsapp" /> (71) 99629-3392</p>
                  <a className="footer-meta" href="tel:+557131839000"><i className="bi bi-telephone" /> (71) 3183-9000</a>
                  <a className="footer-meta" href="mailto:Contato@dubaimagazine.com.br"><i className="bi bi-envelope" /> Contato@dubaimagazine.com.br</a>
                  <a className="footer-meta" href="https://maps.app.goo.gl/8wzibo5BrmwbmK9H8" target="_blank" rel="noreferrer"><i className="bi bi-geo-alt" /> Rua do Uruguay, 63 - Uruguai, Salvador - BA, 40450-211</a>
                  <p className="footer-meta footer-meta--hours"><i className="bi bi-clock" /> <span>Segunda a sexta: 08:30 às 18:00<br />Sábado: 08:30 às 16:00<br />Domingo: 08:30 às 13:00</span></p>
                </div>
              </div>
            </div>
          </div>

          <div className="footer-bottom">
            <div className="footer-bottom__inner">
              <strong>Dubai Magazine</strong>
              <span>© 2026. Dubai Magazine - 11427503000407. Todos os direitos reservados.</span>
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
