package com.autorent.controller;

import com.autorent.entity.Locacao;
import com.autorent.entity.Usuario;
import com.autorent.service.LocacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/locacoes")
public class LocacaoController {

    private final LocacaoService locacoes;

    public LocacaoController(LocacaoService locacoes) {
        this.locacoes = locacoes;
    }

    @GetMapping
    public List<Locacao> listar(@AuthenticationPrincipal Usuario u) {
        return locacoes.listar(u);
    }

    @PostMapping("/retirada")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Locacao retirar(@RequestBody LocacaoService.Retirada retirada) {
        return locacoes.retirar(retirada);
    }

    @PostMapping("/{id}/devolucao")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public Locacao devolver(@PathVariable Long id, @RequestBody Map<String, Integer> corpo) {
        return locacoes.devolver(id, corpo.get("kmDevolucao"));
    }
}
