USE autorent;

CREATE TABLE IF NOT EXISTS cliente (
    id_cliente        BIGINT        NOT NULL AUTO_INCREMENT,
    nome_completo     VARCHAR(120)  NOT NULL,
    cpf               CHAR(11)      NOT NULL COMMENT 'Somente dígitos',
    email             VARCHAR(120)  NOT NULL,
    telefone          VARCHAR(20)   NOT NULL,
    cnh_numero        CHAR(11)      NOT NULL COMMENT 'Somente dígitos',
    cnh_validade      DATE          NOT NULL,
    cnh_situacao      VARCHAR(20)   NOT NULL DEFAULT 'NAO_VERIFICADA',
    cnh_verificada_em DATETIME      NULL,
    data_cadastro     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_cliente PRIMARY KEY (id_cliente),
    CONSTRAINT uk_cliente_cpf   UNIQUE (cpf),
    CONSTRAINT uk_cliente_email UNIQUE (email),
    CONSTRAINT uk_cliente_cnh   UNIQUE (cnh_numero),
    CONSTRAINT ck_cliente_cpf   CHECK (REGEXP_LIKE(cpf, '^[0-9]{11}$')),
    CONSTRAINT ck_cliente_cnh   CHECK (REGEXP_LIKE(cnh_numero, '^[0-9]{11}$')),
    CONSTRAINT ck_cliente_cnh_situacao CHECK (cnh_situacao IN
        ('NAO_VERIFICADA', 'VALIDA', 'VENCIDA', 'INEXISTENTE', 'IRREGULAR'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_cliente_nome ON cliente (nome_completo);

CREATE TABLE IF NOT EXISTS veiculo (
    id_veiculo    BIGINT         NOT NULL AUTO_INCREMENT,
    placa         CHAR(7)        NOT NULL COMMENT 'Padrão antigo (ABC1234) ou Mercosul (ABC1D23)',
    marca         VARCHAR(50)    NOT NULL,
    modelo        VARCHAR(60)    NOT NULL,
    ano           SMALLINT       NOT NULL,
    cor           VARCHAR(30)    NOT NULL,
    categoria     VARCHAR(20)    NOT NULL,
    valor_diaria  DECIMAL(10,2)  NOT NULL,
    quilometragem INT            NOT NULL DEFAULT 0,
    status        VARCHAR(20)    NOT NULL DEFAULT 'DISPONIVEL',
    data_cadastro DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_veiculo PRIMARY KEY (id_veiculo),
    CONSTRAINT uk_veiculo_placa UNIQUE (placa),
    CONSTRAINT ck_veiculo_placa CHECK (REGEXP_LIKE(placa, '^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$')),
    CONSTRAINT ck_veiculo_ano   CHECK (ano BETWEEN 1990 AND 2100),
    CONSTRAINT ck_veiculo_diaria CHECK (valor_diaria > 0),
    CONSTRAINT ck_veiculo_km    CHECK (quilometragem >= 0),
    CONSTRAINT ck_veiculo_categoria CHECK (categoria IN
        ('ECONOMICO', 'COMPACTO', 'SEDAN', 'SUV', 'PICAPE', 'LUXO')),
    CONSTRAINT ck_veiculo_status CHECK (status IN
        ('DISPONIVEL', 'RESERVADO', 'ALUGADO', 'MANUTENCAO', 'INATIVO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_veiculo_status_categoria ON veiculo (status, categoria);
CREATE INDEX idx_veiculo_marca_modelo     ON veiculo (marca, modelo);

CREATE TABLE IF NOT EXISTS reserva (
    id_reserva           BIGINT         NOT NULL AUTO_INCREMENT,
    id_cliente           BIGINT         NOT NULL,
    id_veiculo           BIGINT         NOT NULL,
    data_inicio          DATETIME       NOT NULL,
    data_fim             DATETIME       NOT NULL,
    valor_total_previsto DECIMAL(10,2)  NOT NULL,
    status               VARCHAR(20)    NOT NULL DEFAULT 'PENDENTE',
    data_criacao         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_reserva PRIMARY KEY (id_reserva),
    CONSTRAINT uk_reserva_chave_locacao UNIQUE (id_reserva, id_cliente, id_veiculo),
    CONSTRAINT fk_reserva_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reserva_veiculo FOREIGN KEY (id_veiculo) REFERENCES veiculo (id_veiculo)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_reserva_periodo CHECK (data_fim > data_inicio),
    CONSTRAINT ck_reserva_valor   CHECK (valor_total_previsto >= 0),
    CONSTRAINT ck_reserva_status  CHECK (status IN
        ('PENDENTE', 'CONFIRMADA', 'CANCELADA', 'CONCLUIDA'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_reserva_veiculo_periodo ON reserva (id_veiculo, data_inicio, data_fim);
CREATE INDEX idx_reserva_cliente         ON reserva (id_cliente);
CREATE INDEX idx_reserva_status          ON reserva (status);

CREATE TABLE IF NOT EXISTS locacao (
    id_locacao                BIGINT         NOT NULL AUTO_INCREMENT,
    id_reserva                BIGINT         NULL,
    id_cliente                BIGINT         NOT NULL,
    id_veiculo                BIGINT         NOT NULL,
    data_retirada             DATETIME       NOT NULL,
    data_prevista_devolucao   DATETIME       NOT NULL,
    data_devolucao_efetiva    DATETIME       NULL,
    km_retirada               INT            NOT NULL,
    km_devolucao              INT            NULL,
    valor_diaria              DECIMAL(10,2)  NOT NULL,
    valor_total               DECIMAL(10,2)  NOT NULL,
    valor_multa               DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    status                    VARCHAR(20)    NOT NULL DEFAULT 'ATIVA',

    CONSTRAINT pk_locacao PRIMARY KEY (id_locacao),
    CONSTRAINT uk_locacao_reserva UNIQUE (id_reserva),
    CONSTRAINT fk_locacao_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_locacao_veiculo FOREIGN KEY (id_veiculo) REFERENCES veiculo (id_veiculo)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_locacao_reserva FOREIGN KEY (id_reserva, id_cliente, id_veiculo)
        REFERENCES reserva (id_reserva, id_cliente, id_veiculo)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_locacao_prevista  CHECK (data_prevista_devolucao > data_retirada),
    CONSTRAINT ck_locacao_efetiva   CHECK (data_devolucao_efetiva IS NULL
                                           OR data_devolucao_efetiva >= data_retirada),
    CONSTRAINT ck_locacao_km        CHECK (km_retirada >= 0
                                           AND (km_devolucao IS NULL OR km_devolucao >= km_retirada)),
    CONSTRAINT ck_locacao_valores   CHECK (valor_diaria > 0 AND valor_total >= 0 AND valor_multa >= 0),
    CONSTRAINT ck_locacao_status    CHECK (status IN ('ATIVA', 'ATRASADA', 'FINALIZADA')),
    CONSTRAINT ck_locacao_devolucao CHECK (
        (status = 'FINALIZADA' AND data_devolucao_efetiva IS NOT NULL AND km_devolucao IS NOT NULL)
        OR (status <> 'FINALIZADA' AND data_devolucao_efetiva IS NULL AND km_devolucao IS NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_locacao_veiculo_status ON locacao (id_veiculo, status);
CREATE INDEX idx_locacao_cliente        ON locacao (id_cliente);
CREATE INDEX idx_locacao_status_prevista ON locacao (status, data_prevista_devolucao);

CREATE TABLE IF NOT EXISTS pagamento (
    id_pagamento    BIGINT         NOT NULL AUTO_INCREMENT,
    id_locacao      BIGINT         NOT NULL,
    data_pagamento  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valor           DECIMAL(10,2)  NOT NULL,
    forma_pagamento VARCHAR(20)    NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDENTE',
    observacao      VARCHAR(255)   NULL,

    CONSTRAINT pk_pagamento PRIMARY KEY (id_pagamento),
    CONSTRAINT fk_pagamento_locacao FOREIGN KEY (id_locacao) REFERENCES locacao (id_locacao)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_pagamento_valor  CHECK (valor > 0),
    CONSTRAINT ck_pagamento_forma  CHECK (forma_pagamento IN
        ('PIX', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'DINHEIRO')),
    CONSTRAINT ck_pagamento_status CHECK (status IN
        ('PENDENTE', 'PAGO', 'PARCIAL', 'ESTORNADO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_pagamento_locacao ON pagamento (id_locacao);
CREATE INDEX idx_pagamento_status  ON pagamento (status);

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario    BIGINT        NOT NULL AUTO_INCREMENT,
    nome          VARCHAR(120)  NOT NULL,
    email         VARCHAR(120)  NOT NULL,
    senha_hash    VARCHAR(100)  NOT NULL,
    perfil        VARCHAR(20)   NOT NULL,
    ativo         BOOLEAN       NOT NULL DEFAULT TRUE,
    id_cliente    BIGINT        NULL,
    data_cadastro DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_cliente UNIQUE (id_cliente),
    CONSTRAINT fk_usuario_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ADMIN', 'VENDEDOR', 'CLIENTE')),
    CONSTRAINT ck_usuario_vinculo CHECK ((perfil = 'CLIENTE') = (id_cliente IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP PROCEDURE IF EXISTS sp_validar_disponibilidade_veiculo;
DROP PROCEDURE IF EXISTS sp_validar_cnh_cliente;
DROP TRIGGER IF EXISTS trg_reserva_bi;
DROP TRIGGER IF EXISTS trg_reserva_bu;
DROP TRIGGER IF EXISTS trg_locacao_bi;
DROP TRIGGER IF EXISTS trg_locacao_bu;

DELIMITER $$

CREATE PROCEDURE sp_validar_disponibilidade_veiculo(
    IN p_id_veiculo   BIGINT,
    IN p_inicio       DATETIME,
    IN p_fim          DATETIME,
    IN p_ignora_reserva BIGINT,
    IN p_ignora_locacao BIGINT)
BEGIN
    DECLARE v_status    VARCHAR(20);
    DECLARE v_conflitos INT DEFAULT 0;

    SELECT v.status INTO v_status
      FROM veiculo v WHERE v.id_veiculo = p_id_veiculo FOR UPDATE;

    IF v_status IN ('MANUTENCAO', 'INATIVO') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Veiculo indisponivel: em manutencao ou inativo.';
    END IF;

    SELECT COUNT(*) INTO v_conflitos
      FROM reserva r
     WHERE r.id_veiculo = p_id_veiculo
       AND r.status = 'CONFIRMADA'
       AND r.id_reserva <> COALESCE(p_ignora_reserva, 0)
       AND r.data_inicio < p_fim
       AND r.data_fim > p_inicio;

    IF v_conflitos > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Conflito de periodo com reserva confirmada do veiculo.';
    END IF;

    SELECT COUNT(*) INTO v_conflitos
      FROM locacao l
     WHERE l.id_veiculo = p_id_veiculo
       AND l.status IN ('ATIVA', 'ATRASADA')
       AND l.id_locacao <> COALESCE(p_ignora_locacao, 0)
       AND (l.id_reserva IS NULL OR p_ignora_reserva IS NULL OR l.id_reserva <> p_ignora_reserva)
       AND l.data_retirada < p_fim
       AND IF(l.status = 'ATRASADA',
              GREATEST(l.data_prevista_devolucao, NOW()),
              l.data_prevista_devolucao) > p_inicio;

    IF v_conflitos > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Conflito de periodo com locacao ativa do veiculo.';
    END IF;
END$$

CREATE PROCEDURE sp_validar_cnh_cliente(
    IN p_id_cliente BIGINT,
    IN p_data_fim   DATETIME)
BEGIN
    DECLARE v_validade  DATE;
    DECLARE v_situacao  VARCHAR(20);

    SELECT c.cnh_validade, c.cnh_situacao INTO v_validade, v_situacao
      FROM cliente c WHERE c.id_cliente = p_id_cliente;

    IF v_validade IS NOT NULL AND v_validade < DATE(p_data_fim) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'CNH do cliente vence antes do fim do periodo.';
    END IF;

    IF v_situacao IN ('VENCIDA', 'INEXISTENTE', 'IRREGULAR') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'CNH do cliente invalida (vencida, inexistente ou irregular).';
    END IF;
END$$

CREATE TRIGGER trg_reserva_bi BEFORE INSERT ON reserva
FOR EACH ROW
BEGIN
    IF NEW.status IN ('PENDENTE', 'CONFIRMADA') THEN
        CALL sp_validar_disponibilidade_veiculo(NEW.id_veiculo, NEW.data_inicio, NEW.data_fim, NULL, NULL);
        CALL sp_validar_cnh_cliente(NEW.id_cliente, NEW.data_fim);
    END IF;
END$$

CREATE TRIGGER trg_reserva_bu BEFORE UPDATE ON reserva
FOR EACH ROW
BEGIN
    IF NEW.status IN ('PENDENTE', 'CONFIRMADA')
       AND (OLD.status <> NEW.status
            OR OLD.id_veiculo <> NEW.id_veiculo
            OR OLD.id_cliente <> NEW.id_cliente
            OR OLD.data_inicio <> NEW.data_inicio
            OR OLD.data_fim <> NEW.data_fim) THEN
        CALL sp_validar_disponibilidade_veiculo(NEW.id_veiculo, NEW.data_inicio, NEW.data_fim, NEW.id_reserva, NULL);
        CALL sp_validar_cnh_cliente(NEW.id_cliente, NEW.data_fim);
    END IF;
END$$

CREATE TRIGGER trg_locacao_bi BEFORE INSERT ON locacao
FOR EACH ROW
BEGIN
    IF NEW.status IN ('ATIVA', 'ATRASADA') THEN
        CALL sp_validar_disponibilidade_veiculo(NEW.id_veiculo, NEW.data_retirada,
                                                NEW.data_prevista_devolucao, NEW.id_reserva, NULL);
        CALL sp_validar_cnh_cliente(NEW.id_cliente, NEW.data_prevista_devolucao);
    END IF;
END$$

CREATE TRIGGER trg_locacao_bu BEFORE UPDATE ON locacao
FOR EACH ROW
BEGIN
    IF NEW.status IN ('ATIVA', 'ATRASADA')
       AND (OLD.status NOT IN ('ATIVA', 'ATRASADA')
            OR OLD.id_veiculo <> NEW.id_veiculo
            OR OLD.id_cliente <> NEW.id_cliente
            OR OLD.data_retirada <> NEW.data_retirada
            OR OLD.data_prevista_devolucao <> NEW.data_prevista_devolucao) THEN
        CALL sp_validar_disponibilidade_veiculo(NEW.id_veiculo, NEW.data_retirada,
                                                NEW.data_prevista_devolucao, NEW.id_reserva, NEW.id_locacao);
        CALL sp_validar_cnh_cliente(NEW.id_cliente, NEW.data_prevista_devolucao);
    END IF;
END$$

DELIMITER ;
