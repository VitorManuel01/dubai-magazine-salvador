package com.ecommerceproject.dubaimagazinesalvador.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.AuthenticationDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Funcionario;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.LoginResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Role;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;
import com.ecommerceproject.dubaimagazinesalvador.repositories.AdministradorRespository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.LimitadorOrigemLoginService;
import com.ecommerceproject.dubaimagazinesalvador.services.LimitadorOrigemLoginService.EstadoLimite;
import com.ecommerceproject.dubaimagazinesalvador.services.TentativasLoginService;

class AuthControllerTest {

    private AuthenticationManager authenticationManager;
    private TokenService tokenService;
    private TentativasLoginService tentativasLoginService;
    private LimitadorOrigemLoginService limitador;
    private AuthController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void configurar() {
        authenticationManager = mock(AuthenticationManager.class);
        tokenService = mock(TokenService.class);
        tentativasLoginService = mock(TentativasLoginService.class);
        limitador = mock(LimitadorOrigemLoginService.class);
        controller = new AuthController(
                authenticationManager,
                mock(AdministradorRespository.class),
                mock(UsuarioRepository.class),
                tokenService,
                mock(PasswordEncoder.class),
                tentativasLoginService,
                limitador
        );
        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.60");
        request.addHeader(
                LimitadorOrigemLoginService.CABECALHO_DISPOSITIVO,
                UUID.randomUUID().toString()
        );
        when(limitador.reservarTentativa(any(), any()))
                .thenReturn(new EstadoLimite(false, null));
    }

    @Test
    void falhaUsaRespostaGenericaSemInformarSeContaExiste() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("detalhe interno"));

        var resposta = controller.login(
                new AuthenticationDTO("fun-001", "senha-incorreta"),
                request
        );

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        AuthController.LoginErroDTO erro = (AuthController.LoginErroDTO) resposta.getBody();
        assertNotNull(erro);
        assertEquals("Código Santri ou senha inválidos.", erro.erro());
        verify(tentativasLoginService).registrarFalha("FUN-001");
    }

    @Test
    void origemLimitadaRecebe429ComRetryAfter() {
        when(limitador.reservarTentativa(any(), any())).thenReturn(
                new EstadoLimite(true, Instant.now().plusSeconds(10))
        );

        var resposta = controller.login(
                new AuthenticationDTO("FUN-001", "qualquer-senha"),
                request
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, resposta.getStatusCode());
        assertNotNull(resposta.getHeaders().getFirst("Retry-After"));
    }

    @Test
    void sucessoNormalizaCodigoEEmiteTokenDoUsuarioAutenticado() {
        Funcionario usuario = new Funcionario();
        usuario.setId(UUID.randomUUID());
        usuario.setCodigoSantri("FUN-001");
        usuario.setFuncao(Role.ROLE_FUNCIONARIO);
        var autenticado = new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                usuario.getAuthorities()
        );
        when(authenticationManager.authenticate(any())).thenReturn(autenticado);
        when(tokenService.generateToken(usuario)).thenReturn("jwt-seguro");

        var resposta = controller.login(
                new AuthenticationDTO(" fun-001 ", "SenhaForte@2026"),
                request
        );

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals("jwt-seguro", ((LoginResponseDTO) resposta.getBody()).token());
        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("FUN-001", captor.getValue().getPrincipal());
        verify(tentativasLoginService).registrarSucesso(usuario.getId());
    }
}
