package com.autorent;

import com.autorent.entity.Cliente;
import com.autorent.entity.Usuario;
import com.autorent.repository.ClienteRepository;
import com.autorent.repository.UsuarioRepository;
import com.autorent.service.Regras;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:autorent_simples;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AutorentTest {

    static final String[] ADMIN = {"admin@teste.com", "Admin@123"};
    static final String[] VENDEDOR = {"vendedor@teste.com", "Vendedor@123"};
    static final String[] CLIENTE = {"cliente@teste.com", "Cliente@123"};

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired ClienteRepository clientes;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void criarUsuarios() {
        if (usuarios.count() > 0) {
            return;
        }
        usuario("Admin", ADMIN, Usuario.Perfil.ADMIN, null);
        usuario("Vendedor", VENDEDOR, Usuario.Perfil.VENDEDOR, null);
        Cliente c = new Cliente();
        c.setNomeCompleto("Cliente Portal");
        c.setCpf("39053344705");
        c.setEmail(CLIENTE[0]);
        c.setTelefone("11999990000");
        c.setCnhNumero("04100000001");
        c.setCnhValidade(LocalDate.now().plusYears(3));
        c.setCnhSituacao(Cliente.CnhSituacao.VALIDA);
        usuario("Cliente Portal", CLIENTE, Usuario.Perfil.CLIENTE, clientes.save(c));
    }

    private void usuario(String nome, String[] cred, Usuario.Perfil perfil, Cliente cliente) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(cred[0]);
        u.setSenhaHash(encoder.encode(cred[1]));
        u.setPerfil(perfil);
        u.setCliente(cliente);
        usuarios.save(u);
    }

    @Test
    void regrasDeCalculoEDocumentos() {
        assertTrue(Regras.cpfValido("390.533.447-05"));
        assertFalse(Regras.cpfValido("11144477736"));
        assertFalse(Regras.cpfValido("11111111111"));
        assertTrue(Regras.placaValida("ABC1D23"));
        assertFalse(Regras.placaValida("AB12345"));
        LocalDateTime ini = LocalDateTime.of(2026, 11, 1, 10, 0);
        assertEquals(1, Regras.diarias(ini, ini.plusHours(2)));
        assertEquals(2, Regras.diarias(ini, ini.plusHours(25)));
        assertEquals(new BigDecimal("540.00"), Regras.valorEstimado(ini, ini.plusDays(4), new BigDecimal("135")));
        assertEquals(0, Regras.multaAtraso(ini, ini.plusMinutes(60), new BigDecimal("100"), BigDecimal.ONE, 60).signum());
        assertEquals(new BigDecimal("200.00"), Regras.multaAtraso(ini, ini.plusHours(28), new BigDecimal("100"), BigDecimal.ONE, 60));
    }

    @Test
    void autenticacaoEPermissoesPorPerfil() throws Exception {
        assertEquals(401, status(HttpMethod.GET, "/api/clientes", null, null));
        assertEquals(401, status(HttpMethod.GET, "/api/clientes", null, new String[]{"admin@teste.com", "errada"}));
        assertEquals(200, status(HttpMethod.GET, "/api/clientes", null, ADMIN));
        assertEquals(200, status(HttpMethod.GET, "/api/clientes", null, VENDEDOR));
        assertEquals(403, status(HttpMethod.GET, "/api/clientes", null, CLIENTE));
        assertEquals(403, status(HttpMethod.POST, "/api/veiculos", veiculo("PRM1A11", "X"), VENDEDOR));
        assertEquals(403, status(HttpMethod.GET, "/api/usuarios", null, VENDEDOR));
        assertEquals(403, status(HttpMethod.GET, "/api/dashboard", null, CLIENTE));
        assertEquals(200, status(HttpMethod.GET, "/api/usuarios", null, ADMIN));
        assertEquals("CLIENTE", ler(executar(HttpMethod.GET, "/api/eu", null, CLIENTE)).get("perfil").asText());
    }

    @Test
    void tresSenhasErradasBloqueiamOLoginMesmoComSenhaCorreta() throws Exception {
        usuario("Alvo", new String[]{"alvo@teste.com", "Alvo@12345"}, Usuario.Perfil.VENDEDOR, null);
        String[] errada = {"alvo@teste.com", "errada"};
        String[] correta = {"alvo@teste.com", "Alvo@12345"};
        for (int i = 0; i < 3; i++) {
            assertEquals(401, status(HttpMethod.GET, "/api/eu", null, errada));
        }
        MvcResult bloqueado = executar(HttpMethod.GET, "/api/eu", null, correta);
        assertEquals(429, bloqueado.getResponse().getStatus());
        assertNotNull(bloqueado.getResponse().getHeader("Retry-After"));
        assertTrue(bloqueado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("Tente novamente"));

        MockHttpServletRequestBuilder outroIp = MockMvcRequestBuilders.get("/api/eu").with(httpBasic(correta[0], correta[1]))
                .with(req -> {
                    req.setRemoteAddr("10.9.8.7");
                    return req;
                });
        assertEquals(200, mvc.perform(outroIp).andReturn().getResponse().getStatus());
        assertEquals(200, status(HttpMethod.GET, "/api/eu", null, ADMIN));
    }

    @Test
    void acertoAntesDoLimiteZeraAsFalhas() throws Exception {
        String[] errada = {"vendedor@teste.com", "errada"};
        for (int i = 0; i < 2; i++) {
            assertEquals(401, status(HttpMethod.GET, "/api/eu", null, errada));
        }
        assertEquals(200, status(HttpMethod.GET, "/api/eu", null, VENDEDOR));
        for (int i = 0; i < 2; i++) {
            assertEquals(401, status(HttpMethod.GET, "/api/eu", null, errada));
        }
        assertEquals(200, status(HttpMethod.GET, "/api/eu", null, VENDEDOR));
    }

    @Test
    void dashboardCalculadoDoBancoEReceitaSoParaAdmin() throws Exception {
        MvcResult admin = executar(HttpMethod.GET, "/api/dashboard", null, ADMIN);
        assertEquals(200, admin.getResponse().getStatus());
        assertFalse(ler(admin).get("receitaMes").isNull());
        MvcResult vendedor = executar(HttpMethod.GET, "/api/dashboard", null, VENDEDOR);
        assertEquals(200, vendedor.getResponse().getStatus());
        assertTrue(ler(vendedor).get("receitaMes").isNull());
        assertEquals(6, ler(vendedor).get("locacoesPorMes").size());
    }

    @Test
    void crudDeVeiculo() throws Exception {
        long id = criar("/api/veiculos", veiculo("CRD1A11", "Kwid"), ADMIN);
        assertEquals(409, status(HttpMethod.POST, "/api/veiculos", veiculo("CRD1A11", "Outro"), ADMIN));
        assertEquals(422, status(HttpMethod.POST, "/api/veiculos", veiculo("INVALIDA", "X"), ADMIN));
        assertEquals(400, status(HttpMethod.POST, "/api/veiculos", "{}", ADMIN));
        assertEquals(200, status(HttpMethod.PUT, "/api/veiculos/" + id, veiculo("CRD1A11", "Kwid Zen"), ADMIN));
        assertTrue(executar(HttpMethod.GET, "/api/veiculos", null, ADMIN).getResponse().getContentAsString().contains("Kwid Zen"));
        assertTrue(executar(HttpMethod.DELETE, "/api/veiculos/" + id, null, ADMIN).getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("excluído"));
    }

    @Test
    void crudDeClienteComValidacoes() throws Exception {
        long id = criar("/api/clientes", cliente("11144477735", "crud@teste.com", "04100000002"), VENDEDOR);
        assertEquals(409, status(HttpMethod.POST, "/api/clientes", cliente("11144477735", "outro@teste.com", "04100000003"), VENDEDOR));
        assertEquals(422, status(HttpMethod.POST, "/api/clientes", cliente("11144477736", "x@teste.com", "04100000003"), VENDEDOR));
        assertEquals(200, status(HttpMethod.PUT, "/api/clientes/" + id, cliente("11144477735", "crud@teste.com", "04100000002"), VENDEDOR));
        assertEquals(403, status(HttpMethod.DELETE, "/api/clientes/" + id, null, VENDEDOR));
        assertEquals(204, status(HttpMethod.DELETE, "/api/clientes/" + id, null, ADMIN));
    }

    @Test
    void cadastroPublicoCriaApenasClienteEPermiteLogin() throws Exception {
        String corpo = "{\"nomeCompleto\":\"Nova Cliente\",\"cpf\":\"52998224725\",\"email\":\"nova@teste.com\",\"telefone\":\"11988887777\","
                + "\"cnhNumero\":\"04100000011\",\"cnhValidade\":\"" + LocalDate.now().plusYears(3) + "\",\"senha\":\"Senha@1234\",\"perfil\":\"ADMIN\"}";
        assertEquals(201, status(HttpMethod.POST, "/api/cadastro", corpo, null));
        assertEquals("CLIENTE", ler(executar(HttpMethod.GET, "/api/eu", null, new String[]{"nova@teste.com", "Senha@1234"})).get("perfil").asText());
    }

    @Test
    void fluxoReservaRetiradaPagamentoDevolucao() throws Exception {
        long veiculo = criar("/api/veiculos", veiculo("FLX1A11", "Polo"), ADMIN);
        long cliente1 = criar("/api/clientes", cliente("16899535009", "flx1@teste.com", "04100000004"), VENDEDOR);
        long cliente2 = criar("/api/clientes", cliente("93541134780", "flx2@teste.com", "04100000005"), VENDEDOR);
        long semCnh = criar("/api/clientes", cliente("74697131401", "flx3@teste.com", "55555555555"), VENDEDOR);
        LocalDateTime ini = LocalDateTime.now().plusHours(1).withNano(0);
        LocalDateTime fim = ini.plusDays(3);

        assertEquals(422, status(HttpMethod.POST, "/api/reservas", reserva(semCnh, veiculo, ini, fim), VENDEDOR));
        assertEquals(422, status(HttpMethod.POST, "/api/reservas", reserva(cliente1, veiculo, fim, ini), VENDEDOR));

        long reserva = criar("/api/reservas", reserva(cliente1, veiculo, ini, fim), VENDEDOR);
        assertEquals(390.0, valorDaReserva(reserva));
        assertEquals(200, status(HttpMethod.PATCH, "/api/reservas/" + reserva + "/confirmar", null, VENDEDOR));
        assertEquals(409, status(HttpMethod.POST, "/api/reservas", reserva(cliente2, veiculo, ini.plusDays(1), fim.plusDays(1)), VENDEDOR));

        long locacao = criar("/api/locacoes/retirada", "{\"idReserva\":" + reserva + ",\"kmRetirada\":1000}", VENDEDOR);
        assertEquals("ALUGADO", statusVeiculo(veiculo));

        assertEquals(422, status(HttpMethod.POST, "/api/locacoes/" + locacao + "/devolucao", "{\"kmDevolucao\":500}", VENDEDOR));
        assertEquals(422, status(HttpMethod.POST, "/api/pagamentos", pagamento(locacao, "99999"), VENDEDOR));
        MvcResult parcial = executar(HttpMethod.POST, "/api/pagamentos", pagamento(locacao, "100"), VENDEDOR);
        assertEquals(201, parcial.getResponse().getStatus());
        assertEquals("PARCIAL", ler(parcial).get("status").asText());
        assertEquals(409, status(HttpMethod.POST, "/api/pagamentos", pagamento(locacao, "100"), VENDEDOR));
        MvcResult saldo = executar(HttpMethod.GET, "/api/pagamentos/locacao/" + locacao + "/saldo", null, VENDEDOR);
        assertEquals(290.0, ler(saldo).get("saldo").asDouble());
        MvcResult quitado = executar(HttpMethod.POST, "/api/pagamentos", pagamento(locacao, "290"), VENDEDOR);
        assertEquals("PAGO", ler(quitado).get("status").asText());
        assertEquals(422, status(HttpMethod.POST, "/api/pagamentos", pagamento(locacao, "1"), VENDEDOR));

        MvcResult devolucao = executar(HttpMethod.POST, "/api/locacoes/" + locacao + "/devolucao", "{\"kmDevolucao\":1450}", VENDEDOR);
        assertEquals(200, devolucao.getResponse().getStatus());
        assertEquals("FINALIZADA", ler(devolucao).get("status").asText());
        assertEquals("DISPONIVEL", statusVeiculo(veiculo));
        assertEquals(409, status(HttpMethod.POST, "/api/locacoes/" + locacao + "/devolucao", "{\"kmDevolucao\":1500}", VENDEDOR));
        assertEquals(403, status(HttpMethod.PATCH, "/api/pagamentos/1/estorno", null, VENDEDOR));
    }

    @Test
    void clienteVeSomenteOsPropriosRegistros() throws Exception {
        long veiculo = criar("/api/veiculos", veiculo("CLI1A11", "Onix"), ADMIN);
        LocalDateTime ini = LocalDateTime.now().plusDays(20).withNano(0);
        String corpo = "{\"idVeiculo\":" + veiculo + ",\"dataInicio\":\"" + ini + "\",\"dataFim\":\"" + ini.plusDays(2) + "\"}";
        long reserva = criar("/api/reservas", corpo, CLIENTE);
        assertEquals(1, ler(executar(HttpMethod.GET, "/api/reservas", null, CLIENTE)).size());

        long outro = criar("/api/clientes", cliente("29537995593", "outro@teste.com", "04100000006"), ADMIN);
        long reservaOutro = criar("/api/reservas", reserva(outro, veiculo, ini.plusDays(10), ini.plusDays(12)), ADMIN);
        JsonNode minhas = ler(executar(HttpMethod.GET, "/api/reservas", null, CLIENTE));
        for (JsonNode r : minhas) {
            assertNotEquals(reservaOutro, r.get("id").asLong());
        }
        assertEquals(403, status(HttpMethod.PATCH, "/api/reservas/" + reservaOutro + "/cancelar", null, CLIENTE));
        assertEquals(403, status(HttpMethod.PATCH, "/api/reservas/" + reserva + "/confirmar", null, CLIENTE));
        assertEquals(200, status(HttpMethod.PATCH, "/api/reservas/" + reserva + "/cancelar", null, CLIENTE));
    }

    private double valorDaReserva(long id) throws Exception {
        for (JsonNode r : ler(executar(HttpMethod.GET, "/api/reservas", null, VENDEDOR))) {
            if (r.get("id").asLong() == id) {
                return r.get("valorTotalPrevisto").asDouble();
            }
        }
        return -1;
    }

    private String statusVeiculo(long id) throws Exception {
        for (JsonNode v : ler(executar(HttpMethod.GET, "/api/veiculos", null, ADMIN))) {
            if (v.get("id").asLong() == id) {
                return v.get("status").asText();
            }
        }
        return null;
    }

    private MvcResult executar(HttpMethod metodo, String url, String corpo, String[] cred) throws Exception {
        MockHttpServletRequestBuilder b = MockMvcRequestBuilders.request(metodo, url);
        if (corpo != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        if (cred != null) {
            b.with(httpBasic(cred[0], cred[1]));
        }
        return mvc.perform(b).andReturn();
    }

    private int status(HttpMethod metodo, String url, String corpo, String[] cred) throws Exception {
        return executar(metodo, url, corpo, cred).getResponse().getStatus();
    }

    private JsonNode ler(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private long criar(String url, String corpo, String[] cred) throws Exception {
        MvcResult r = executar(HttpMethod.POST, url, corpo, cred);
        assertEquals(201, r.getResponse().getStatus(), url + ": " + r.getResponse().getContentAsString());
        return ler(r).get("id").asLong();
    }

    private String veiculo(String placa, String modelo) {
        return "{\"placa\":\"" + placa + "\",\"marca\":\"Marca\",\"modelo\":\"" + modelo + "\",\"ano\":2024,\"cor\":\"Branco\","
                + "\"categoria\":\"SEDAN\",\"quilometragem\":1000,\"valorDiaria\":130.00}";
    }

    private String cliente(String cpf, String email, String cnh) {
        return "{\"nomeCompleto\":\"Cliente Teste\",\"cpf\":\"" + cpf + "\",\"email\":\"" + email + "\",\"telefone\":\"11988887777\","
                + "\"cnhNumero\":\"" + cnh + "\",\"cnhValidade\":\"" + LocalDate.now().plusYears(3) + "\"}";
    }

    private String reserva(long cliente, long veiculo, LocalDateTime ini, LocalDateTime fim) {
        return "{\"idCliente\":" + cliente + ",\"idVeiculo\":" + veiculo + ",\"dataInicio\":\"" + ini + "\",\"dataFim\":\"" + fim + "\"}";
    }

    private String pagamento(long locacao, String valor) {
        return "{\"idLocacao\":" + locacao + ",\"valor\":" + valor + ",\"formaPagamento\":\"PIX\"}";
    }
}
