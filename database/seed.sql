USE autorent;
SET NAMES utf8mb4;

INSERT INTO cliente (id_cliente, nome_completo, cpf, email, telefone, cnh_numero, cnh_validade, cnh_situacao, cnh_verificada_em, data_cadastro) VALUES
(1,  'Ana Beatriz Souza',      '39053344705', 'ana.souza@exemplo.com',      '(11) 91111-0001', '04100000001', '2030-05-20', 'VALIDA',         '2026-01-10 09:00:00', '2026-01-10 09:00:00'),
(2,  'Bruno Henrique Lima',    '11144477735', 'bruno.lima@exemplo.com',     '(11) 91111-0002', '04100000002', '2029-11-02', 'VALIDA',         '2026-01-15 10:30:00', '2026-01-15 10:30:00'),
(3,  'Carla Mendes Ferreira',  '52998224725', 'carla.ferreira@exemplo.com', '(11) 91111-0003', '04100000003', '2031-03-14', 'VALIDA',         '2026-02-03 14:00:00', '2026-02-03 14:00:00'),
(4,  'Diego Alves Barbosa',    '12345678909', 'diego.barbosa@exemplo.com',  '(11) 91111-0004', '04100000004', '2028-08-30', 'VALIDA',         '2026-02-20 11:15:00', '2026-02-20 11:15:00'),
(5,  'Eduarda Nogueira Costa', '16899535009', 'eduarda.costa@exemplo.com',  '(11) 91111-0005', '04100000005', '2029-01-25', 'VALIDA',         '2026-03-05 16:45:00', '2026-03-05 16:45:00'),
(6,  'Felipe Rocha Martins',   '93541134780', 'felipe.martins@exemplo.com', '(11) 91111-0006', '04100000006', '2030-09-09', 'VALIDA',         '2026-03-18 08:20:00', '2026-03-18 08:20:00'),
(7,  'Gabriela Pires Duarte',  '74697131401', 'gabriela.duarte@exemplo.com','(11) 91111-0007', '04100000007', '2032-02-11', 'NAO_VERIFICADA', NULL,                  '2026-10-25 13:00:00'),
(8,  'Henrique Tavares Neto',  '29537995593', 'henrique.neto@exemplo.com',  '(11) 91111-0008', '04100000008', '2025-03-10', 'VENCIDA',        '2026-08-20 09:10:00', '2026-08-20 09:10:00'),
(9,  'Isabela Cardoso Ramos',  '86398734530', 'isabela.ramos@exemplo.com',  '(11) 91111-0009', '04100000009', '2028-12-01', 'IRREGULAR',      '2026-09-15 15:30:00', '2026-09-15 15:30:00'),
(10, 'João Pedro Azevedo',     '45738291050', 'joao.azevedo@exemplo.com',   '(11) 91111-0010', '99999999999', '2029-06-18', 'INEXISTENTE',    '2026-10-01 10:00:00', '2026-10-01 10:00:00');

INSERT INTO veiculo (id_veiculo, placa, marca, modelo, ano, cor, categoria, valor_diaria, quilometragem, status, data_cadastro) VALUES
(1,  'ABC1D23', 'Fiat',       'Mobi',          2023, 'Branco', 'ECONOMICO',  99.00, 18500, 'DISPONIVEL', '2026-01-05 08:00:00'),
(2,  'BCD2E34', 'Renault',    'Kwid',          2024, 'Prata',  'ECONOMICO',  95.00,  8000, 'ALUGADO',    '2026-01-05 08:10:00'),
(3,  'CDE3F45', 'Volkswagen', 'Polo',          2023, 'Cinza',  'COMPACTO',  130.00, 21000, 'ALUGADO',    '2026-01-05 08:20:00'),
(4,  'DEF4G56', 'Hyundai',    'HB20',          2024, 'Preto',  'COMPACTO',  135.00,  9500, 'RESERVADO',  '2026-01-06 09:00:00'),
(5,  'EFG5H67', 'Chevrolet',  'Onix Plus',     2023, 'Branco', 'SEDAN',     150.00, 33000, 'DISPONIVEL', '2026-01-06 09:10:00'),
(6,  'FGH6I78', 'Toyota',     'Corolla',       2024, 'Preto',  'SEDAN',     220.00, 15980, 'DISPONIVEL', '2026-01-06 09:20:00'),
(7,  'GHI7J89', 'Jeep',       'Renegade',      2023, 'Verde',  'SUV',       250.00,  5400, 'ALUGADO',    '2026-01-07 10:00:00'),
(8,  'HIJ8K90', 'Toyota',     'Corolla Cross', 2024, 'Prata',  'SUV',       280.00, 12610, 'DISPONIVEL', '2026-01-07 10:10:00'),
(9,  'IJK9L01', 'Fiat',       'Strada',        2024, 'Vermelho','PICAPE',   200.00,  7200, 'RESERVADO',  '2026-01-08 11:00:00'),
(10, 'JKL0M12', 'Toyota',     'Hilux',         2023, 'Branco', 'PICAPE',    380.00, 28000, 'DISPONIVEL', '2026-01-08 11:10:00'),
(11, 'KLM1N23', 'BMW',        '320i',          2023, 'Azul',   'LUXO',      520.00, 14000, 'DISPONIVEL', '2026-01-09 14:00:00'),
(12, 'LMN2O34', 'Volvo',      'XC40',          2024, 'Cinza',  'LUXO',      480.00, 11000, 'MANUTENCAO', '2026-01-09 14:10:00');

INSERT INTO reserva (id_reserva, id_cliente, id_veiculo, data_inicio, data_fim, valor_total_previsto, status, data_criacao) VALUES
(1,  1, 6, '2026-08-03 09:00:00', '2026-08-07 09:00:00',  880.00, 'CONCLUIDA',  '2026-07-28 15:00:00'),
(2,  2, 5, '2026-09-10 10:00:00', '2026-09-15 10:00:00',  750.00, 'CONCLUIDA',  '2026-09-05 11:00:00'),
(3,  3, 2, CURDATE() - INTERVAL 2 DAY + INTERVAL 10 HOUR, CURDATE() + INTERVAL 3 DAY + INTERVAL 10 HOUR,  475.00, 'CONCLUIDA', CURDATE() - INTERVAL 5 DAY),
(4,  4, 3, CURDATE() - INTERVAL 6 DAY + INTERVAL 9 HOUR,  CURDATE() - INTERVAL 1 DAY + INTERVAL 10 HOUR,  650.00, 'CONCLUIDA', CURDATE() - INTERVAL 9 DAY),
(5,  5, 7, CURDATE() - INTERVAL 3 DAY + INTERVAL 10 HOUR, CURDATE() + INTERVAL 18 HOUR,                 1000.00, 'CONCLUIDA', CURDATE() - INTERVAL 7 DAY),
(6,  6, 4, CURDATE() + INTERVAL 5 DAY + INTERVAL 9 HOUR,  CURDATE() + INTERVAL 9 DAY + INTERVAL 9 HOUR,   540.00, 'CONFIRMADA', CURDATE() - INTERVAL 1 DAY),
(7,  1, 9, CURDATE() + INTERVAL 2 DAY + INTERVAL 8 HOUR,  CURDATE() + INTERVAL 4 DAY + INTERVAL 8 HOUR,   400.00, 'CONFIRMADA', CURDATE()),
(8,  7, 8, CURDATE() + INTERVAL 10 DAY + INTERVAL 9 HOUR, CURDATE() + INTERVAL 14 DAY + INTERVAL 9 HOUR, 1120.00, 'PENDENTE',   CURDATE()),
(9,  2, 1, CURDATE() + INTERVAL 1 DAY + INTERVAL 9 HOUR,  CURDATE() + INTERVAL 3 DAY + INTERVAL 9 HOUR,   198.00, 'CANCELADA',  CURDATE() - INTERVAL 2 DAY),
(10, 8, 10,'2026-09-01 09:00:00', '2026-09-05 09:00:00', 1520.00, 'CANCELADA', '2026-08-25 10:00:00'),
(11, 9, 11,'2026-09-20 09:00:00', '2026-09-23 09:00:00', 1560.00, 'CANCELADA', '2026-09-16 10:00:00');

INSERT INTO locacao (id_locacao, id_reserva, id_cliente, id_veiculo, data_retirada, data_prevista_devolucao, data_devolucao_efetiva, km_retirada, km_devolucao, valor_diaria, valor_total, valor_multa, status) VALUES
(1, 1, 1, 6, '2026-08-03 09:15:00', '2026-08-07 09:00:00', '2026-08-07 08:50:00', 15200, 15980, 220.00,  880.00,   0.00, 'FINALIZADA'),
(2, 2, 2, 5, '2026-09-10 10:00:00', '2026-09-15 10:00:00', '2026-09-16 14:00:00', 32000, 32940, 150.00,  750.00, 300.00, 'FINALIZADA'),
(3, 3, 3, 2, CURDATE() - INTERVAL 2 DAY + INTERVAL 10 HOUR + INTERVAL 15 MINUTE, CURDATE() + INTERVAL 3 DAY + INTERVAL 10 HOUR, NULL, 8000, NULL, 95.00,  475.00, 0.00, 'ATIVA'),
(4, 4, 4, 3, CURDATE() - INTERVAL 6 DAY + INTERVAL 9 HOUR, CURDATE() - INTERVAL 1 DAY + INTERVAL 10 HOUR, NULL, 21000, NULL, 130.00, 650.00, 0.00, 'ATRASADA'),
(5, 5, 5, 7, CURDATE() - INTERVAL 3 DAY + INTERVAL 10 HOUR, CURDATE() + INTERVAL 18 HOUR, NULL, 5400, NULL, 250.00, 1000.00, 0.00, 'ATIVA'),
(6, NULL, 6, 8, '2026-07-10 11:00:00', '2026-07-13 11:00:00', '2026-07-13 10:30:00', 12000, 12610, 280.00, 840.00, 0.00, 'FINALIZADA');

INSERT INTO pagamento (id_pagamento, id_locacao, data_pagamento, valor, forma_pagamento, status, observacao) VALUES
(1, 1, '2026-08-03 09:20:00',  880.00, 'PIX',            'PAGO',      'Pagamento integral na retirada'),
(2, 2, '2026-09-10 10:05:00',  750.00, 'CARTAO_CREDITO', 'PAGO',      'Diárias'),
(3, 2, '2026-09-16 14:10:00',  300.00, 'DINHEIRO',       'PAGO',      'Multa por atraso na devolução'),
(4, 3, NOW() - INTERVAL 2 DAY, 200.00, 'PIX',            'PARCIAL',   'Entrada; saldo a pagar na devolução'),
(5, 3, NOW(),                  275.00, 'CARTAO_DEBITO',  'PENDENTE',  'Saldo restante aguardando confirmação'),
(6, 4, NOW() - INTERVAL 6 DAY, 650.00, 'CARTAO_DEBITO',  'PENDENTE',  'Aguardando confirmação'),
(7, 5, NOW() - INTERVAL 3 DAY, 1000.00,'CARTAO_CREDITO', 'PAGO',      'Pagamento integral na retirada'),
(8, 6, '2026-07-10 11:05:00',  100.00, 'PIX',            'ESTORNADO', 'Cobrança em duplicidade'),
(9, 6, '2026-07-10 11:10:00',  840.00, 'DINHEIRO',       'PAGO',      'Pagamento integral');
