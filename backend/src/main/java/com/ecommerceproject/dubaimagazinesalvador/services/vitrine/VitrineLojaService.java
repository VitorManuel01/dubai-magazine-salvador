package com.ecommerceproject.dubaimagazinesalvador.services.vitrine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.produto.Produto;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.ProdutoCandidatoVitrineLojaDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.ProdutoVitrineLoja;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.ProdutoVitrineLojaRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.SecaoVitrineLoja;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.SecaoVitrineLojaRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.VitrineLoja;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.VitrineLojaRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.loja.VitrineLojaResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.ProdutoRepository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.VitrineLojaRepository;

@Service
public class VitrineLojaService {

    private static final int MAXIMO_OPCOES = 20;
    private static final int MAXIMO_IMAGENS_POR_OPCAO = 20;
    private static final int MAXIMO_SECOES_POR_OPCAO = 30;

    private final VitrineLojaRepository vitrineRepository;
    private final ProdutoRepository produtoRepository;

    public VitrineLojaService(
            VitrineLojaRepository vitrineRepository,
            ProdutoRepository produtoRepository
    ) {
        this.vitrineRepository = vitrineRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional(readOnly = true)
    public Page<VitrineLojaResponseDTO> pesquisar(
            String busca,
            int pagina,
            int tamanho,
            boolean somenteAtivas
    ) {
        validarPaginacao(pagina, tamanho);
        String buscaNormalizada = busca == null || busca.isBlank()
                ? null
                : busca.trim();
        return vitrineRepository.pesquisar(
                somenteAtivas,
                buscaNormalizada,
                PageRequest.of(pagina, tamanho)
        ).map(VitrineLojaResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoCandidatoVitrineLojaDTO> pesquisarProdutosParaAdministracao(
            String busca,
            int pagina,
            int tamanho
    ) {
        validarPaginacao(pagina, tamanho, 60);
        String buscaNormalizada = busca == null || busca.isBlank()
                ? null
                : busca.trim();
        return produtoRepository.findCatalogoPorCategoria(
                null,
                buscaNormalizada,
                true,
                false,
                PageRequest.of(pagina, tamanho)
        ).map(ProdutoCandidatoVitrineLojaDTO::new);
    }

    @Transactional(readOnly = true)
    public VitrineLojaResponseDTO buscarAtiva(Long id) {
        VitrineLoja vitrine = vitrineRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vitrine da loja não encontrada."
                ));
        return new VitrineLojaResponseDTO(vitrine);
    }

    @Transactional(readOnly = true)
    public VitrineLojaResponseDTO buscarAdministracao(Long id) {
        return new VitrineLojaResponseDTO(buscar(id));
    }

    @Transactional
    public VitrineLojaResponseDTO criar(VitrineLojaRequestDTO dados) {
        DadosNormalizados normalizados = validar(dados, null);
        VitrineLoja vitrine = new VitrineLoja(normalizados.ativo());
        adicionarOpcoes(vitrine, normalizados.opcoes());
        return new VitrineLojaResponseDTO(vitrineRepository.saveAndFlush(vitrine));
    }

    @Transactional
    public VitrineLojaResponseDTO atualizar(Long id, VitrineLojaRequestDTO dados) {
        VitrineLoja vitrine = buscar(id);
        DadosNormalizados normalizados = validar(dados, id);

        vitrine.atualizarAtivo(normalizados.ativo());
        vitrine.removerTodasOpcoes();
        vitrineRepository.flush();
        adicionarOpcoes(vitrine, normalizados.opcoes());

        return new VitrineLojaResponseDTO(vitrineRepository.saveAndFlush(vitrine));
    }

    @Transactional
    public void excluir(Long id) {
        vitrineRepository.delete(buscar(id));
    }

    private VitrineLoja buscar(Long id) {
        return vitrineRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vitrine da loja não encontrada."
                ));
    }

    private DadosNormalizados validar(VitrineLojaRequestDTO dados, Long vitrineId) {
        if (dados == null) {
            throw erro("Informe os dados da vitrine da loja.");
        }
        List<ProdutoVitrineLojaRequestDTO> opcoesRecebidas = dados.opcoes();
        if (opcoesRecebidas == null || opcoesRecebidas.isEmpty()) {
            throw erro("A vitrine deve possuir pelo menos uma opção de produto.");
        }
        if (opcoesRecebidas.size() > MAXIMO_OPCOES) {
            throw erro("A vitrine pode possuir no máximo 20 opções de produto.");
        }

        boolean ativo = dados.ativo() == null || dados.ativo();
        Set<String> codigosNoFormulario = new HashSet<>();
        List<OpcaoNormalizada> opcoes = new ArrayList<>(opcoesRecebidas.size());

        for (int indice = 0; indice < opcoesRecebidas.size(); indice++) {
            ProdutoVitrineLojaRequestDTO opcao = opcoesRecebidas.get(indice);
            if (opcao == null) {
                throw erro("A opção de produto na posição " + (indice + 1) + " está vazia.");
            }
            String codigo = normalizarCodigoProduto(opcao.produtoCodigoSantri());
            if (!codigosNoFormulario.add(codigo)) {
                throw erro("O produto " + codigo + " foi informado mais de uma vez.");
            }
            Produto produto = produtoRepository.findById(codigo)
                    .orElseThrow(() -> erro("Produto não encontrado: " + codigo));
            String rotulo = textoOuPadrao(
                    opcao.rotuloOpcao(),
                    produto.getNomeExibidoSite()
            );
            if (rotulo.length() > 100) {
                throw erro("O rótulo da opção deve possuir no máximo 100 caracteres.");
            }
            int ordem = numeroNaoNegativo(opcao.ordem(), indice, "ordem da opção");
            List<String> imagens = normalizarImagens(opcao.imagens());
            List<SecaoNormalizada> secoes = normalizarSecoes(opcao.secoes());
            if (ativo && secoes.isEmpty()) {
                throw erro(
                        "A opção " + rotulo
                                + " precisa possuir ao menos uma seção para ser ativada."
                );
            }
            opcoes.add(new OpcaoNormalizada(produto, rotulo, ordem, imagens, secoes));
        }

        List<String> conflitos = vitrineRepository.encontrarProdutosEmOutrasVitrines(
                codigosNoFormulario,
                vitrineId
        );
        if (!conflitos.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Produto já utilizado em outra vitrine: " + conflitos.getFirst()
            );
        }

        return new DadosNormalizados(ativo, opcoes);
    }

    private List<String> normalizarImagens(List<String> imagensRecebidas) {
        if (imagensRecebidas == null || imagensRecebidas.isEmpty()) {
            return List.of();
        }
        if (imagensRecebidas.size() > MAXIMO_IMAGENS_POR_OPCAO) {
            throw erro("Cada opção pode possuir no máximo 20 imagens.");
        }

        LinkedHashSet<String> imagens = new LinkedHashSet<>();
        for (String imagem : imagensRecebidas) {
            if (imagem == null || imagem.isBlank()) {
                continue;
            }
            String url = imagem.trim();
            if (url.length() > 1000) {
                throw erro("A URL da imagem deve possuir no máximo 1000 caracteres.");
            }
            imagens.add(url);
        }
        return List.copyOf(imagens);
    }

    private List<SecaoNormalizada> normalizarSecoes(
            List<SecaoVitrineLojaRequestDTO> secoesRecebidas
    ) {
        if (secoesRecebidas == null || secoesRecebidas.isEmpty()) {
            return List.of();
        }
        if (secoesRecebidas.size() > MAXIMO_SECOES_POR_OPCAO) {
            throw erro("Cada opção pode possuir no máximo 30 seções.");
        }

        List<SecaoNormalizada> secoes = new ArrayList<>(secoesRecebidas.size());
        for (int indice = 0; indice < secoesRecebidas.size(); indice++) {
            SecaoVitrineLojaRequestDTO secao = secoesRecebidas.get(indice);
            if (secao == null) {
                throw erro("A seção na posição " + (indice + 1) + " está vazia.");
            }
            String titulo = textoObrigatorio(secao.titulo(), "O título da seção é obrigatório.");
            String conteudo = conteudoObrigatorio(secao.conteudo());
            if (titulo.length() > 180) {
                throw erro("O título da seção deve possuir no máximo 180 caracteres.");
            }
            if (conteudo.length() > 10_000) {
                throw erro("O conteúdo da seção deve possuir no máximo 10.000 caracteres.");
            }
            int ordem = numeroNaoNegativo(secao.ordem(), indice, "ordem da seção");
            secoes.add(new SecaoNormalizada(titulo, conteudo, ordem));
        }
        return secoes;
    }

    private void adicionarOpcoes(VitrineLoja vitrine, List<OpcaoNormalizada> opcoes) {
        for (OpcaoNormalizada dadosOpcao : opcoes) {
            ProdutoVitrineLoja opcao = new ProdutoVitrineLoja(
                    dadosOpcao.produto(),
                    dadosOpcao.rotulo(),
                    dadosOpcao.ordem(),
                    dadosOpcao.imagens()
            );
            for (SecaoNormalizada dadosSecao : dadosOpcao.secoes()) {
                opcao.adicionarSecao(new SecaoVitrineLoja(
                        dadosSecao.titulo(),
                        dadosSecao.conteudo(),
                        dadosSecao.ordem()
                ));
            }
            vitrine.adicionarOpcao(opcao);
        }
    }

    private void validarPaginacao(int pagina, int tamanho) {
        validarPaginacao(pagina, tamanho, 50);
    }

    private void validarPaginacao(int pagina, int tamanho, int tamanhoMaximo) {
        if (pagina < 0 || tamanho < 1 || tamanho > tamanhoMaximo) {
            throw erro(
                    "A página deve ser positiva e o tamanho deve estar entre 1 e "
                            + tamanhoMaximo + "."
            );
        }
    }

    private String normalizarCodigoProduto(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw erro("O código Santri do produto é obrigatório.");
        }
        String normalizado = codigo.replace(".", "").replaceAll("\\s+", "");
        if (normalizado.length() > 32) {
            throw erro("O código Santri deve possuir no máximo 32 caracteres.");
        }
        return normalizado;
    }

    private String textoOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank()
                ? padrao
                : valor.trim().replaceAll("\\s+", " ");
    }

    private String textoObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw erro(mensagem);
        }
        return valor.trim().replaceAll("\\s+", " ");
    }

    private String conteudoObrigatorio(String valor) {
        if (valor == null || valor.isBlank()) {
            throw erro("O conteúdo da seção é obrigatório.");
        }
        return valor.trim().replaceAll("[\\t ]+", " ");
    }

    private int numeroNaoNegativo(Integer valor, int padrao, String campo) {
        int numero = valor == null ? padrao : valor;
        if (numero < 0) {
            throw erro("A " + campo + " não pode ser negativa.");
        }
        return numero;
    }

    private ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }

    private record DadosNormalizados(
            boolean ativo,
            List<OpcaoNormalizada> opcoes
    ) {
    }

    private record OpcaoNormalizada(
            Produto produto,
            String rotulo,
            int ordem,
            List<String> imagens,
            List<SecaoNormalizada> secoes
    ) {
    }

    private record SecaoNormalizada(
            String titulo,
            String conteudo,
            int ordem
    ) {
    }
}
