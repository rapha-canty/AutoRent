package com.autorent.service;

import com.autorent.config.ApiException;
import com.autorent.entity.Reserva;
import com.autorent.entity.Veiculo;
import com.autorent.repository.LocacaoRepository;
import com.autorent.repository.ReservaRepository;
import com.autorent.repository.VeiculoRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class VeiculoService {

    public record Form(
            @NotBlank(message = "Informe a placa.") String placa,
            @NotBlank(message = "Informe a marca.") String marca,
            @NotBlank(message = "Informe o modelo.") String modelo,
            @NotNull(message = "Informe o ano.") @Min(value = 1990, message = "Ano inválido.") @Max(value = 2100, message = "Ano inválido.") Integer ano,
            @NotBlank(message = "Informe a cor.") String cor,
            @NotNull(message = "Informe a categoria.") Veiculo.Categoria categoria,
            @NotNull(message = "Informe a quilometragem.") @Min(value = 0, message = "Quilometragem inválida.") Integer quilometragem,
            @NotNull(message = "Informe a diária.") @DecimalMin(value = "0.01", message = "A diária deve ser maior que zero.") BigDecimal valorDiaria,
            Veiculo.Status status) {
    }

    private final VeiculoRepository veiculos;
    private final ReservaRepository reservas;
    private final LocacaoRepository locacoes;

    public VeiculoService(VeiculoRepository veiculos, ReservaRepository reservas, LocacaoRepository locacoes) {
        this.veiculos = veiculos;
        this.reservas = reservas;
        this.locacoes = locacoes;
    }

    @Transactional(readOnly = true)
    public List<Veiculo> listar() {
        return veiculos.findAll(Sort.by("marca", "modelo"));
    }

    @Transactional(readOnly = true)
    public List<Veiculo> disponiveis(LocalDateTime inicio, LocalDateTime fim, String busca, Veiculo.Categoria categoria,
                                     BigDecimal precoMin, BigDecimal precoMax) {
        if (!fim.isAfter(inicio)) {
            throw ApiException.regra("A devolução deve ser posterior à retirada.");
        }
        return veiculos.disponiveis(inicio, fim, busca, categoria, precoMin, precoMax);
    }

    public Veiculo criar(Form f) {
        Veiculo v = new Veiculo();
        preencher(v, f);
        v.setStatus(f.status() == null ? Veiculo.Status.DISPONIVEL : statusManual(f.status()));
        return veiculos.saveAndFlush(v);
    }

    public Veiculo atualizar(Long id, Form f) {
        Veiculo v = obter(id);
        preencher(v, f);
        if (f.status() != null && f.status() != v.getStatus()) {
            if (v.getStatus() == Veiculo.Status.ALUGADO) {
                throw ApiException.conflito("O veículo está alugado; registre a devolução antes de alterar o status.");
            }
            v.setStatus(statusManual(f.status()));
        }
        return veiculos.saveAndFlush(v);
    }

    public String excluir(Long id) {
        Veiculo v = obter(id);
        if (locacoes.existsByVeiculoIdAndStatusIn(id, LocacaoRepository.ABERTAS)
                || reservas.existsByVeiculoIdAndStatusIn(id, List.of(Reserva.Status.PENDENTE, Reserva.Status.CONFIRMADA))) {
            throw ApiException.conflito("O veículo possui locação em andamento ou reservas em aberto.");
        }
        if (reservas.existsByVeiculoId(id) || locacoes.existsByVeiculoId(id)) {
            v.setStatus(Veiculo.Status.INATIVO);
            veiculos.save(v);
            return "Veículo com histórico: foi desativado (inativo).";
        }
        veiculos.delete(v);
        return "Veículo excluído.";
    }

    public Veiculo obter(Long id) {
        return veiculos.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Veículo"));
    }

    public void exigirDisponivel(Veiculo v, LocalDateTime inicio, LocalDateTime fim, Long ignoraReserva) {
        if (v.getStatus() == Veiculo.Status.MANUTENCAO || v.getStatus() == Veiculo.Status.INATIVO) {
            throw ApiException.regra("O veículo não está apto para locação (manutenção ou inativo).");
        }
        if (reservas.conflitosConfirmadas(v.getId(), inicio, fim, ignoraReserva) > 0) {
            throw ApiException.conflito("Já existe reserva confirmada para o veículo neste período.");
        }
        if (locacoes.conflitosAbertas(v.getId(), inicio, fim, ignoraReserva) > 0) {
            throw ApiException.conflito("O veículo está em locação neste período.");
        }
    }

    public void recalcularStatus(Veiculo v) {
        if (v.getStatus() == Veiculo.Status.MANUTENCAO || v.getStatus() == Veiculo.Status.INATIVO) {
            return;
        }
        Veiculo.Status novo;
        if (locacoes.existsByVeiculoIdAndStatusIn(v.getId(), LocacaoRepository.ABERTAS)) {
            novo = Veiculo.Status.ALUGADO;
        } else if (reservas.existsByVeiculoIdAndStatusAndDataFimAfter(v.getId(), Reserva.Status.CONFIRMADA, LocalDateTime.now())) {
            novo = Veiculo.Status.RESERVADO;
        } else {
            novo = Veiculo.Status.DISPONIVEL;
        }
        if (novo != v.getStatus()) {
            v.setStatus(novo);
            veiculos.save(v);
        }
    }

    private Veiculo.Status statusManual(Veiculo.Status s) {
        if (s == Veiculo.Status.RESERVADO || s == Veiculo.Status.ALUGADO) {
            throw ApiException.regra("Os status RESERVADO e ALUGADO são definidos pelo sistema.");
        }
        return s;
    }

    private void preencher(Veiculo v, Form f) {
        String placa = f.placa().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (!Regras.placaValida(placa)) {
            throw ApiException.regra("Placa inválida. Use o formato ABC1234 ou ABC1D23.");
        }
        if (veiculos.existsByPlacaAndIdNot(placa, v.getId() == null ? 0L : v.getId())) {
            throw ApiException.conflito("Já existe veículo com esta placa.");
        }
        v.setPlaca(placa);
        v.setMarca(f.marca().trim());
        v.setModelo(f.modelo().trim());
        v.setAno(f.ano());
        v.setCor(f.cor().trim());
        v.setCategoria(f.categoria());
        v.setQuilometragem(f.quilometragem());
        v.setValorDiaria(f.valorDiaria());
    }
}
