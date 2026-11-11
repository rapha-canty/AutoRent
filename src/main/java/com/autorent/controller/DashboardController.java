package com.autorent.controller;

import com.autorent.entity.Locacao;
import com.autorent.entity.Reserva;
import com.autorent.entity.Usuario;
import com.autorent.entity.Veiculo;
import com.autorent.repository.LocacaoRepository;
import com.autorent.repository.PagamentoRepository;
import com.autorent.repository.ReservaRepository;
import com.autorent.repository.VeiculoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final VeiculoRepository veiculos;
    private final ReservaRepository reservas;
    private final LocacaoRepository locacoes;
    private final PagamentoRepository pagamentos;

    public DashboardController(VeiculoRepository veiculos, ReservaRepository reservas, LocacaoRepository locacoes,
                               PagamentoRepository pagamentos) {
        this.veiculos = veiculos;
        this.reservas = reservas;
        this.locacoes = locacoes;
        this.pagamentos = pagamentos;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    @Transactional
    public Map<String, Object> resumo(@AuthenticationPrincipal Usuario u) {
        locacoes.marcarAtrasadas(LocalDateTime.now(), Locacao.Status.ATIVA, Locacao.Status.ATRASADA);
        LocalDate hoje = LocalDate.now();
        Map<String, Long> frota = new LinkedHashMap<>();
        for (Veiculo.Status s : Veiculo.Status.values()) {
            frota.put(s.name(), veiculos.countByStatus(s));
        }
        long total = veiculos.count() - frota.get(Veiculo.Status.INATIVO.name());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalVeiculos", total);
        m.put("frota", frota);
        m.put("reservasPendentes", reservas.countByStatus(Reserva.Status.PENDENTE));
        m.put("locacoesEmAndamento", locacoes.countByStatusIn(LocacaoRepository.ABERTAS));
        m.put("locacoesAtrasadas", locacoes.countByStatusIn(List.of(Locacao.Status.ATRASADA)));
        m.put("reservasRecentes", reservas.findTop5ByOrderByDataCriacaoDesc());
        m.put("devolucoesHoje", locacoes.findByStatusInAndDataPrevistaDevolucaoBetween(
                LocacaoRepository.ABERTAS, hoje.atStartOfDay(), hoje.plusDays(1).atStartOfDay()));
        m.put("locacoesPorMes", locacoesPorMes());
        m.put("receitaMes", u.getPerfil() == Usuario.Perfil.ADMIN
                ? pagamentos.receita(hoje.withDayOfMonth(1).atStartOfDay(), hoje.withDayOfMonth(1).plusMonths(1).atStartOfDay())
                : null);
        return m;
    }

    private Map<String, Long> locacoesPorMes() {
        LocalDate primeiro = LocalDate.now().withDayOfMonth(1).minusMonths(5);
        Map<String, Long> serie = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            serie.put(primeiro.plusMonths(i).toString().substring(0, 7), 0L);
        }
        for (Locacao l : locacoes.findByDataRetiradaAfter(primeiro.atStartOfDay())) {
            serie.merge(l.getDataRetirada().toLocalDate().toString().substring(0, 7), 1L, Long::sum);
        }
        return serie;
    }
}
