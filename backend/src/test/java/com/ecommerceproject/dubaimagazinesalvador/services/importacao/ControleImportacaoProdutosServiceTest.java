package com.ecommerceproject.dubaimagazinesalvador.services.importacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class ControleImportacaoProdutosServiceTest {

    @Test
    void deveAceitarSomenteUmaImportacaoPorVez() throws Exception {
        ControleImportacaoProdutosService controle =
                new ControleImportacaoProdutosService();
        CountDownLatch primeiraIniciada = new CountDownLatch(1);
        CountDownLatch liberarPrimeira = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<String> primeira = executor.submit(() ->
                    controle.executarExclusivamente(() -> {
                        primeiraIniciada.countDown();
                        aguardar(liberarPrimeira);
                        return "primeira";
                    })
            );

            assertTrue(primeiraIniciada.await(2, TimeUnit.SECONDS));
            assertThrows(
                    ImportacaoEmAndamentoException.class,
                    () -> controle.executarExclusivamente(() -> "segunda")
            );

            liberarPrimeira.countDown();
            assertEquals("primeira", primeira.get());
            assertEquals(
                    "terceira",
                    controle.executarExclusivamente(() -> "terceira")
            );
        }
    }

    private void aguardar(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Teste interrompido", e);
        }
    }
}
