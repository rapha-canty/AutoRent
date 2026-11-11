package com.autorent.controller;

import com.autorent.entity.Cliente;
import com.autorent.entity.Usuario;
import com.autorent.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clientes;

    public ClienteController(ClienteService clientes) {
        this.clientes = clientes;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public List<Cliente> listar() {
        return clientes.listar();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Cliente criar(@Valid @RequestBody ClienteService.Form form) {
        return clientes.criar(form);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public Cliente atualizar(@PathVariable Long id, @Valid @RequestBody ClienteService.Form form) {
        return clientes.atualizar(id, form);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        clientes.excluir(id);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public Cliente meusDados(@AuthenticationPrincipal Usuario u) {
        return clientes.meusDados(u);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public Cliente atualizarMeusDados(@AuthenticationPrincipal Usuario u, @Valid @RequestBody ClienteService.Form form) {
        return clientes.atualizarMeusDados(u, form);
    }
}
