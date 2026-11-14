# CapeMod
- `mod/`: mod Forge 1.8.9 (Java 8). Build: `cd mod && ./gradlew setupDecompWorkspace build` (requer Gradle 2.14 wrapper e internet).
- `server/`: API REST (ETAPA 4).


## Compilar no GitHub
1. Crie um repositório e envie TUDO (a pasta `.github` precisa ficar na raiz, ao lado de `mod/`).
2. Aba **Actions** -> workflow "Build CapeMod" (roda a cada push, ou clique em Run workflow).
3. Ao terminar, baixe o jar em **Artifacts -> CapeMod-1.8.9**.

## Status
- ETAPA 1: esqueleto + tecla C (validada no jogo).
- ETAPA 2: menu gráfico (menu, galeria local, importar PNG, selecionar/remover, prévia, indicador de conexão).
  Upload remoto e rede: ETAPAS 3-6. Exibição da capa no personagem: ETAPA 7.
