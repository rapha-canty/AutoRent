package com.autorent.controller;

import com.autorent.entity.Reserva;
import com.autorent.entity.Usuario;
import com.autorent.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservas;

    public ReservaController(ReservaService reservas) {
        this.reservas = reservas;
    }

    @GetMapping
    public List<Reserva> listar(@AuthenticationPrincipal Usuario u) {
        return reservas.listar(u);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Reserva criar(@AuthenticationPrincipal Usuario u, @Valid @RequestBody ReservaService.Form form) {
        return reservas.criar(u, form);
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public Reserva confirmar(@PathVariable Long id) {
        return reservas.confirmar(id);
    }

    @PatchMapping("/{id}/cancelar")
    public Reserva cancelar(@AuthenticationPrincipal Usuario u, @PathVariable Long id) {
        return reservas.cancelar(u, id);
    }
}
