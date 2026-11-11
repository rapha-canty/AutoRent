package com.autorent.service;

import com.autorent.config.ApiException;
import com.autorent.entity.Locacao;
import com.autorent.entity.Pagamento;
import com.autorent.entity.Usuario;
import com.autorent.repository.LocacaoRepository;
import com.autorent.repository.PagamentoRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PagamentoService {

    public record Form(
            @NotNull(message = "Informe a locação.") Long idLocacao,
            @NotNull(message = "Informe o valor.") @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.") BigDecimal valor,
            @NotNull(message = "Informe a forma de pagamento.") Pagamento.Forma formaPagamento,
            String observacao) {
    }

    private final PagamentoRepository pagamentos;
    private final LocacaoRepository locacoes;
    private final LocacaoService locacaoService;

    public PagamentoService(PagamentoRepository pagamentos, LocacaoRepository locacoes, LocacaoService locacaoService) {
        this.pagamentos = pagamentos;
        this.locacoes = locacoes;
        this.locacaoService = locacaoService;
    }

    @Transactional(readOnly = true)
    public List<Pagamento> listar(Usuario u) {
        return u.ehCliente()
                ? pagamentos.findByLocacaoClienteIdOrderByDataPagamentoDesc(u.getCliente().getId())
                : pagamentos.findAllByOrderByDataPagamentoDesc();
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> saldo(Usuario u, Long idLocacao) {
        Locacao l = locacaoService.obterAutorizada(u, idLocacao);
        BigDecimal pago = pagamentos.totalRecebido(idLocacao);
        return Map.of("valorDevido", l.getValorDevido(), "valorPago", pago, "saldo", l.getValorDevido().subtract(pago));
    }

    public Pagamento registrar(Form f) {
        Locacao l = locacoes.buscarParaPagamento(f.idLocacao()).orElseThrow(() -> ApiException.naoEncontrado("Locação"));
        locacaoService.comMultaAtual(l);
        BigDecimal saldo = l.getValorDevido().subtract(pagamentos.totalRecebido(l.getId()));
        if (saldo.signum() <= 0) {
            throw ApiException.regra("Esta locação já está quitada.");
        }
        if (f.valor().compareTo(saldo) > 0) {
            throw ApiException.regra("O valor excede o saldo devedor da locação (R$ " + saldo + ").");
        }
        if (pagamentos.existsByLocacaoIdAndValorAndFormaPagamentoAndDataPagamentoAfter(l.getId(), f.valor(),
                f.formaPagamento(), LocalDateTime.now().minusSeconds(15))) {
            throw ApiException.conflito("Pagamento idêntico registrado há instantes. Aguarde antes de repetir.");
        }
        Pagamento p = new Pagamento();
        p.setLocacao(l);
        p.setValor(f.valor());
        p.setFormaPagamento(f.formaPagamento());
        p.setObservacao(f.observacao());
        p.setStatus(f.valor().compareTo(saldo) == 0 ? Pagamento.Status.PAGO : Pagamento.Status.PARCIAL);
        return pagamentos.saveAndFlush(p);
    }

    public Pagamento estornar(Long id) {
        Pagamento p = pagamentos.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Pagamento"));
        if (p.getStatus() != Pagamento.Status.PAGO && p.getStatus() != Pagamento.Status.PARCIAL) {
            throw ApiException.regra("Somente pagamentos pagos ou parciais podem ser estornados.");
        }
        p.setStatus(Pagamento.Status.ESTORNADO);
        return pagamentos.saveAndFlush(p);
    }
}
