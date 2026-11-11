package com.autorent.repository;

import com.autorent.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByCnhNumeroAndIdNot(String cnhNumero, Long id);
}
