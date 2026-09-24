# Retro Cassette Player

Reprodutor de áudio Android nativo com estética de Walkman / tape deck metálico.

## Stack
- **Kotlin** + **Jetpack Compose** (Material 3)
- **Media3 / ExoPlayer** rodando dentro de um `MediaSessionService` (tocar em segundo plano, notificação de mídia e tela de bloqueio)
- **Navigation Compose** com barra de navegação inferior (Início, Buscar, Biblioteca)
- **MediaStore** para ler as músicas locais (`READ_MEDIA_AUDIO` no Android 13+, `READ_EXTERNAL_STORAGE` em versões anteriores)
- **Coil** para as capas dos álbuns

## Estrutura
```
app/src/main/java/com/retro/cassetteplayer/
├── MainActivity.kt              # Entrada, edge-to-edge
├── MainViewModel.kt             # Biblioteca, busca, álbuns, ações do player
├── data/
│   ├── Song.kt                  # Modelo + conversão para MediaItem
│   ├── SongCollection.kt        # Álbuns, artistas e playlists automáticas
│   └── MusicRepository.kt       # Consulta ao MediaStore
├── playback/
│   ├── PlaybackService.kt       # MediaSessionService + ExoPlayer
│   └── PlaybackConnection.kt    # MediaController -> StateFlow<PlaybackState>
└── ui/
    ├── RetroCassetteApp.kt      # Scaffold, permissões, NavHost, mini-player
    ├── navigation/Routes.kt
    ├── theme/                   # Paleta (#1E1E1E, #2B2B2B, #D35400, #F5E6CA...), tipografia mono
    ├── components/
    │   ├── BrushedMetal.kt      # Textura de metal escovado
    │   ├── MetalButton.kt       # Teclas metálicas em relevo (afundam / travam)
    │   ├── Cassette.kt          # Fita cassete em Canvas, carretéis girando, caixa de acrílico
    │   ├── RetroSeekBar.kt      # Régua graduada com agulha laranja + contadores LCD
    │   ├── Sections.kt          # Chips, capas, cabeçalhos de seção
    │   ├── MiniPlayer.kt, RetroBottomBar.kt, SongRow.kt, AlbumArt.kt
    └── screens/                 # Início, Biblioteca, Coleção, Player, Buscar
```

## Destaques
- **Layout inspirado no YouTube Music**: Início com chips de artistas, "Jukebox de fitas" em páginas 3×3, "Escolha a dedo" em colunas e "Adicionadas recentemente"; Biblioteca com filtros (Playlists, Álbuns, Artistas, Músicas), ordenação, grade/lista e botão "Modo aleatório".
- **Carretéis animados**: `rememberReelRotation(isPlaying)` gira os hubs enquanto toca e para no mesmo ângulo quando pausado. Os rolos de fita passam do carretel esquerdo para o direito conforme o progresso da faixa.
- **Controles de deck**: anterior, retroceder 10 s, play/pause (a tecla fica "travada" enquanto toca), avançar 10 s, próxima.
- **Seek**: toque ou arraste na régua; o contador mostra o tempo de destino enquanto você arrasta.
- **Lista**: título, artista e álbum, com menu de três pontos (Tocar, Tocar a seguir, Adicionar à fila).

## Como compilar
Abra a pasta no Android Studio (Ladybug ou mais recente, JDK 17) ou rode:
```
./gradlew assembleDebug
```
minSdk 26, target/compileSdk 35.
