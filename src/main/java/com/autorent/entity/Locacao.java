package com.autorent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "locacao")
@Getter
@Setter
public class Locacao {

    public enum Status { ATIVA, ATRASADA, FINALIZADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_locacao")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_reserva")
    private Reserva reserva;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_veiculo")
    private Veiculo veiculo;

    @Column(name = "data_retirada")
    private LocalDateTime dataRetirada;

    @Column(name = "data_prevista_devolucao")
    private LocalDateTime dataPrevistaDevolucao;

    @Column(name = "data_devolucao_efetiva")
    private LocalDateTime dataDevolucaoEfetiva;

    @Column(name = "km_retirada")
    private Integer kmRetirada;

    @Column(name = "km_devolucao")
    private Integer kmDevolucao;

    @Column(name = "valor_diaria")
    private BigDecimal valorDiaria;

    @Column(name = "valor_total")
    private BigDecimal valorTotal;

    @Column(name = "valor_multa")
    private BigDecimal valorMulta = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ATIVA;

    @Transient
    private BigDecimal multaAcumulada;

    public BigDecimal getValorDevido() {
        BigDecimal multa = multaAcumulada != null ? multaAcumulada : valorMulta;
        return valorTotal.add(multa);
    }
}
