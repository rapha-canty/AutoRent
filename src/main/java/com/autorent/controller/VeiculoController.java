package com.autorent.controller;

import com.autorent.entity.Veiculo;
import com.autorent.service.VeiculoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/veiculos")
public class VeiculoController {

    private final VeiculoService veiculos;

    public VeiculoController(VeiculoService veiculos) {
        this.veiculos = veiculos;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public List<Veiculo> listar() {
        return veiculos.listar();
    }

    @GetMapping("/disponiveis")
    public List<Veiculo> disponiveis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Veiculo.Categoria categoria,
            @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax) {
        return veiculos.disponiveis(inicio, fim, busca, categoria, precoMin, precoMax);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Veiculo criar(@Valid @RequestBody VeiculoService.Form form) {
        return veiculos.criar(form);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Veiculo atualizar(@PathVariable Long id, @Valid @RequestBody VeiculoService.Form form) {
        return veiculos.atualizar(id, form);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> excluir(@PathVariable Long id) {
        return Map.of("mensagem", veiculos.excluir(id));
    }
}
