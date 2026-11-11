package com.autorent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "veiculo")
@Getter
@Setter
public class Veiculo {

    public enum Categoria { ECONOMICO, COMPACTO, SEDAN, SUV, PICAPE, LUXO }

    public enum Status { DISPONIVEL, RESERVADO, ALUGADO, MANUTENCAO, INATIVO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_veiculo")
    private Long id;

    private String placa;
    private String marca;
    private String modelo;
    private Integer ano;
    private String cor;

    @Enumerated(EnumType.STRING)
    private Categoria categoria;

    @Column(name = "valor_diaria")
    private BigDecimal valorDiaria;

    private Integer quilometragem = 0;

    @Enumerated(EnumType.STRING)
    private Status status = Status.DISPONIVEL;

    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro = LocalDateTime.now();
}
