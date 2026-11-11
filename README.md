# AutoRent (versão simples)

Sistema de locação de veículos do trabalho A3 de Modelagem de Software (USJT). Esta versão reúne tudo em um único projeto:

- **Backend:** Java 17, Spring Boot 3 (Web, Data JPA, Security, Validation), Maven.
- **Banco:** MySQL 8.
- **Interface:** HTML, CSS e JavaScript puro, servida pelo próprio Spring Boot (sem Node e sem etapa de build).

## Pré-requisitos

JDK 17+, Maven 3.9+ e MySQL 8.0.16+ (com o MySQL Workbench).

## Banco de dados

Usa o mesmo schema `autorent` do projeto anterior, sem nenhuma mudança de estrutura.

1. No Workbench, com o schema `autorent` criado, execute `database/schema.sql` e depois `database/seed.sql` (uma única vez, com as tabelas vazias).
2. Execute `database/usuarios.sql`: cria os usuários de demonstração (senhas em BCrypt). É seguro repetir, pois só insere quem ainda não existe.

Se o seu banco já tem esses dados, não precisa executar nada de novo.

## Executar

```bash
cd autorent-simples
mvn spring-boot:run
```

Antes, defina o usuário e a senha do MySQL (PowerShell):

```powershell
$env:DB_USER = "root"
$env:DB_PASSWORD = "sua_senha"
```

Abra `http://localhost:8080`. Outras variáveis opcionais: `DB_URL`, `SERVER_PORT`, `MULTA_FATOR_DIARIA` e `MULTA_TOLERANCIA_MINUTOS`.

## Credenciais de demonstração (somente ambiente local)

| Perfil | E-mail | Senha |
|---|---|---|
| Administrador | `admin@autorent.local` | `Admin@123` |
| Vendedor | `vendedor@autorent.local` | `Vendedor@123` |
| Cliente | `ana.souza@exemplo.com` | `Cliente@123` |

Novos clientes também podem se cadastrar na tela de login (CNHs aceitas para teste: `04100000011` a `04100000020`).

## Perfis e permissões (verificados no backend)

| Recurso | ADMIN | VENDEDOR | CLIENTE |
|---|---|---|---|
| Dashboard | completo (com receita) | sem receita | não |
| Veículos | CRUD | consulta | busca de disponíveis |
| Clientes | CRUD | cadastro e edição | só os próprios dados |
| Reservas | todas | todas | só as próprias (criar e cancelar) |
| Locações (retirada e devolução) | sim | sim | só consulta das próprias |
| Pagamentos | registrar e estornar | registrar | só consulta dos próprios |
| Usuários | sim | não | não |

O login usa HTTP Basic: a tela guarda as credenciais na sessão do navegador (`sessionStorage`) e as envia em cada requisição; o servidor valida a senha com BCrypt e recarrega o usuário a cada chamada, então desativar um usuário bloqueia o acesso na hora.

## Bloqueio por tentativas de login

Após **3 senhas erradas** para o mesmo e-mail a partir do mesmo endereço IP, o login fica bloqueado por **10 minutos** (mesmo com a senha correta), com a mensagem "Muitas tentativas de login. Tente novamente em N minutos." (HTTP 429 com `Retry-After`). Um acerto antes do limite zera a contagem, e o bloqueio não afeta outros e-mails nem outros IPs. Configurável por `LOGIN_MAX_TENTATIVAS` e `LOGIN_BLOQUEIO_MINUTOS`. O controle é em memória: reiniciar o sistema limpa os bloqueios.

## Regras de negócio

- **RN01** Sem conflito de período: um veículo não tem reservas confirmadas ou locações ativas em períodos sobrepostos (`VeiculoService.exigirDisponivel`, mais triggers do MySQL).
- **RN02** Só veículos aptos (não em manutenção nem inativos) podem ser reservados ou alugados.
- **RN03** A locação sempre tem cliente e veículo, vinda de uma reserva confirmada ou do balcão (`LocacaoService.retirar`).
- **RN04** A devolução só ocorre após a retirada, uma única vez, e a quilometragem final não pode ser menor que a inicial.
- **RN05** Valor estimado = diárias (período arredondado para cima, mínimo de uma) × valor da diária (`Regras.valorEstimado`).
- **RN06** O cliente só consulta e altera os próprios registros.
- **RN07** As operações administrativas respeitam o perfil (`@PreAuthorize` nos controllers).
- CNH precisa estar válida e com validade até o fim do período (consulta simulada em `ClienteService`).
- Multa por atraso: dias de atraso (após a tolerância) × diária × fator, **parâmetros ainda não aprovados** (padrão: 1,0 e 60 minutos). A multa acumulada aparece nas locações atrasadas antes da devolução.
- Pagamento: o status (pago ou parcial) é definido pelo saldo, o valor não pode exceder o saldo e um pagamento idêntico em menos de 15 segundos é recusado. A locação é bloqueada durante o registro para evitar pagamentos simultâneos.
- Exclusão: cliente com histórico não é excluído; veículo com histórico é apenas desativado.

## Estrutura

```
src/main/java/com/autorent/
  entity/       Cliente, Veiculo, Reserva, Locacao, Pagamento, Usuario (enums dentro de cada entidade)
  repository/   consultas JPA (inclui conflito de período e disponibilidade)
  service/      regras de negócio (Cliente, Veiculo, Reserva, Locacao, Pagamento) e Regras (cálculos)
  controller/   API REST (/api/...)
  config/       segurança, tratamento de erros
src/main/resources/static/   index.html, css/estilo.css, js/ (api, ui, app) e js/telas/ (uma por tela)
database/       schema.sql, seed.sql, usuarios.sql
```

## Testes

```bash
mvn test
```

15 testes (H2 em memória): bloqueio de login (inclui desbloqueio após o intervalo), autenticação e permissões dos três perfis, CRUD de veículos e clientes, cadastro público, dashboard, fluxo reserva → retirada → pagamento parcial e integral → devolução (com conflito de período e pagamento duplicado) e isolamento entre clientes.

## O que não está nesta versão

Em relação ao projeto maior: sem JWT (o login é HTTP Basic), sem paginação no servidor (as listas são filtradas na tela), sem relatórios separados, sem expiração automática de reservas e sem registro de combustível (o schema não possui essas colunas).

## Verificação

Compila e os 15 testes passam em H2. A interface foi exercitada no navegador contra o backend em H2 (login dos três perfis, máscaras, reserva, retirada, pagamentos e devolução). Ainda **não foi executado contra o seu MySQL real**.
