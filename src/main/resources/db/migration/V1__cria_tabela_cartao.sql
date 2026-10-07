CREATE TABLE cartao (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    numero_cartao VARCHAR(19)   NOT NULL,
    senha         VARCHAR(100)  NOT NULL,
    saldo         DECIMAL(15,2) NOT NULL,
    versao        BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_cartao_numero_cartao UNIQUE (numero_cartao)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
