package com.ecommerceproject.dubaimagazinesalvador.services.vitrine;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.DepoimentoHome;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.DepoimentoHomeRequestDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.DepoimentoHomeResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.repositories.DepoimentoHomeRepository;

@Service
public class DepoimentoHomeService {

    private static final Set<Integer> POSICOES_VALIDAS = Set.of(1, 2, 3);

    private final DepoimentoHomeRepository repository;

    public DepoimentoHomeService(DepoimentoHomeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<DepoimentoHomeResponseDTO> listar() {
        return repository.findAllByOrderByPosicaoAsc().stream()
                .map(DepoimentoHomeResponseDTO::new)
                .toList();
    }

    @Transactional
    public List<DepoimentoHomeResponseDTO> atualizar(
            List<DepoimentoHomeRequestDTO> solicitados
    ) {
        Map<Integer, DepoimentoHomeRequestDTO> porPosicao = solicitados.stream()
                .collect(Collectors.toMap(
                        DepoimentoHomeRequestDTO::posicao,
                        Function.identity(),
                        (primeiro, repetido) -> primeiro
                ));

        if (!porPosicao.keySet().equals(POSICOES_VALIDAS)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Devem ser enviados exatamente os depoimentos das posições 1, 2 e 3."
            );
        }

        List<DepoimentoHome> existentes = repository.findAllByOrderByPosicaoAsc();
        if (existentes.size() != POSICOES_VALIDAS.size()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Os depoimentos da página inicial não estão configurados corretamente."
            );
        }

        existentes.forEach(depoimento -> {
            DepoimentoHomeRequestDTO solicitado = porPosicao.get(depoimento.getPosicao());
            depoimento.atualizar(solicitado.nome().trim(), solicitado.texto().trim());
        });

        return repository.saveAllAndFlush(existentes).stream()
                .sorted((primeiro, segundo) -> Integer.compare(
                        primeiro.getPosicao(),
                        segundo.getPosicao()
                ))
                .map(DepoimentoHomeResponseDTO::new)
                .toList();
    }
}
