package com.autorent.repository;

import com.autorent.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("""
            select count(r) from Reserva r
            where r.veiculo.id = :veiculo and r.status = :status and r.id <> :ignora
              and r.dataInicio < :fim and r.dataFim > :inicio
            """)
    long contarPorStatus(@Param("veiculo") Long veiculo, @Param("status") Reserva.Status status,
                         @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim,
                         @Param("ignora") Long ignora);

    default long conflitosConfirmadas(Long veiculo, LocalDateTime inicio, LocalDateTime fim, Long ignora) {
        return contarPorStatus(veiculo, Reserva.Status.CONFIRMADA, inicio, fim, ignora == null ? 0L : ignora);
    }

    List<Reserva> findAllByOrderByDataInicioDesc();

    List<Reserva> findByClienteIdOrderByDataInicioDesc(Long clienteId);

    List<Reserva> findByStatusOrderByDataInicio(Reserva.Status status);

    boolean existsByClienteId(Long clienteId);

    boolean existsByVeiculoId(Long veiculoId);

    boolean existsByVeiculoIdAndStatusIn(Long veiculoId, Collection<Reserva.Status> status);

    boolean existsByVeiculoIdAndStatusAndDataFimAfter(Long veiculoId, Reserva.Status status, LocalDateTime data);

    long countByStatus(Reserva.Status status);

    List<Reserva> findTop5ByOrderByDataCriacaoDesc();
}
