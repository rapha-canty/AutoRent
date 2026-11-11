package com.autorent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamento")
@Getter
@Setter
public class Pagamento {

    public enum Forma { PIX, CARTAO_CREDITO, CARTAO_DEBITO, DINHEIRO }

    public enum Status { PENDENTE, PAGO, PARCIAL, ESTORNADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pagamento")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_locacao")
    private Locacao locacao;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento = LocalDateTime.now();

    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento")
    private Forma formaPagamento;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDENTE;

    private String observacao;
}
