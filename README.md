# Retro Cassette Player

Reprodutor de áudio Android nativo com interface minimalista inspirada no YouTube Music e um player retrô: fita cassete vertical numa placa de alumínio escovado, teclas de piano e régua de contador.

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
│   ├── SongCollection.kt        # Álbuns, artistas e playlists
│   ├── PlaylistRepository.kt    # Playlists do usuário (salvas no aparelho)
│   ├── FavoritesRepository.kt   # Músicas favoritas
│   └── MusicRepository.kt       # Consulta ao MediaStore
├── playback/
│   ├── PlaybackService.kt       # MediaSessionService + ExoPlayer
│   └── PlaybackConnection.kt    # MediaController -> StateFlow<PlaybackState>
└── ui/
    ├── RetroCassetteApp.kt      # Scaffold, permissões, NavHost, mini-player
    ├── navigation/Routes.kt
    ├── theme/                   # Paleta escura minimalista com o laranja da fita
    ├── components/
    │   ├── PianoKeys.kt         # Teclas de piano (afundam / PLAY fica travada)
    │   ├── QueueSheet.kt        # Painel "A seguir" com a fila
    │   ├── VerticalCassette.kt  # Fita vertical em Canvas, carretéis girando
    │   ├── RetroSeekBar.kt      # Régua 0–9 com marcador laranja
    │   ├── Sections.kt          # Chips, capas, cabeçalhos de seção
    │   ├── MiniPlayer.kt, RetroBottomBar.kt, SongRow.kt, AlbumArt.kt
    └── screens/                 # Início, Biblioteca, Coleção, Player, Buscar
```

## Destaques
- **Layout inspirado no YouTube Music**: Início com chips de artistas, "Jukebox de fitas" em páginas 3×3, "Escolha a dedo" em colunas e "Adicionadas recentemente"; Biblioteca com filtros (Playlists, Álbuns, Artistas, Músicas), ordenação, grade/lista e botão "Modo aleatório".
- **Player**: mesmo fundo escuro do app com um brilho laranja sutil; fita em pé com sombra suave, teclas de piano grafite; carretéis em 3D (rolo de fita com volume, cubo convexo com miolo rebaixado, luz fixa); a fita passa do carretel de cima para o de baixo conforme a minutagem, e o carretel mais vazio gira mais rápido (velocidade de fita constante).
- **Controles**: três teclas de piano (anterior, play/pause travando, próxima), aleatório e repetir (desligado / todas / uma).
- **Favoritas**: coração no player (fica laranja) ou "Adicionar às favoritas" no menu de qualquer música; playlist automática "Favoritas" no topo da Biblioteca.
- **Playlists**: crie pelo "+" da Biblioteca ou por "Salvar na playlist" (menu ⋮ das músicas, botão no player, botão "Salvar" em álbuns/artistas); renomeie, exclua, remova faixas e arraste pela alça ⠿ para reordenar na página da playlist.
- **A seguir**: fila de reprodução na ordem real (respeita o aleatório), tocar qualquer item ou remover da fila.
- **Seek**: toque ou arraste na régua; o contador mostra o tempo de destino enquanto você arrasta.
- **Lista**: título, artista e álbum, com menu de três pontos (Tocar, Tocar a seguir, Adicionar à fila).

## Como compilar
Abra a pasta no Android Studio (Ladybug ou mais recente, JDK 17) ou rode:
```
./gradlew assembleDebug
```
minSdk 26, target/compileSdk 35.
