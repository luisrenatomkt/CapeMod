# CapeMod Server (ETAPA 4)

API REST em Java 8 puro (`com.sun.net.httpserver`), SQLite, sem frameworks. Dependências: `sqlite-jdbc` (runtime) e JUnit (testes).

## Build e execução
```
cd server
mvn package            # roda os testes e gera target/capemod-server.jar
```
Desenvolvimento local (HTTP, só 127.0.0.1):
```
CAPEMOD_DEV_HTTP=true java -jar target/capemod-server.jar
```
Produção (HTTPS direto, keystore PKCS12):
```
CAPEMOD_KEYSTORE=/caminho/keystore.p12 CAPEMOD_KEYSTORE_PASSWORD=... java -jar target/capemod-server.jar
```
Alternativa: proxy reverso (nginx/Caddy) com TLS na frente e o servidor com `CAPEMOD_DEV_HTTP=true`.
Sem keystore e sem DEV_HTTP o servidor se recusa a iniciar.

Variáveis: `CAPEMOD_PORT` (8443), `CAPEMOD_DB_PATH` (data/capemod.db), `CAPEMOD_STORAGE_DIR` (data/capes),
`CAPEMOD_TOKEN_TTL_HOURS` (24), `CAPEMOD_PBKDF2_ITERATIONS` (120000), `CAPEMOD_MAX_CAPES_PER_USER` (20),
`CAPEMOD_AUTH_PER_MINUTE` (10), `CAPEMOD_UPLOAD_PER_MINUTE` (10).

## Endpoints
| Método | Caminho | Auth | Descrição |
|---|---|---|---|
| POST | /api/auth/register | não | `{"username","password"}` → 201 `{userId, playerId}` |
| POST | /api/auth/login | não | → 200 `{token, expiresAt, playerId}` |
| POST | /api/capes/upload | Bearer | multipart, campo `file` (PNG) → 201 `{capeId, sha256, textureUrl, active, warnings}` |
| GET | /api/capes/{playerId} | Bearer (dono) | lista as capas do jogador |
| PUT | /api/capes/{playerId}/active | Bearer (dono) | `{"capeId":"..."}` ou `{"capeId":null}` |
| DELETE | /api/capes/{capeId} | Bearer (dono) | remove a capa |
| GET | /api/players/{playerId}/cape | não | capa ativa (usado pelo mod ao ver outros jogadores) |
| GET | /api/textures/{sha256}.png | não | textura (cache imutável, ETag) |
| GET | /api/health | não | status |

## Decisões de segurança
- Senhas: PBKDF2-HMAC-SHA256 com sal; nunca em texto puro. Resposta de login igual para usuário inexistente e senha errada.
- Tokens: 256 bits aleatórios, expiram; só o SHA-256 é guardado no banco.
- Identidade: o `playerId` é um UUID do CapeMod ligado a uma conta autenticada; o nickname nunca define identidade.
  Donos são verificados em toda operação de escrita (404 para recursos de outros, sem revelar existência).
- Upload: limite de 1 MB, validação completa do PNG no servidor (assinatura, CRC, dimensões pelo cabeçalho, transparência),
  PNG regravado limpo; arquivo salvo como `{sha256}.png` (nome vem só do hash: sem path traversal, com deduplicação).
- Rate limit por IP (global, auth e upload). Redirecionamentos e `%`/`..` em caminhos são recusados.
- Atrás de proxy reverso o IP visto é o do proxy: o limite por IP fica menos útil nesse cenário (ver ETAPA 5/limitações).

## Limitações conhecidas
- Repositórios JDBC foram verificados só pelo SQL (esquema e consultas rodaram em SQLite); a execução com o driver
  `sqlite-jdbc` acontece no `mvn package` do GitHub Actions. A API em si foi testada ponta a ponta com repositórios em memória.
- PostgreSQL: trocar o driver e a URL em `Database` (SQL usado é padrão), ainda não testado.
- Associação de UUIDs reais do Minecraft e fluxo de login no mod: ETAPA 5.
