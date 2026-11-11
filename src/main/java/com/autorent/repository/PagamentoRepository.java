package com.autorent.repository;

import com.autorent.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    @Query("""
            select coalesce(sum(p.valor), 0) from Pagamento p
            where p.locacao.id = :locacao and p.status in :recebidos
            """)
    BigDecimal somarRecebidos(@Param("locacao") Long locacao, @Param("recebidos") List<Pagamento.Status> recebidos);

    default BigDecimal totalRecebido(Long locacao) {
        return somarRecebidos(locacao, List.of(Pagamento.Status.PAGO, Pagamento.Status.PARCIAL));
    }

    @Query("""
            select coalesce(sum(p.valor), 0) from Pagamento p
            where p.status in :recebidos and p.dataPagamento >= :de and p.dataPagamento < :ate
            """)
    BigDecimal somarPeriodo(@Param("recebidos") List<Pagamento.Status> recebidos, @Param("de") LocalDateTime de,
                            @Param("ate") LocalDateTime ate);

    default BigDecimal receita(LocalDateTime de, LocalDateTime ate) {
        return somarPeriodo(List.of(Pagamento.Status.PAGO, Pagamento.Status.PARCIAL), de, ate);
    }

    List<Pagamento> findAllByOrderByDataPagamentoDesc();

    List<Pagamento> findByLocacaoClienteIdOrderByDataPagamentoDesc(Long clienteId);

    boolean existsByLocacaoIdAndValorAndFormaPagamentoAndDataPagamentoAfter(Long locacaoId, BigDecimal valor,
                                                                            Pagamento.Forma forma, LocalDateTime depois);
}
