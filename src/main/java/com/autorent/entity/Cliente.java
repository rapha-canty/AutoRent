package com.autorent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cliente")
@Getter
@Setter
public class Cliente {

    public enum CnhSituacao { NAO_VERIFICADA, VALIDA, VENCIDA, INEXISTENTE, IRREGULAR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id;

    @Column(name = "nome_completo")
    private String nomeCompleto;

    private String cpf;
    private String email;
    private String telefone;

    @Column(name = "cnh_numero")
    private String cnhNumero;

    @Column(name = "cnh_validade")
    private LocalDate cnhValidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "cnh_situacao")
    private CnhSituacao cnhSituacao = CnhSituacao.NAO_VERIFICADA;

    @Column(name = "cnh_verificada_em")
    private LocalDateTime cnhVerificadaEm;

    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro = LocalDateTime.now();
}
