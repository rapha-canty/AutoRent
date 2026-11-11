package com.autorent.repository;

import com.autorent.entity.Locacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LocacaoRepository extends JpaRepository<Locacao, Long> {

    List<Locacao.Status> ABERTAS = List.of(Locacao.Status.ATIVA, Locacao.Status.ATRASADA);

    @Query("""
            select count(l) from Locacao l
            where l.veiculo.id = :veiculo and l.status in :abertas and l.id <> :ignoraLocacao
              and (l.reserva is null or l.reserva.id <> :ignoraReserva)
              and l.dataRetirada < :fim
              and (l.dataPrevistaDevolucao > :inicio or (l.status = :atrasada and :agora > :inicio))
            """)
    long contarAbertas(@Param("veiculo") Long veiculo, @Param("inicio") LocalDateTime inicio,
                       @Param("fim") LocalDateTime fim, @Param("agora") LocalDateTime agora,
                       @Param("ignoraReserva") Long ignoraReserva, @Param("ignoraLocacao") Long ignoraLocacao,
                       @Param("abertas") List<Locacao.Status> abertas, @Param("atrasada") Locacao.Status atrasada);

    default long conflitosAbertas(Long veiculo, LocalDateTime inicio, LocalDateTime fim, Long ignoraReserva) {
        return contarAbertas(veiculo, inicio, fim, LocalDateTime.now(), ignoraReserva == null ? 0L : ignoraReserva, 0L,
                ABERTAS, Locacao.Status.ATRASADA);
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Locacao l set l.status = :atrasada where l.status = :ativa and l.dataPrevistaDevolucao < :agora")
    int marcarAtrasadas(@Param("agora") LocalDateTime agora, @Param("ativa") Locacao.Status ativa,
                        @Param("atrasada") Locacao.Status atrasada);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Locacao l where l.id = :id")
    Optional<Locacao> buscarParaPagamento(@Param("id") Long id);

    List<Locacao> findAllByOrderByDataRetiradaDesc();

    List<Locacao> findByClienteIdOrderByDataRetiradaDesc(Long clienteId);

    List<Locacao> findByStatusInAndDataPrevistaDevolucaoBetween(Collection<Locacao.Status> status,
                                                                LocalDateTime de, LocalDateTime ate);

    boolean existsByClienteId(Long clienteId);

    boolean existsByVeiculoId(Long veiculoId);

    boolean existsByReservaId(Long reservaId);

    boolean existsByVeiculoIdAndStatusIn(Long veiculoId, Collection<Locacao.Status> status);

    long countByStatusIn(Collection<Locacao.Status> status);

    List<Locacao> findByDataRetiradaAfter(LocalDateTime data);
}
