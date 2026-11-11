package com.autorent.controller;

import com.autorent.config.ApiException;
import com.autorent.entity.Usuario;
import com.autorent.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    public record Form(
            @NotBlank(message = "Informe o nome.") String nome,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
            @NotBlank(message = "Informe a senha.") @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.") String senha,
            @NotNull(message = "Informe o perfil.") Usuario.Perfil perfil) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public UsuarioController(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return usuarios.findAll().stream().map(this::resumo).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> criar(@Valid @RequestBody Form f) {
        if (f.perfil() == Usuario.Perfil.CLIENTE) {
            throw ApiException.regra("Usuários CLIENTE são criados pelo cadastro público.");
        }
        String email = f.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflito("Já existe usuário com este e-mail.");
        }
        Usuario u = new Usuario();
        u.setNome(f.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(encoder.encode(f.senha()));
        u.setPerfil(f.perfil());
        return resumo(usuarios.saveAndFlush(u));
    }

    @PatchMapping("/{id}/ativo")
    @Transactional
    public Map<String, Object> alterarAtivo(@AuthenticationPrincipal Usuario logado, @PathVariable Long id,
                                            @RequestParam boolean valor) {
        if (logado.getId().equals(id) && !valor) {
            throw ApiException.regra("Você não pode desativar o seu próprio usuário.");
        }
        Usuario u = usuarios.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Usuário"));
        u.setAtivo(valor);
        return resumo(usuarios.saveAndFlush(u));
    }

    private Map<String, Object> resumo(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("nome", u.getNome());
        m.put("email", u.getEmail());
        m.put("perfil", u.getPerfil());
        m.put("ativo", u.isAtivo());
        return m;
    }
}
