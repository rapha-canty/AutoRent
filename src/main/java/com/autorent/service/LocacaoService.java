package com.autorent.service;

import com.autorent.config.ApiException;
import com.autorent.entity.Cliente;
import com.autorent.entity.Locacao;
import com.autorent.entity.Reserva;
import com.autorent.entity.Usuario;
import com.autorent.entity.Veiculo;
import com.autorent.repository.LocacaoRepository;
import com.autorent.repository.VeiculoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class LocacaoService {

    public record Retirada(Long idReserva, Long idCliente, Long idVeiculo, LocalDateTime dataPrevistaDevolucao,
                           Integer kmRetirada) {
    }

    private static final long ANTECEDENCIA_RETIRADA_MIN = 120;

    private final LocacaoRepository locacoes;
    private final VeiculoRepository veiculos;
    private final ReservaService reservas;
    private final ClienteService clientes;
    private final VeiculoService veiculoService;
    private final BigDecimal fatorMulta;
    private final long toleranciaMinutos;

    public LocacaoService(LocacaoRepository locacoes, VeiculoRepository veiculos, ReservaService reservas,
                          ClienteService clientes, VeiculoService veiculoService,
                          @Value("${autorent.multa.fator-diaria}") BigDecimal fatorMulta,
                          @Value("${autorent.multa.tolerancia-minutos}") long toleranciaMinutos) {
        this.locacoes = locacoes;
        this.veiculos = veiculos;
        this.reservas = reservas;
        this.clientes = clientes;
        this.veiculoService = veiculoService;
        this.fatorMulta = fatorMulta;
        this.toleranciaMinutos = toleranciaMinutos;
    }

    public List<Locacao> listar(Usuario u) {
        locacoes.marcarAtrasadas(LocalDateTime.now(), Locacao.Status.ATIVA, Locacao.Status.ATRASADA);
        List<Locacao> lista = u.ehCliente()
                ? locacoes.findByClienteIdOrderByDataRetiradaDesc(u.getCliente().getId())
                : locacoes.findAllByOrderByDataRetiradaDesc();
        lista.forEach(this::comMultaAtual);
        return lista;
    }

    public Locacao comMultaAtual(Locacao l) {
        if (l.getStatus() == Locacao.Status.ATRASADA) {
            l.setMultaAcumulada(Regras.multaAtraso(l.getDataPrevistaDevolucao(), LocalDateTime.now(),
                    l.getValorDiaria(), fatorMulta, toleranciaMinutos));
        }
        return l;
    }

    public Locacao obterAutorizada(Usuario u, Long id) {
        Locacao l = locacoes.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Locação"));
        if (u.ehCliente() && !l.getCliente().getId().equals(u.getCliente().getId())) {
            throw ApiException.proibido("Você só pode consultar as suas próprias locações.");
        }
        return comMultaAtual(l);
    }

    public Locacao retirar(Retirada r) {
        LocalDateTime agora = LocalDateTime.now();
        Reserva reserva = null;
        Cliente cliente;
        Veiculo veiculo;
        LocalDateTime prevista;

        if (r.idReserva() != null) {
            reserva = reservas.obter(r.idReserva());
            if (reserva.getStatus() != Reserva.Status.CONFIRMADA) {
                throw ApiException.regra("A retirada exige uma reserva confirmada.");
            }
            if (locacoes.existsByReservaId(reserva.getId())) {
                throw ApiException.conflito("Esta reserva já possui locação.");
            }
            if (agora.isBefore(reserva.getDataInicio().minusMinutes(ANTECEDENCIA_RETIRADA_MIN))) {
                throw ApiException.regra("Ainda não é possível retirar: a reserva começa em " + reserva.getDataInicio() + ".");
            }
            if (agora.isAfter(reserva.getDataFim())) {
                throw ApiException.regra("O período da reserva já terminou.");
            }
            cliente = reserva.getCliente();
            veiculo = reserva.getVeiculo();
            prevista = reserva.getDataFim();
        } else {
            if (r.idCliente() == null || r.idVeiculo() == null || r.dataPrevistaDevolucao() == null) {
                throw ApiException.regra("Sem reserva, informe cliente, veículo e devolução prevista.");
            }
            if (!r.dataPrevistaDevolucao().isAfter(agora)) {
                throw ApiException.regra("A devolução prevista deve ser posterior à retirada.");
            }
            cliente = clientes.obter(r.idCliente());
            veiculo = veiculoService.obter(r.idVeiculo());
            prevista = r.dataPrevistaDevolucao();
        }

        if (veiculo.getStatus() == Veiculo.Status.ALUGADO) {
            throw ApiException.conflito("O veículo já está alugado.");
        }
        clientes.exigirCnhValida(cliente, prevista);
        veiculoService.exigirDisponivel(veiculo, agora, prevista, reserva == null ? null : reserva.getId());

        int km = r.kmRetirada() == null ? veiculo.getQuilometragem() : r.kmRetirada();
        if (km < veiculo.getQuilometragem()) {
            throw ApiException.regra("A quilometragem de retirada não pode ser menor que a do veículo (" + veiculo.getQuilometragem() + ").");
        }

        Locacao l = new Locacao();
        l.setReserva(reserva);
        l.setCliente(cliente);
        l.setVeiculo(veiculo);
        l.setDataRetirada(agora);
        l.setDataPrevistaDevolucao(prevista);
        l.setKmRetirada(km);
        l.setValorDiaria(veiculo.getValorDiaria());
        l.setValorTotal(reserva != null ? reserva.getValorTotalPrevisto()
                : Regras.valorEstimado(agora, prevista, veiculo.getValorDiaria()));
        l.setValorMulta(BigDecimal.ZERO.setScale(2));
        l.setStatus(Locacao.Status.ATIVA);
        l = locacoes.saveAndFlush(l);

        if (reserva != null) {
            reserva.setStatus(Reserva.Status.CONCLUIDA);
        }
        veiculo.setQuilometragem(km);
        veiculo.setStatus(Veiculo.Status.ALUGADO);
        veiculos.save(veiculo);
        return l;
    }

    public Locacao devolver(Long id, Integer kmDevolucao) {
        Locacao l = locacoes.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Locação"));
        if (l.getStatus() == Locacao.Status.FINALIZADA) {
            throw ApiException.conflito("A devolução desta locação já foi registrada.");
        }
        if (kmDevolucao == null || kmDevolucao < l.getKmRetirada()) {
            throw ApiException.regra("A quilometragem de devolução não pode ser menor que a de retirada (" + l.getKmRetirada() + ").");
        }
        LocalDateTime agora = LocalDateTime.now();
        l.setDataDevolucaoEfetiva(agora);
        l.setKmDevolucao(kmDevolucao);
        l.setValorMulta(Regras.multaAtraso(l.getDataPrevistaDevolucao(), agora, l.getValorDiaria(), fatorMulta, toleranciaMinutos));
        l.setStatus(Locacao.Status.FINALIZADA);
        locacoes.saveAndFlush(l);

        Veiculo v = l.getVeiculo();
        v.setQuilometragem(kmDevolucao);
        veiculos.save(v);
        veiculoService.recalcularStatus(v);
        return l;
    }
}
