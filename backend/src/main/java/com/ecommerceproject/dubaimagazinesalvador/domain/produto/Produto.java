package com.ecommerceproject.dubaimagazinesalvador.domain.produto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ecommerceproject.dubaimagazinesalvador.domain.categoria.Categoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    @Id
    @Column(name = "codigo_santri", length = 32)
    private String codigoSantri;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(name = "nome_exibido_site", nullable = false, length = 500)
    private String nomeExibidoSite;

    @Column(length = 20)
    private String ncm;

    @Column(length = 20)
    private String unidade;

    private String marca;

    @Column(name = "codigo_original")
    private String codigoOriginal;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "preco_venda", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoVenda;

    @Column(name = "preco_venda_iva", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoVendaIva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_codigo", nullable = false)
    private Categoria categoria;

    @Column(name = "imagem_url", length = 1000)
    private String imagemUrl;

    @Column(name = "exibir_no_site", nullable = false)
    private boolean exibirNoSite;

    @Column(name = "destaque_na_home", nullable = false)
    private boolean destaqueNaHome;

    @Column(name = "ultima_importacao_em")
    private LocalDateTime ultimaImportacaoEm;

    public Produto(ProdutoRequestDTO data, Categoria categoria) {
        this.codigoSantri = data.codigoSantri();
        atualizar(data, categoria);
    }

    public Produto(ProdutoImportacaoDTO data, Categoria categoria, LocalDateTime importadoEm) {
        this.codigoSantri = data.codigoSantri();
        this.imagemUrl = null;
        this.exibirNoSite = false;
        this.destaqueNaHome = false;
        atualizarDadosImportados(data, categoria, importadoEm);
    }

    public void atualizar(ProdutoRequestDTO data, Categoria categoria) {
        this.descricao = data.descricao();
        this.nomeExibidoSite = normalizarNomeExibidoSite(
                data.nomeExibidoSite(),
                data.descricao()
        );
        this.ncm = data.ncm();
        this.unidade = data.unidade();
        this.marca = data.marca();
        this.codigoOriginal = data.codigoOriginal();
        this.quantidade = data.quantidade();
        this.precoVenda = data.precoVenda();
        this.precoVendaIva = valorOuZero(data.precoVendaIva());
        this.categoria = categoria;
        this.imagemUrl = data.imagemUrl();
        this.exibirNoSite = data.exibirNoSite();
    }

    public void atualizarDadosImportados(
            ProdutoImportacaoDTO data,
            Categoria categoria,
            LocalDateTime importadoEm
    ) {
        if (nomeExibidoSite == null
                || nomeExibidoSite.isBlank()
                || nomeExibidoSite.equals(descricao)) {
            this.nomeExibidoSite = data.descricao();
        }
        this.descricao = data.descricao();
        this.ncm = data.ncm();
        this.unidade = data.unidade();
        this.marca = data.marca();
        this.codigoOriginal = data.codigoOriginal();
        this.quantidade = data.quantidade();
        this.precoVenda = data.precoVenda();
        this.precoVendaIva = valorOuZero(data.precoVendaIva());
        this.categoria = categoria;
        this.ultimaImportacaoEm = importadoEm;
    }

    public void atualizarApresentacao(
            String nomeExibidoSite,
            boolean exibirNoSite,
            boolean destaqueNaHome,
            String novaImagemUrl
    ) {
        this.nomeExibidoSite = normalizarNomeExibidoSite(
                nomeExibidoSite,
                descricao
        );
        this.exibirNoSite = exibirNoSite;
        this.destaqueNaHome = destaqueNaHome;
        if (novaImagemUrl != null && !novaImagemUrl.isBlank()) {
            this.imagemUrl = novaImagemUrl;
        }
    }

    private BigDecimal valorOuZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private String normalizarNomeExibidoSite(String nome, String nomePadrao) {
        if (nome == null || nome.isBlank()) {
            return nomePadrao;
        }
        return nome.trim().replaceAll("\\s+", " ");
    }
}
