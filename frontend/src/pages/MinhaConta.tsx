import { Link } from 'react-router-dom';
import './MinhaConta.css';

const opcoes = [
  {
    rota: '/admin/importacao-produtos',
    icone: 'bi-file-earmark-spreadsheet',
    titulo: 'Importar relação de produtos',
    descricao: 'Atualize o catálogo a partir do relatório ODS gerado no Santri.',
  },
  {
    rota: '/admin/vitrines-home',
    icone: 'bi-window-stack',
    titulo: 'Gerenciar vitrine do site',
    descricao: 'Configure os cards de categorias apresentados na página inicial.',
  },
  {
    rota: '/admin/vitrine-loja',
    icone: 'bi-display',
    titulo: 'Gerenciar vitrine da loja física',
    descricao: 'Cadastre e organize os produtos da apresentação usada na loja.',
  },
  {
    rota: '/admin/funcionarios',
    icone: 'bi-person-badge',
    titulo: 'Cadastrar funcionário',
    descricao: 'Crie um acesso restrito à consulta da vitrine da loja física.',
  },
];

function MinhaConta() {
  return (
    <div className="account-page">
      <header className="account-heading">
        <span>Área administrativa</span>
        <h1>Minha conta</h1>
        <p>Escolha o que deseja gerenciar.</p>
      </header>

      <section className="account-grid" aria-label="Opções administrativas">
        {opcoes.map((opcao) => (
          <Link className="account-card" to={opcao.rota} key={opcao.rota}>
            <i className={`bi ${opcao.icone}`} aria-hidden="true" />
            <div>
              <h2>{opcao.titulo}</h2>
              <p>{opcao.descricao}</p>
            </div>
            <i className="bi bi-arrow-right" aria-hidden="true" />
          </Link>
        ))}
      </section>
    </div>
  );
}

export default MinhaConta;
