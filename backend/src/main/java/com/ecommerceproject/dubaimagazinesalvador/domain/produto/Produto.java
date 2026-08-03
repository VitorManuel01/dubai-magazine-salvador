package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "codigoSantri")
public class Produto {

    //A lista de atributos da classe Produto é extensa para acomodar as informações contidas na base de dados do santri
    //e permitir uma futura integração com o sistema de e-commerce caso venha a ser implementado.
    //Como o santri cobra pela integração, a ideia é, de certa forma manualmente, mas não realmente, sincronizar o banco de dados 
    //desta aplicação com o banco de dados do santri através da importação da planilha .ods que o santri disponibiliza para download, que contém todos os produtos cadastrados no santri, com seus respectivos atributos.


    @Id
    @Column(name = "codigo_santri", length = 32)
    private String codigoSantri;

    @Column(nullable = false, length = 500)
    private String nome;

    @Column(name = "nome_exibido_site", nullable = false, length = 500)
    private String nomeExibidoSite;

    @Column(length = 20)
    private String ncm;

    @Column(name = "nome_compra", length = 500)
    private String nomeCompra;

    @Column(length = 255)
    private String fabricante;

    @Column(length = 255)
    private String marca;

    @Column(name = "ativo_santri", nullable = false)
    private boolean ativoSantri;

    @Column(name = "unidade_venda", length = 20)
    private String unidadeVenda;

    @Column(name = "unidade_compra", length = 20)
    private String unidadeCompra;

    @Column(name = "data_cadastro")
    private LocalDate dataCadastro;

    @Column(name = "codigo_original", length = 255)
    private String codigoOriginal;

    @Column(name = "codigo_barras", length = 32)
    private String codigoBarras;

    @Column(name = "bloqueado_para_compras", nullable = false)
    private boolean bloqueadoParaCompras;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal estoque;

    @Column(name = "preco_sem_ipi", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoSemIpi;

    @Column(name = "percentual_ipi_entrada", precision = 7, scale = 4)
    private BigDecimal percentualIpiEntrada;

    @Column(name = "peso_unidade", precision = 18, scale = 6)
    private BigDecimal pesoUnidade;

    @Column(name = "altura_unidade", precision = 18, scale = 6)
    private BigDecimal alturaUnidade;

    @Column(name = "largura_unidade", precision = 18, scale = 6)
    private BigDecimal larguraUnidade;

    @Column(name = "comprimento_unidade", precision = 18, scale = 6)
    private BigDecimal comprimentoUnidade;

    @Column(name = "volume_unidade_m3", precision = 18, scale = 9)
    private BigDecimal volumeUnidadeM3;

    @Column(name = "volume_litros", precision = 18, scale = 6)
    private BigDecimal volumeLitros;

    @Column(name = "peso_caixa", precision = 18, scale = 6)
    private BigDecimal pesoCaixa;

    @Column(name = "altura_caixa", precision = 18, scale = 6)
    private BigDecimal alturaCaixa;

    @Column(name = "largura_caixa", precision = 18, scale = 6)
    private BigDecimal larguraCaixa;

    @Column(name = "comprimento_caixa", precision = 18, scale = 6)
    private BigDecimal comprimentoCaixa;

    @Column(length = 255)
    private String origem;

    private Boolean industrializado;

    private Boolean insumo;

    @Column(name = "percentual_maximo_aproveitamento_ipi", precision = 7, scale = 4)
    private BigDecimal percentualMaximoAproveitamentoIpi;

    @Column(name = "numero_fci", length = 64)
    private String numeroFci;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_codigo", nullable = false)
    private Categoria categoria;

    @Column(name = "imagem_url", length = 1000)
    private String imagemUrl;

    @Column(name = "exibir_no_site", nullable = false)
    private boolean exibirNoSite;

    @Column(name = "destaque_na_home", nullable = false)
    private boolean destaqueNaHome;

    @Column(name = "disponivel_ultima_importacao", nullable = false)
    private boolean disponivelUltimaImportacao;

    @Column(name = "ultima_importacao_em")
    private LocalDateTime ultimaImportacaoEm;

    public Produto(ProdutoRequestDTO data, Categoria categoria) {
        this.codigoSantri = data.codigoSantri();
        this.nome = data.descricao();
        this.nomeExibidoSite = normalizarNomeExibidoSite(
                data.nomeExibidoSite(),
                data.descricao()
        );
        this.ncm = data.ncm();
        this.nomeCompra = data.descricao();
        this.marca = data.marca();
        this.ativoSantri = true;
        this.unidadeVenda = data.unidade();
        this.unidadeCompra = data.unidade();
        this.codigoOriginal = data.codigoOriginal();
        this.bloqueadoParaCompras = false;
        this.estoque = valorOuZero(data.quantidade());
        this.precoSemIpi = valorOuZero(data.precoVenda());
        this.percentualIpiEntrada = BigDecimal.ZERO;
        this.categoria = categoria;
        this.imagemUrl = data.imagemUrl();
        this.exibirNoSite = data.exibirNoSite();
        this.destaqueNaHome = false;
        this.disponivelUltimaImportacao = true;
    }

    public Produto(ProdutoImportacaoDTO data, Categoria categoria, LocalDateTime importadoEm) {
        this.codigoSantri = data.codigoSantri();
        this.imagemUrl = null;
        this.exibirNoSite = false;
        this.destaqueNaHome = false;
        atualizarDadosImportados(data, categoria, importadoEm);
    }

    public void atualizarDadosImportados(
            ProdutoImportacaoDTO data,
            Categoria categoria,
            LocalDateTime importadoEm
    ) {
        if (nomeExibidoSite == null
                || nomeExibidoSite.isBlank()
                || nomeExibidoSite.equals(nome)) {
            this.nomeExibidoSite = data.nome();
        }
        this.nome = data.nome();
        this.ncm = data.ncm();
        this.nomeCompra = data.nomeCompra();
        this.fabricante = data.fabricante();
        this.marca = data.marca();
        this.ativoSantri = data.ativoSantri();
        this.unidadeVenda = data.unidadeVenda();
        this.unidadeCompra = data.unidadeCompra();
        this.dataCadastro = data.dataCadastro();
        this.codigoOriginal = data.codigoOriginal();
        this.codigoBarras = data.codigoBarras();
        this.bloqueadoParaCompras = data.bloqueadoParaCompras();
        this.estoque = valorOuZero(data.estoque());
        this.precoSemIpi = valorOuZero(data.precoSemIpi());
        this.percentualIpiEntrada = data.percentualIpiEntrada();
        this.pesoUnidade = data.pesoUnidade();
        this.alturaUnidade = data.alturaUnidade();
        this.larguraUnidade = data.larguraUnidade();
        this.comprimentoUnidade = data.comprimentoUnidade();
        this.volumeUnidadeM3 = data.volumeUnidadeM3();
        this.volumeLitros = data.volumeLitros();
        this.pesoCaixa = data.pesoCaixa();
        this.alturaCaixa = data.alturaCaixa();
        this.larguraCaixa = data.larguraCaixa();
        this.comprimentoCaixa = data.comprimentoCaixa();
        this.origem = data.origem();
        this.industrializado = data.industrializado();
        this.insumo = data.insumo();
        this.percentualMaximoAproveitamentoIpi = data.percentualMaximoAproveitamentoIpi();
        this.numeroFci = data.numeroFci();
        this.categoria = categoria;
        this.disponivelUltimaImportacao = true;
        this.ultimaImportacaoEm = importadoEm;
    }

    public void atualizarApresentacao(
            String nomeExibidoSite,
            boolean exibirNoSite,
            boolean destaqueNaHome,
            String novaImagemUrl
    ) {
        this.nomeExibidoSite = normalizarNomeExibidoSite(nomeExibidoSite, nome);
        this.exibirNoSite = exibirNoSite;
        this.destaqueNaHome = destaqueNaHome;
        if (novaImagemUrl != null && !novaImagemUrl.isBlank()) {
            this.imagemUrl = novaImagemUrl;
        }
    }
    //como a loja trabalha com muitos produtos importados, é imperativo a tratativa do preço com IPI, pois o santri não disponibiliza o preço com IPI, apenas o preço sem IPI e o percentual de IPI de entrada. 
    // Portanto, para exibir o preço correto no site, é necessário calcular o preço com IPI a partir do preço sem IPI e do percentual de IPI de entrada.
    //Isto é com esta função onde fatorIpi é gerado com a função BigDecimal.ONE e movePointLeft(2) que converte o percentual de IPI de entrada em um fator multiplicativo(ex: 10.5% = 0.105 ou seja preco = precoSemIpi * 1.105) e arredondando para duas casas decimais.
    @Transient
    public BigDecimal getPrecoComIpi() {
        BigDecimal fatorIpi = BigDecimal.ONE.add(
                valorOuZero(percentualIpiEntrada).movePointLeft(2)
        );
        return valorOuZero(precoSemIpi)
                .multiply(fatorIpi)
                .setScale(2, RoundingMode.HALF_UP);
    }

    //A função valorOuZero é utilizada para evitar NullPointerException ao lidar com valores nulos de BigDecimal, retornando BigDecimal.ZERO caso o valor seja nulo.
    private BigDecimal valorOuZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private String normalizarNomeExibidoSite(String nomeInformado, String nomePadrao) {
        if (nomeInformado == null || nomeInformado.isBlank()) {
            return nomePadrao;
        }
        return nomeInformado.trim().replaceAll("\\s+", " ");
    }
}
