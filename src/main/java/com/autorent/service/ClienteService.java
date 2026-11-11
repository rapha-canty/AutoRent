package com.autorent.service;

import com.autorent.config.ApiException;
import com.autorent.entity.Cliente;
import com.autorent.entity.Cliente.CnhSituacao;
import com.autorent.entity.Usuario;
import com.autorent.repository.ClienteRepository;
import com.autorent.repository.LocacaoRepository;
import com.autorent.repository.ReservaRepository;
import com.autorent.repository.UsuarioRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ClienteService {

    public record Form(
            @NotBlank(message = "Informe o nome completo.") @Size(max = 120) String nomeCompleto,
            @NotBlank(message = "Informe o CPF.") String cpf,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 120) String email,
            @NotBlank(message = "Informe o telefone.") @Size(max = 20) String telefone,
            @NotBlank(message = "Informe a CNH.") String cnhNumero,
            @NotNull(message = "Informe a validade da CNH.") LocalDate cnhValidade) {
    }

    public record Cadastro(
            @NotBlank(message = "Informe o nome completo.") @Size(max = 120) String nomeCompleto,
            @NotBlank(message = "Informe o CPF.") String cpf,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 120) String email,
            @NotBlank(message = "Informe o telefone.") @Size(max = 20) String telefone,
            @NotBlank(message = "Informe a CNH.") String cnhNumero,
            @NotNull(message = "Informe a validade da CNH.") LocalDate cnhValidade,
            @NotBlank(message = "Informe a senha.") @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.") String senha) {
    }

    private static final Map<String, CnhSituacao> BASE_CNH = new HashMap<>();

    static {
        for (int i = 1; i <= 7; i++) {
            BASE_CNH.put("0410000000" + i, CnhSituacao.VALIDA);
        }
        for (int i = 11; i <= 20; i++) {
            BASE_CNH.put("041000000" + i, CnhSituacao.VALIDA);
        }
        BASE_CNH.put("04100000008", CnhSituacao.VENCIDA);
        BASE_CNH.put("04100000009", CnhSituacao.IRREGULAR);
        BASE_CNH.put("99999999999", CnhSituacao.INEXISTENTE);
    }

    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;
    private final ReservaRepository reservas;
    private final LocacaoRepository locacoes;
    private final PasswordEncoder encoder;

    public ClienteService(ClienteRepository clientes, UsuarioRepository usuarios, ReservaRepository reservas,
                          LocacaoRepository locacoes, PasswordEncoder encoder) {
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.reservas = reservas;
        this.locacoes = locacoes;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clientes.findAll(Sort.by("nomeCompleto"));
    }

    public Cliente criar(Form f) {
        Cliente c = new Cliente();
        preencher(c, f, true);
        return verificarCnh(clientes.saveAndFlush(c));
    }

    public Cliente atualizar(Long id, Form f) {
        Cliente c = obter(id);
        preencher(c, f, true);
        return verificarCnh(clientes.saveAndFlush(c));
    }

    public void excluir(Long id) {
        Cliente c = obter(id);
        if (reservas.existsByClienteId(id) || locacoes.existsByClienteId(id)) {
            throw ApiException.conflito("O cliente possui reservas ou locações e não pode ser excluído.");
        }
        if (usuarios.findByClienteId(id).isPresent()) {
            throw ApiException.conflito("O cliente possui um usuário de acesso vinculado.");
        }
        clientes.delete(c);
    }

    @Transactional(readOnly = true)
    public Cliente meusDados(Usuario u) {
        return obter(idDoCliente(u));
    }

    public Cliente atualizarMeusDados(Usuario u, Form f) {
        Cliente c = obter(idDoCliente(u));
        preencher(c, f, false);
        return verificarCnh(clientes.saveAndFlush(c));
    }

    public Cliente cadastrar(Cadastro c) {
        String email = c.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflito("Já existe usuário com este e-mail.");
        }
        Cliente cliente = new Cliente();
        preencher(cliente, new Form(c.nomeCompleto(), c.cpf(), email, c.telefone(), c.cnhNumero(), c.cnhValidade()), true);
        cliente = verificarCnh(clientes.saveAndFlush(cliente));

        Usuario usuario = new Usuario();
        usuario.setNome(cliente.getNomeCompleto());
        usuario.setEmail(email);
        usuario.setSenhaHash(encoder.encode(c.senha()));
        usuario.setPerfil(Usuario.Perfil.CLIENTE);
        usuario.setCliente(cliente);
        usuarios.saveAndFlush(usuario);
        return cliente;
    }

    public Cliente obter(Long id) {
        return clientes.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Cliente"));
    }

    public Cliente verificarCnh(Cliente c) {
        CnhSituacao situacao = BASE_CNH.getOrDefault(c.getCnhNumero(), CnhSituacao.INEXISTENTE);
        if (situacao == CnhSituacao.VALIDA && c.getCnhValidade().isBefore(LocalDate.now())) {
            situacao = CnhSituacao.VENCIDA;
        }
        c.setCnhSituacao(situacao);
        c.setCnhVerificadaEm(LocalDateTime.now());
        return clientes.save(c);
    }

    public void exigirCnhValida(Cliente c, LocalDateTime fimPeriodo) {
        verificarCnh(c);
        if (c.getCnhSituacao() != CnhSituacao.VALIDA) {
            throw ApiException.regra("A CNH do cliente não está válida (situação: " + c.getCnhSituacao() + ").");
        }
        if (c.getCnhValidade().isBefore(fimPeriodo.toLocalDate())) {
            throw ApiException.regra("A CNH do cliente vence antes do fim do período.");
        }
    }

    private Long idDoCliente(Usuario u) {
        if (u.getCliente() == null) {
            throw ApiException.regra("O usuário não está vinculado a um cliente.");
        }
        return u.getCliente().getId();
    }

    private void preencher(Cliente c, Form f, boolean alteraCpf) {
        String cpf = alteraCpf || c.getCpf() == null ? Regras.somenteDigitos(f.cpf()) : c.getCpf();
        String cnh = Regras.somenteDigitos(f.cnhNumero());
        String email = f.email().trim().toLowerCase();
        Long id = c.getId() == null ? 0L : c.getId();

        if (!Regras.cpfValido(cpf)) {
            throw ApiException.regra("CPF inválido.");
        }
        if (cnh.length() != 11) {
            throw ApiException.regra("A CNH deve ter 11 dígitos.");
        }
        if (clientes.existsByCpfAndIdNot(cpf, id)) {
            throw ApiException.conflito("Já existe cliente com este CPF.");
        }
        if (clientes.existsByEmailAndIdNot(email, id)) {
            throw ApiException.conflito("Já existe cliente com este e-mail.");
        }
        if (clientes.existsByCnhNumeroAndIdNot(cnh, id)) {
            throw ApiException.conflito("Já existe cliente com esta CNH.");
        }
        c.setNomeCompleto(f.nomeCompleto().trim());
        c.setCpf(cpf);
        c.setEmail(email);
        c.setTelefone(f.telefone().trim());
        c.setCnhNumero(cnh);
        c.setCnhValidade(f.cnhValidade());
    }
}
