package com.ecommerceproject.dubaimagazinesalvador.controller;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ecommerceproject.dubaimagazinesalvador.infra.security.AuditoriaAdministrativaInterceptor;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.LimitadorRequisicoesPublicasInterceptor;
import com.ecommerceproject.dubaimagazinesalvador.services.AuditoriaAdministrativaService;
import com.ecommerceproject.dubaimagazinesalvador.services.LimitadorRequisicoesPublicasService;

/** O recorte MVC preserva os interceptors; somente a persistência da auditoria é simulada. */
@Import({
        LimitadorRequisicoesPublicasService.class,
        LimitadorRequisicoesPublicasInterceptor.class,
        AuditoriaAdministrativaInterceptor.class
})
abstract class ProtecoesWebTestSupport {
    @MockitoBean
    protected AuditoriaAdministrativaService auditoria;
}
