import { Link } from 'react-router-dom';

type PaginaInstitucionalProps = {
  titulo: string;
  subtitulo: string;
  children: React.ReactNode;
};

function PaginaInstitucional({ titulo, subtitulo, children }: PaginaInstitucionalProps) {
  return (
    <div className="institutional-page">
      <p className="institutional-page__breadcrumb"><Link to="/">Início</Link> / {titulo}</p>
      <header>
        <h1>{titulo}</h1>
        <p>{subtitulo}</p>
      </header>
      <div className="institutional-page__content">{children}</div>
    </div>
  );
}

export function QuemSomos() {
  return (
    <PaginaInstitucional titulo="Quem somos" subtitulo="Conheça a Dubai Magazine Salvador.">
      <p>
        Somos uma empresa comprometida em oferecer produtos de alta qualidade, com um atendimento
        atencioso e uma experiência de compra que vai além do esperado. Nossa missão é atender com
        excelência e superar suas expectativas em cada pedido.
      </p>
      <p>
        Contamos com uma loja física em <strong>Salvador</strong>, onde recebemos nossos clientes
        com todo cuidado e atenção. Além disso, temos <strong>Centros de Distribuição em São Paulo
        e Minas Gerais</strong>, o que nos permite realizar entregas com rapidez e eficiência para
        todas as regiões do Brasil.
      </p>
      <p>
        Nos principais marketplaces, somos reconhecidos como <strong>vendedores Platinum</strong>,
        com mais de <strong>200 mil pedidos entregues</strong> e milhares de clientes satisfeitos.
      </p>
      <p>
        Cada detalhe do nosso serviço é pensado para garantir praticidade, segurança e confiança
        para quem compra com a gente.
      </p>

      <section className="institutional-addresses" aria-labelledby="titulo-enderecos">
        <h2 id="titulo-enderecos">Endereços</h2>
        <div className="institutional-addresses__grid">
          <address>
            <i className="bi bi-box-seam" aria-hidden="true" />
            <div>
              <strong>Centro de Distribuição – Santana de Parnaíba – SP</strong>
              <span>Av. Tenente Marques, 6590 – Vila Poupança, Santana de Parnaíba – SP, 06525-001</span>
            </div>
          </address>
          <address>
            <i className="bi bi-shop" aria-hidden="true" />
            <div>
              <strong>Loja Física – Salvador – BA</strong>
              <a href="https://maps.app.goo.gl/8wzibo5BrmwbmK9H8" target="_blank" rel="noreferrer">
                Rua Direta do Uruguai, 63 – Uruguai, Salvador – BA, 40450-211
              </a>
            </div>
          </address>
        </div>
      </section>
    </PaginaInstitucional>
  );
}

export function PoliticaPrivacidade() {
  return (
    <PaginaInstitucional
      titulo="Política de Privacidade e Termos de Uso"
      subtitulo="Transparência, segurança e respeito no uso do catálogo da Dubai Magazine Salvador."
    >
      <p>
        A Dubai Magazine valoriza a transparência e o respeito aos seus clientes. Esta Política de
        Privacidade e estes Termos de Uso esclarecem como as informações são coletadas, utilizadas,
        armazenadas e protegidas durante a navegação no portal.
      </p>

      <section className="privacy-section">
        <h2>1. Definições</h2>
        <dl className="privacy-definitions">
          <div><dt>Dubai Magazine</dt><dd>Nome comercial da Dubai Importadora e Distribuidora Ltda., inscrita no CNPJ nº 11.427.503/0004-07, com unidade na Rua Direta do Uruguai, 63 – Uruguai, Salvador – BA, 40450-211.</dd></div>
          <div><dt>Portal</dt><dd>Catálogo eletrônico oficial da Dubai Magazine Salvador e seus futuros domínios ou subdomínios.</dd></div>
          <div><dt>Visitante</dt><dd>Qualquer pessoa que acessa ou navega pelas áreas públicas do portal.</dd></div>
          <div><dt>Usuário interno</dt><dd>Administrador ou funcionário autorizado que acessa áreas restritas por meio de login. O portal não oferece cadastro ou login para clientes.</dd></div>
          <div><dt>IP</dt><dd>Identificador numérico associado à conexão utilizada para acessar a internet.</dd></div>
          <div><dt>Logs</dt><dd>Registros técnicos de acesso, autenticação, erros e ações relevantes para a operação e segurança do portal.</dd></div>
          <div><dt>Armazenamento local</dt><dd>Recurso do navegador usado para manter o token de autenticação, um identificador técnico do dispositivo e, no ambiente administrativo, os depoimentos configurados localmente.</dd></div>
        </dl>
      </section>

      <section className="privacy-section">
        <h2>2. Coleta de informações</h2>
        <p>
          O catálogo público pode ser consultado sem cadastro. Durante a navegação, podem ser
          registrados endereço IP, data e hora, página ou recurso solicitado, informações técnicas
          do navegador e registros necessários ao diagnóstico de falhas e à proteção do sistema.
        </p>
        <p>
          Nas áreas restritas, são tratados o código interno do funcionário, credenciais de acesso,
          função autorizada e identificador técnico do dispositivo. As senhas não são armazenadas em
          texto legível. Importações de produtos e alterações administrativas também podem gerar
          registros de operação e auditoria.
        </p>
        <p>
          Quando o visitante entra em contato por e-mail, telefone, Instagram ou WhatsApp, os dados
          fornecidos passam a ser tratados também conforme as práticas e políticas do respectivo canal.
        </p>
      </section>

      <section className="privacy-section">
        <h2>3. Armazenamento e segurança</h2>
        <p>
          Os dados são mantidos em ambiente com acesso restrito a pessoas autorizadas. Adotamos
          medidas técnicas e administrativas como controle de permissões, senhas protegidas,
          autenticação por token, limitação de tentativas de login e validação de arquivos enviados.
        </p>
        <p>
          Nenhum sistema conectado à internet é totalmente livre de riscos. Eventuais incidentes
          serão tratados conforme a legislação aplicável e as responsabilidades da empresa. Os dados
          serão conservados apenas pelo tempo necessário às finalidades informadas, ao exercício de
          direitos e ao cumprimento de obrigações legais ou regulatórias.
        </p>
      </section>

      <section className="privacy-section">
        <h2>4. Uso das informações</h2>
        <p>As informações podem ser utilizadas para:</p>
        <ul>
          <li>disponibilizar e manter o catálogo;</li>
          <li>responder a dúvidas, solicitações e contatos;</li>
          <li>autenticar administradores e funcionários autorizados;</li>
          <li>prevenir fraudes, abuso, acessos indevidos e ataques;</li>
          <li>diagnosticar erros e melhorar o desempenho e a segurança;</li>
          <li>cumprir obrigações legais, regulatórias ou determinações de autoridades;</li>
          <li>proteger direitos da Dubai Magazine em processos judiciais ou administrativos.</li>
        </ul>
        <p>
          As informações pessoais não serão vendidas. O compartilhamento ocorrerá somente quando
          necessário à prestação de um serviço legítimo, à proteção do portal ou ao cumprimento de
          uma obrigação legal, respeitando finalidade, necessidade e segurança.
        </p>
      </section>

      <section className="privacy-section">
        <h2>5. Cookies e armazenamento no navegador</h2>
        <p>
          Atualmente, o portal não utiliza cookies publicitários nem web beacons próprios para
          acompanhamento comercial. As áreas restritas utilizam armazenamento local do navegador
          para manter a sessão autenticada e reconhecer tecnicamente o dispositivo para fins de
          segurança. O administrador também pode salvar três depoimentos da página inicial somente
          no navegador em que realizou a edição.
        </p>
        <p>
          A limpeza dos dados do navegador encerra a sessão e pode apagar essas configurações locais.
          Caso ferramentas de análise, publicidade ou novos cookies sejam adotados futuramente, esta
          política e, quando necessário, os mecanismos de consentimento serão atualizados.
        </p>
      </section>

      <section className="privacy-section">
        <h2>6. Direitos dos titulares</h2>
        <p>
          Nos termos da Lei Geral de Proteção de Dados Pessoais, o titular pode solicitar, quando
          aplicável, confirmação e acesso ao tratamento, correção de dados inexatos, informações
          sobre compartilhamento, anonimização, bloqueio, eliminação, portabilidade ou revogação do
          consentimento, observadas as hipóteses legais de conservação.
        </p>
        <p>
          Solicitações podem ser encaminhadas para{' '}
          <a href="mailto:Contato@dubaimagazine.com.br">Contato@dubaimagazine.com.br</a>. Poderemos
          pedir informações adicionais para confirmar a identidade do solicitante e proteger seus dados.
        </p>
      </section>

      <section className="privacy-section">
        <h2>7. Termos de uso do catálogo</h2>
        <ul>
          <li>O portal funciona como catálogo informativo e não realiza vendas ou pagamentos online.</li>
          <li>As compras são concluídas presencialmente na loja; clientes empresa devem entrar em contato pelos canais informados.</li>
          <li>Disponibilidade, estoque, imagens, características e preços podem ser atualizados ou corrigidos sem aviso prévio.</li>
          <li>É proibido tentar acessar áreas restritas sem autorização, explorar falhas, automatizar acessos abusivos ou prejudicar a disponibilidade do portal.</li>
          <li>Links para serviços externos, como Instagram e futuros links de WhatsApp, estão sujeitos às políticas dessas plataformas.</li>
        </ul>
      </section>

      <section className="privacy-section">
        <h2>8. Disposições gerais</h2>
        <p>
          Esta política pode ser atualizada para refletir mudanças no portal, nas operações da empresa
          ou na legislação. A versão vigente ficará disponível nesta página, acompanhada da data da
          última atualização.
        </p>
      </section>

      <section className="privacy-section">
        <h2>9. Lei aplicável e foro</h2>
        <p>
          Esta política é regida pelas leis brasileiras. Fica eleito o Foro da Comarca de Salvador – BA
          para questões que não possam ser resolvidas pelos canais de atendimento, ressalvadas as
          regras legais obrigatórias, inclusive o direito do consumidor de recorrer ao foro de seu domicílio.
        </p>
      </section>

      <p className="privacy-updated"><strong>Última atualização:</strong> 4 de agosto de 2026.</p>
    </PaginaInstitucional>
  );
}

export function Contato() {
  return (
    <PaginaInstitucional titulo="Contato" subtitulo="Fale com a equipe da Dubai Magazine Salvador.">
      <div className="institutional-contact-list">
        <a href="mailto:Contato@dubaimagazine.com.br"><i className="bi bi-envelope" /> Contato@dubaimagazine.com.br</a>
        <span><i className="bi bi-whatsapp" /> (71) 99629-3392</span>
        <a href="tel:+557131839000"><i className="bi bi-telephone" /> (71) 3183-9000</a>
        <a href="https://www.instagram.com/dubai.magazine/" target="_blank" rel="noreferrer">
          <i className="bi bi-instagram" /> @dubai.magazine
        </a>
        <a href="https://maps.app.goo.gl/8wzibo5BrmwbmK9H8" target="_blank" rel="noreferrer">
          <i className="bi bi-geo-alt" /> Rua do Uruguay, 63 - Uruguai, Salvador - BA, 40450-211
        </a>
      </div>
    </PaginaInstitucional>
  );
}

export function TrocasDevolucoes() {
  return (
    <PaginaInstitucional titulo="Trocas e devoluções" subtitulo="Informações iniciais para atendimento após a compra.">
      <h2>Como solicitar atendimento</h2>
      <p>
        Para solicitar troca ou devolução, entre em contato com a loja e tenha em mãos a nota
        fiscal e as informações do produto. A equipe verificará as condições, os prazos aplicáveis
        e orientará os próximos passos.
      </p>
    </PaginaInstitucional>
  );
}
