# CapeMod
- `mod/`: mod Forge 1.8.9 (Java 8). Build: `cd mod && ./gradlew setupDecompWorkspace build` (requer Gradle 2.14 wrapper e internet).
- `server/`: API REST + SQLite (ETAPA 4). Veja `server/README.md`.


## Compilar no GitHub
1. Crie um repositório e envie TUDO (a pasta `.github` precisa ficar na raiz, ao lado de `mod/`).
2. Aba **Actions** -> workflow "Build CapeMod" (roda a cada push, ou clique em Run workflow).
3. Ao terminar, baixe o jar em **Artifacts -> CapeMod-1.8.9**.

## Status
- ETAPA 1: esqueleto + tecla C (validada no jogo).
- ETAPA 2: menu gráfico (menu, galeria local, importar PNG, selecionar/remover, prévia, indicador de conexão).
  Upload remoto e rede: ETAPAS 3-6. Exibição da capa no personagem: ETAPA 7.

## ETAPA 3 - Upload e validação
- `CapeValidator` (textures): tamanho ≤ 1 MB, assinatura PNG, CRC de todos os chunks, sem APNG/dados extras,
  tamanhos 64x32 e dobros até 1024x512 (checados pelo cabeçalho antes de decodificar), transparência da face da capa.
- `CapeUploader` (networking): POST multipart em thread própria, só HTTPS, sem seguir redirecionamentos, com progresso.
  Contrato: `POST {apiBaseUrl}/api/capes/upload`, campo `file`, `Authorization: Bearer <token>`.
- Config (`config/capemod.cfg`): `apiBaseUrl`, `authToken` (preenchido pelo login na ETAPA 5),
  `allowInsecureLocalhost` (só desenvolvimento).
- Testes: `gradle test` (JUnit 4, `CapeValidatorTest`).
- O upload real só pode ser testado de ponta a ponta quando a API existir (ETAPA 4).

- ETAPA 4: servidor/API REST (`server/`), banco SQLite, armazenamento de texturas, autenticação básica, testes de API.
