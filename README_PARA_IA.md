# AutoRent – documento de passagem de contexto (para outra IA)

Use este arquivo como ponto de partida. Ele descreve o projeto, o que já existe, como rodar, as decisões tomadas e o que ainda falta. Nada aqui foi inventado: o que não foi verificado está marcado como tal.

## 1. Contexto

- **Trabalho:** A3 da unidade curricular Modelagem de Software (USJT, 2026/2), 40 pontos. Tema escolhido: **sistema de locação de veículos (AutoRent)**.
- **O que o edital exige:**
  - ≥ 6 requisitos funcionais (RF), ≥ 4 não funcionais (RNF) e ≥ 4 regras de negócio (RN).
  - Diagramas UML: casos de uso, classes, ≥ 2 de sequência, ≥ 1 de atividades e 1 complementar (componentes, implantação, estados ou comunicação).
  - Banco relacional com DER, modelo lógico, DDL, ≥ 4 entidades e normalização até a 3FN.
  - Software executável com CRUD completo de ≥ 2 entidades ligado a um SGBD.
  - Script `.sql` com DDL e DML (≥ 3 registros por entidade).
  - Documento técnico em PDF (Arial/Times 12, espaçamento 1,5, margens 2,5 cm) e apresentação oral (10–15 min).
- **Peso na nota:** a implementação vale só 6 dos 40 pontos; o grosso é modelagem e documento.
- **Prazos:** entrega final em **28/11/2026, 23h59** (−2 pontos por dia de atraso); apresentações na semana de 30/11.
- **IA generativa é permitida**, mas deve ser **declarada** na seção "Ferramentas e Tecnologias Empregadas" do documento, e a equipe precisa dominar o conteúdo na arguição oral. Plágio vale nota zero.
- **Divisão do trabalho:** o código foi gerado com apoio de IA. Os **diagramas UML, o documento técnico e o manual de uso serão produzidos pelo(a) aluno(a)**, não pela IA, salvo pedido explícito.

## 2. Estado atual

A pasta `autorent-simples/` é a **versão de entrega** (simples e funcional). Há também, na pasta-mãe `A3_Modelagem/`, o projeto anterior (`backend/`, `frontend/`, `database/`) e `_backup_com_comentarios/`; eles **não fazem parte da entrega**.

**Funciona e foi verificado:**
- Compila e **15 testes passam** (H2 em memória): permissões dos 3 perfis, CRUD de veículos e clientes, cadastro público, dashboard, fluxo reserva → retirada → pagamento parcial/integral → devolução (com conflito de período e pagamento duplicado), isolamento entre clientes e bloqueio de login (inclui desbloqueio por tempo).
- Interface exercitada em navegador contra o backend em H2: login dos 3 perfis, máscaras, reserva, retirada, pagamentos, devolução, bloqueio de login, portal do cliente e layout de celular.
- Os scripts SQL (`schema.sql`, `seed.sql`) foram executados com sucesso pelo usuário no MySQL Workbench (6 tabelas, 2 procedures, 4 triggers; seed com 10 clientes, 12 veículos, 11 reservas, 6 locações, 9 pagamentos).

**NÃO verificado:**
- O backend **nunca foi executado contra o MySQL real do usuário** (só H2). Riscos prováveis: nomes de colunas, triggers e restrições CHECK.
- O `usuarios.sql` não foi executado no MySQL.

## 3. Stack e estrutura

- Java 17, Spring Boot 3.3.5 (Web, Data JPA, Security, Validation), Lombok, Maven, MySQL 8.0.16+.
- Interface em **HTML, CSS e JavaScript puro** (ES modules, sem Node e sem build), servida pelo Spring em `src/main/resources/static/`.
- Idioma da interface e das mensagens: **português do Brasil**.

```
src/main/java/com/autorent/
  AutorentApplication.java
  entity/        Cliente, Veiculo, Reserva, Locacao, Pagamento, Usuario (enums aninhados em cada entidade)
  repository/    um por entidade; consultas de conflito de período e de disponibilidade
  service/       ClienteService, VeiculoService, ReservaService, LocacaoService, PagamentoService, Regras (cálculos e validações)
  controller/    Auth, Cliente, Veiculo, Reserva, Locacao, Pagamento, Dashboard, Usuario (/api/...)
  config/        SecurityConfig, BloqueioLogin, ApiException, TratadorErros
src/main/resources/static/
  index.html, css/estilo.css
  js/api.js (fetch, formatadores), js/ui.js (modais, tabelas, máscaras, validação), js/app.js (login, menu, rotas por hash)
  js/telas/ dashboard, veiculos, clientes, reservas, locacoes, pagamentos, usuarios, portal
src/test/java/com/autorent/   AutorentTest, BloqueioLoginTest
database/                     schema.sql, seed.sql, usuarios.sql
```

## 4. Como rodar

Pré-requisitos: JDK 17+, Maven 3.9+, MySQL 8.0.16+ com o schema `autorent` já criado e populado.

```powershell
$env:DB_USER = "root"
$env:DB_PASSWORD = "sua_senha"
mvn spring-boot:run
```

Abrir `http://localhost:8080`. Testes: `mvn test`. Variáveis opcionais: `DB_URL`, `SERVER_PORT`, `LOGIN_MAX_TENTATIVAS` (3), `LOGIN_BLOQUEIO_MINUTOS` (10), `MULTA_FATOR_DIARIA` (1.0), `MULTA_TOLERANCIA_MINUTOS` (60).

Credenciais de demonstração (somente local): `admin@autorent.local` / `Admin@123`, `vendedor@autorent.local` / `Vendedor@123`, `ana.souza@exemplo.com` / `Cliente@123`.

## 5. Banco de dados (MySQL, schema `autorent`)

- **Tabelas:** `cliente`, `veiculo`, `reserva`, `locacao`, `pagamento` e `usuario` (esta última foi acrescentada para autenticação; `senha_hash` guarda BCrypt; perfil `ADMIN`/`VENDEDOR`/`CLIENTE`; usuário CLIENTE tem `id_cliente` obrigatório e único).
- **Integridade:** PK/FK com `ON DELETE RESTRICT`, UNIQUE (CPF, e-mail e CNH do cliente; placa do veículo; e-mail do usuário; uma locação por reserva), CHECKs (datas, valores, formatos, domínios de status) e FK composta `(id_reserva, id_cliente, id_veiculo)` na locação.
- **Procedures e triggers** (segunda camada de proteção): `sp_validar_disponibilidade_veiculo` (conflito de período, veículo em manutenção/inativo) e `sp_validar_cnh_cliente`, chamadas por triggers antes de inserir/atualizar reservas e locações. Erros chegam como SQLSTATE `45000`.
- **Não existem no schema** (por decisão): combustível inicial/final, data de nascimento e endereço do cliente.
- **Restrições de trabalho com o banco:** **nunca** recriar o schema, executar `DROP`, reexecutar `seed.sql` (duplicaria dados) nem apagar registros. Alterações de estrutura devem vir como script separado para o usuário rodar.

## 6. API REST (`/api`)

| Recurso | Endpoints | Perfis |
|---|---|---|
| Sessão | `GET /eu`; `POST /cadastro` (público, cria sempre CLIENTE) | autenticado / público |
| Veículos | `GET` (lista), `GET /disponiveis?inicio&fim&categoria&busca&precoMin&precoMax` | lista: ADMIN, VENDEDOR; disponíveis: todos |
| | `POST`, `PUT /{id}`, `DELETE /{id}` | ADMIN |
| Clientes | `GET`, `POST`, `PUT /{id}` | ADMIN, VENDEDOR |
| | `DELETE /{id}` | ADMIN |
| | `GET/PUT /clientes/me` | CLIENTE |
| Reservas | `GET`, `POST`, `PATCH /{id}/cancelar` | todos (CLIENTE só as próprias) |
| | `PATCH /{id}/confirmar` | ADMIN, VENDEDOR |
| Locações | `GET` | todos (CLIENTE só as próprias) |
| | `POST /retirada`, `POST /{id}/devolucao` | ADMIN, VENDEDOR |
| Pagamentos | `GET`, `GET /locacao/{id}/saldo` | todos (CLIENTE só os próprios) |
| | `POST` | ADMIN, VENDEDOR |
| | `PATCH /{id}/estorno` | ADMIN |
| Dashboard | `GET /dashboard` (receita só para ADMIN) | ADMIN, VENDEDOR |
| Usuários | `GET`, `POST`, `PATCH /{id}/ativo?valor=` | ADMIN |

Erros: JSON `{status, mensagem, campos?}`. 400 validação, 401 credenciais, 403 perfil, 404, 409 conflito (duplicidade, período ocupado, histórico), 422 regra de negócio, 429 bloqueio de login.

## 7. Regras de negócio implementadas

- **RN01** Sem conflito de período no mesmo veículo (reservas confirmadas e locações ativas/atrasadas) — `VeiculoService.exigirDisponivel`.
- **RN02** Só veículo apto (fora de manutenção e inativo).
- **RN03** Locação sempre com cliente e veículo, a partir de reserva confirmada ou de balcão — `LocacaoService.retirar`.
- **RN04** Devolução só após a retirada, uma única vez, quilometragem final ≥ inicial — `LocacaoService.devolver`.
- **RN05** Valor estimado = diárias (período arredondado para cima, mínimo 1) × diária — `Regras.valorEstimado`.
- **RN06** Cliente só vê e altera os próprios registros (`ReservaService`, `LocacaoService`, `PagamentoService`).
- **RN07** Permissões por perfil (`@PreAuthorize` nos controllers; sem token = 401).
- **Outras:** CNH válida e com validade até o fim do período (consulta **simulada** em `ClienteService.BASE_CNH`); reserva com devolução posterior à retirada e retirada não no passado; confirmação revalida disponibilidade e CNH; retirada permitida de 2 h antes do início até o fim da reserva, mantendo o valor da reserva; status do veículo (`RESERVADO`/`ALUGADO`/`DISPONIVEL`) automático e só `DISPONIVEL`/`MANUTENCAO`/`INATIVO` manuais; veículo com histórico é desativado em vez de excluído; cliente com histórico ou usuário vinculado não é excluído; CPF com dígitos verificadores, CPF/e-mail/CNH/placa únicos.
- **Multa por atraso:** dias de atraso (após tolerância) × diária × fator; o valor acumulado aparece nas locações atrasadas antes da devolução (`Regras.multaAtraso`, `LocacaoService.comMultaAtual`).
- **Pagamentos:** o status (`PAGO`/`PARCIAL`) é definido pelo saldo; valor não pode exceder o saldo; pagamento idêntico (locação, valor, forma) em menos de 15 s é recusado; a locação é bloqueada (`PESSIMISTIC_WRITE`) durante o registro; estorno só de pago/parcial, por ADMIN.

## 8. Segurança

- **Login HTTP Basic**, sem sessão no servidor: a interface guarda `e-mail:senha` (Base64) em `sessionStorage` e envia em cada requisição; o servidor valida com BCrypt e **recarrega o usuário a cada chamada** (desativar um usuário bloqueia na hora). Não há JWT (possível de adicionar; a interface teria que guardar um token em vez da senha).
- **Bloqueio de login:** 3 senhas erradas (mesmo e-mail + mesmo IP) bloqueiam por 10 minutos, mesmo com a senha correta (HTTP 429 + `Retry-After`); acerto antes do limite zera; em memória (`config/BloqueioLogin`).
- CSRF desativado (não há cookies de sessão); CORS não configurado porque front e back têm a mesma origem.
- Saída HTML da interface escapada (`esc()` em `js/api.js`).

## 9. Decisões e suposições (não aprovadas formalmente pelo usuário)

- **Multa:** 1 diária por dia de atraso e tolerância de 60 minutos são **suposições**; precisam de confirmação.
- Bloqueio de login de **10 minutos** e janela de contagem igual ao bloqueio: escolha minha.
- Consulta de CNH é uma tabela fixa no código (números `04100000001`–`07` e `11`–`20` válidos; `…08` vencida; `…09` irregular; `99999999999` inexistente; qualquer outro = inexistente). Uma API fake em Python/FastAPI foi cogitada para o futuro e **não foi iniciada**.
- Pagamentos são simulados (sem dados de cartão e sem gateway).

## 10. Limitações conhecidas

- Sem paginação no servidor (as listas são filtradas na tela) e sem relatórios separados do dashboard.
- Reservas confirmadas e não retiradas **não expiram** sozinhas.
- Bloqueio de login é por instância e não sobrevive a reinício.
- Sem registro de combustível, data de nascimento e endereço.
- Sem testes de interface automatizados; sem teste automatizado contra MySQL.

## 11. O que falta para a entrega acadêmica (responsabilidade do aluno)

Documento técnico em PDF com: introdução e justificativa, catálogo RF/RNF/RN, diagrama de casos de uso, de classes, ≥ 2 de sequência (sugestão: reserva e retirada/devolução), ≥ 1 de atividades, 1 complementar (sugestão: estados de Reserva e Locação), DER conceitual, modelo lógico, comprovação da 3FN (atenção a campos derivados como `valor_total` e `valor_total_previsto` e ao snapshot `locacao.valor_diaria`), justificativa do SGBD, seção "Ferramentas e Tecnologias Empregadas" declarando o uso de IA, folha de rosto padronizada e nome de arquivo `A3_ModelagemSoftware_Turma_NomeDoSistema.pdf`. Também: apresentação oral e, opcionalmente, vídeo de até 8 min (bônus de até 2 pontos).

## 12. Regras para quem continuar

- Responder e documentar em **português do Brasil**; manter o visual atual (barra lateral roxo-escura, gradientes violeta/azul, cantos arredondados).
- O código-fonte está **sem comentários** por pedido do usuário; mantenha assim, a menos que ele peça o contrário.
- Não alterar a stack (Java/Spring Boot, MySQL, HTML/CSS/JS) sem justificativa e aprovação. Não alterar o banco existente nem executar seed.
- Não afirmar que algo funciona sem executar. Quando houver decisão de negócio indefinida, **perguntar** em vez de inventar.
- Não commitar nem publicar nada sem o usuário pedir. Não incluir segredos ou dados pessoais reais (os CPFs do seed são fictícios).
- Dicas técnicas: no Windows, caminhos para `java -cp` precisam estar no formato Windows; respostas JSON sem charset exigem `getContentAsString(UTF_8)` nos testes; o conteúdo de `static/` é servido a partir de `target/classes` quando se usa `mvn spring-boot:run`.
