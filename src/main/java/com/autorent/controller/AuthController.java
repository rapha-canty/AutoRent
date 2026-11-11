package com.autorent.controller;

import com.autorent.entity.Usuario;
import com.autorent.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final ClienteService clientes;

    public AuthController(ClienteService clientes) {
        this.clientes = clientes;
    }

    @GetMapping("/eu")
    public Map<String, Object> eu(@AuthenticationPrincipal Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("nome", u.getNome());
        m.put("email", u.getEmail());
        m.put("perfil", u.getPerfil());
        m.put("idCliente", u.getCliente() == null ? null : u.getCliente().getId());
        return m;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> cadastro(@Valid @RequestBody ClienteService.Cadastro dados) {
        clientes.cadastrar(dados);
        return Map.of("mensagem", "Cadastro realizado. Entre com o seu e-mail e a senha.");
    }
}
