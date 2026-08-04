package com.ecommerceproject.dubaimagazinesalvador.services.importacao;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

@Service
public class ControleImportacaoProdutosService {

    private final AtomicBoolean importacaoEmAndamento = new AtomicBoolean(false);

    public <T> T executarExclusivamente(Supplier<T> importacao) {
        if (!importacaoEmAndamento.compareAndSet(false, true)) {
            throw new ImportacaoEmAndamentoException();
        }

        try {
            return importacao.get();
        } finally {
            importacaoEmAndamento.set(false);
        }
    }
}
