USE autorent;

INSERT INTO usuario (nome, email, senha_hash, perfil, ativo, id_cliente)
SELECT 'Administrador AutoRent', 'admin@autorent.local', '$2a$10$mjcNwC9WbEsMFaODlf4hQ.kMUGF8TtK/Fzc5MWn0S/XNGDH4/zKLC', 'ADMIN', TRUE, NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE email = 'admin@autorent.local');

INSERT INTO usuario (nome, email, senha_hash, perfil, ativo, id_cliente)
SELECT 'Vendedor AutoRent', 'vendedor@autorent.local', '$2a$10$suyaDmkT9BehOBaOUF8ITODPShaDq450PXosRhX4OOpVdkcxIRauu', 'VENDEDOR', TRUE, NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE email = 'vendedor@autorent.local');

INSERT INTO usuario (nome, email, senha_hash, perfil, ativo, id_cliente)
SELECT c.nome_completo, c.email, '$2a$10$INR02/opTcmzzRgSFryVVOCBBXBE6gMvnmdm6d8UzJcVjE1UGPswK', 'CLIENTE', TRUE, c.id_cliente
FROM cliente c
WHERE c.email IN ('ana.souza@exemplo.com', 'bruno.lima@exemplo.com', 'carla.ferreira@exemplo.com')
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.email = c.email);
