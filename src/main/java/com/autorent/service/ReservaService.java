package com.autorent.service;

import com.autorent.config.ApiException;
import com.autorent.entity.Cliente;
import com.autorent.entity.Reserva;
import com.autorent.entity.Usuario;
import com.autorent.entity.Veiculo;
import com.autorent.repository.ReservaRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReservaService {

    public record Form(
            Long idCliente,
            @NotNull(message = "Informe o veículo.") Long idVeiculo,
            @NotNull(message = "Informe a retirada.") LocalDateTime dataInicio,
            @NotNull(message = "Informe a devolução.") LocalDateTime dataFim) {
    }

    private final ReservaRepository reservas;
    private final ClienteService clientes;
    private final VeiculoService veiculos;

    public ReservaService(ReservaRepository reservas, ClienteService clientes, VeiculoService veiculos) {
        this.reservas = reservas;
        this.clientes = clientes;
        this.veiculos = veiculos;
    }

    @Transactional(readOnly = true)
    public List<Reserva> listar(Usuario u) {
        return u.ehCliente()
                ? reservas.findByClienteIdOrderByDataInicioDesc(u.getCliente().getId())
                : reservas.findAllByOrderByDataInicioDesc();
    }

    public Reserva criar(Usuario u, Form f) {
        Long idCliente = u.ehCliente() ? u.getCliente().getId() : f.idCliente();
        if (idCliente == null) {
            throw ApiException.regra("Informe o cliente da reserva.");
        }
        if (!f.dataFim().isAfter(f.dataInicio())) {
            throw ApiException.regra("A devolução deve ser posterior à retirada.");
        }
        if (f.dataInicio().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw ApiException.regra("A retirada não pode estar no passado.");
        }
        Cliente cliente = clientes.obter(idCliente);
        Veiculo veiculo = veiculos.obter(f.idVeiculo());
        clientes.exigirCnhValida(cliente, f.dataFim());
        veiculos.exigirDisponivel(veiculo, f.dataInicio(), f.dataFim(), null);

        Reserva r = new Reserva();
        r.setCliente(cliente);
        r.setVeiculo(veiculo);
        r.setDataInicio(f.dataInicio());
        r.setDataFim(f.dataFim());
        r.setValorTotalPrevisto(Regras.valorEstimado(f.dataInicio(), f.dataFim(), veiculo.getValorDiaria()));
        r.setStatus(Reserva.Status.PENDENTE);
        return reservas.saveAndFlush(r);
    }

    public Reserva confirmar(Long id) {
        Reserva r = obter(id);
        if (r.getStatus() != Reserva.Status.PENDENTE) {
            throw ApiException.regra("Somente reservas pendentes podem ser confirmadas.");
        }
        if (r.getDataFim().isBefore(LocalDateTime.now())) {
            throw ApiException.regra("A reserva já expirou.");
        }
        clientes.exigirCnhValida(r.getCliente(), r.getDataFim());
        veiculos.exigirDisponivel(r.getVeiculo(), r.getDataInicio(), r.getDataFim(), r.getId());
        r.setStatus(Reserva.Status.CONFIRMADA);
        reservas.saveAndFlush(r);
        veiculos.recalcularStatus(r.getVeiculo());
        return r;
    }

    public Reserva cancelar(Usuario u, Long id) {
        Reserva r = obter(id);
        if (u.ehCliente() && !r.getCliente().getId().equals(u.getCliente().getId())) {
            throw ApiException.proibido("Você só pode cancelar as suas próprias reservas.");
        }
        if (r.getStatus() != Reserva.Status.PENDENTE && r.getStatus() != Reserva.Status.CONFIRMADA) {
            throw ApiException.regra("Somente reservas pendentes ou confirmadas podem ser canceladas.");
        }
        r.setStatus(Reserva.Status.CANCELADA);
        reservas.saveAndFlush(r);
        veiculos.recalcularStatus(r.getVeiculo());
        return r;
    }

    public Reserva obter(Long id) {
        return reservas.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Reserva"));
    }
}
