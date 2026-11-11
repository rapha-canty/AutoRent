package com.autorent.repository;

import com.autorent.entity.Locacao;
import com.autorent.entity.Reserva;
import com.autorent.entity.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    boolean existsByPlacaAndIdNot(String placa, Long id);

    long countByStatus(Veiculo.Status status);

    @Query("""
            select v from Veiculo v
            where v.status not in :inaptos
              and (lower(v.modelo) like lower(concat('%', :busca, '%')) or lower(v.marca) like lower(concat('%', :busca, '%')))
              and (:categoria is null or v.categoria = :categoria)
              and (:precoMin is null or v.valorDiaria >= :precoMin)
              and (:precoMax is null or v.valorDiaria <= :precoMax)
              and not exists (select 1 from Reserva r where r.veiculo = v and r.status = :confirmada
                              and r.dataInicio < :fim and r.dataFim > :inicio)
              and not exists (select 1 from Locacao l where l.veiculo = v and l.status in :abertas
                              and l.dataRetirada < :fim
                              and (l.dataPrevistaDevolucao > :inicio or (l.status = :atrasada and :agora > :inicio)))
            order by v.valorDiaria
            """)
    List<Veiculo> buscarDisponiveis(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim,
                                    @Param("agora") LocalDateTime agora, @Param("busca") String busca,
                                    @Param("categoria") Veiculo.Categoria categoria,
                                    @Param("precoMin") BigDecimal precoMin, @Param("precoMax") BigDecimal precoMax,
                                    @Param("inaptos") List<Veiculo.Status> inaptos,
                                    @Param("confirmada") Reserva.Status confirmada,
                                    @Param("abertas") List<Locacao.Status> abertas,
                                    @Param("atrasada") Locacao.Status atrasada);

    default List<Veiculo> disponiveis(LocalDateTime inicio, LocalDateTime fim, String busca,
                                      Veiculo.Categoria categoria, BigDecimal precoMin, BigDecimal precoMax) {
        return buscarDisponiveis(inicio, fim, LocalDateTime.now(), busca == null ? "" : busca, categoria, precoMin,
                precoMax, List.of(Veiculo.Status.MANUTENCAO, Veiculo.Status.INATIVO), Reserva.Status.CONFIRMADA,
                List.of(Locacao.Status.ATIVA, Locacao.Status.ATRASADA), Locacao.Status.ATRASADA);
    }
}
