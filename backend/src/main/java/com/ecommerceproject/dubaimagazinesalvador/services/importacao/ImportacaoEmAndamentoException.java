package com.ecommerceproject.dubaimagazinesalvador.services.importacao;

public class ImportacaoEmAndamentoException extends RuntimeException {

    public ImportacaoEmAndamentoException() {
        super("Já existe uma importação de produtos em andamento.");
    }
}
