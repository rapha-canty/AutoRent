package com.autorent.controller;

import com.autorent.entity.Pagamento;
import com.autorent.entity.Usuario;
import com.autorent.service.PagamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentos;

    public PagamentoController(PagamentoService pagamentos) {
        this.pagamentos = pagamentos;
    }

    @GetMapping
    public List<Pagamento> listar(@AuthenticationPrincipal Usuario u) {
        return pagamentos.listar(u);
    }

    @GetMapping("/locacao/{idLocacao}/saldo")
    public Map<String, BigDecimal> saldo(@AuthenticationPrincipal Usuario u, @PathVariable Long idLocacao) {
        return pagamentos.saldo(u, idLocacao);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Pagamento registrar(@Valid @RequestBody PagamentoService.Form form) {
        return pagamentos.registrar(form);
    }

    @PatchMapping("/{id}/estorno")
    @PreAuthorize("hasRole('ADMIN')")
    public Pagamento estornar(@PathVariable Long id) {
        return pagamentos.estornar(id);
    }
}
