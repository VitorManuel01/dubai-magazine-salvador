package com.ecommerceproject.dubaimagazinesalvador.controller;

import java.time.Instant;
import java.util.Locale;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Administrador;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.AuthenticationDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.LoginResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.RegisterAdmDTO;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Role;
import com.ecommerceproject.dubaimagazinesalvador.domain.usuarios.Usuario;
import com.ecommerceproject.dubaimagazinesalvador.infra.security.TokenService;
import com.ecommerceproject.dubaimagazinesalvador.repositories.AdministradorRespository;
import com.ecommerceproject.dubaimagazinesalvador.repositories.UsuarioRepository;
import com.ecommerceproject.dubaimagazinesalvador.services.LimitadorOrigemLoginService;
import com.ecommerceproject.dubaimagazinesalvador.services.LimitadorOrigemLoginService.EstadoLimite;
import com.ecommerceproject.dubaimagazinesalvador.services.TentativasLoginService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("auth")
public class AuthController {

    private static final String ERRO_CREDENCIAIS = "Código Santri ou senha inválidos.";
    private static final String ERRO_LIMITE = "Muitas tentativas. Aguarde antes de tentar novamente.";

    private final AuthenticationManager authenticationManager;
    private final AdministradorRespository administradorRepository;
    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final TentativasLoginService tentativasLoginService;
    private final LimitadorOrigemLoginService limitadorOrigemLoginService;

    public AuthController(
            AuthenticationManager authenticationManager,
            AdministradorRespository administradorRepository,
            UsuarioRepository usuarioRepository,
            TokenService tokenService,
            PasswordEncoder passwordEncoder,
            TentativasLoginService tentativasLoginService,
            LimitadorOrigemLoginService limitadorOrigemLoginService
    ) {
        this.authenticationManager = authenticationManager;
        this.administradorRepository = administradorRepository;
        this.usuarioRepository = usuarioRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.tentativasLoginService = tentativasLoginService;
        this.limitadorOrigemLoginService = limitadorOrigemLoginService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody AuthenticationDTO data,
            HttpServletRequest request
    ) {
        String codigoSantri = normalizarCodigoSantri(data.codigoSantri());
        String enderecoIp = request.getRemoteAddr();
        String dispositivo = request.getHeader(LimitadorOrigemLoginService.CABECALHO_DISPOSITIVO);

        EstadoLimite limiteAtual = limitadorOrigemLoginService.reservarTentativa(
                enderecoIp,
                dispositivo
        );
        if (limiteAtual.limitado()) {
            return respostaLimitada(limiteAtual.tentarNovamenteEm());
        }

        try {
            var credenciais = new UsernamePasswordAuthenticationToken(
                    codigoSantri,
                    data.senha()
            );
            var autenticacao = authenticationManager.authenticate(credenciais);
            Usuario usuario = (Usuario) autenticacao.getPrincipal();
            tentativasLoginService.registrarSucesso(usuario.getId());
            limitadorOrigemLoginService.registrarSucesso(enderecoIp, dispositivo);
            return ResponseEntity.ok(new LoginResponseDTO(tokenService.generateToken(usuario)));
        } catch (AuthenticationException e) {
            tentativasLoginService.registrarFalha(codigoSantri);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginErroDTO(ERRO_CREDENCIAIS));
        }
    }

    private ResponseEntity<LoginErroDTO> respostaLimitada(Instant tentarNovamenteEm) {
        long milissegundos = Math.max(
                0,
                tentarNovamenteEm.toEpochMilli() - Instant.now().toEpochMilli()
        );
        long segundos = Math.max(1, (milissegundos + 999) / 1_000);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(segundos))
                .body(new LoginErroDTO(ERRO_LIMITE));
    }

    public record LoginErroDTO(String erro) {
    }

    @PostMapping("/registerADM")
    public ResponseEntity<Void> registerADM(@Valid @RequestBody RegisterAdmDTO data) {
        String codigoSantri = normalizarCodigoSantri(data.codigoSantri());
        if (usuarioRepository.existsByCodigoSantriIgnoreCase(codigoSantri)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Código Santri já cadastrado."
            );
        }

        Administrador administrador = new Administrador();
        administrador.setCodigoSantri(codigoSantri);
        administrador.setSenha(passwordEncoder.encode(data.senha()));
        administrador.setFuncao(Role.ROLE_ADMIN);
        administrador.setAdmin(true);
        administrador.setNome(data.nome().trim());
        administrador.setCPF(data.CPF());
        administrador.setSexo(textoOpcional(data.sexo()).toUpperCase(Locale.ROOT));
        administrador.setDataNascimento(data.dataNascimento());
        administrador.setCEP(textoOpcional(data.CEP()));
        administrador.setEndereco(textoOpcional(data.endereco()));
        administrador.setBairro(textoOpcional(data.bairro()));
        administrador.setTelefone(textoOpcional(data.telefone()));
        administradorRepository.save(administrador);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private String normalizarCodigoSantri(String codigoSantri) {
        return codigoSantri.trim().toUpperCase(Locale.ROOT);
    }

    private String textoOpcional(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
