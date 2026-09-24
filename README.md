# Retro Cassette Player

Reprodutor de áudio Android nativo com interface minimalista inspirada no YouTube Music e um player retrô: fita cassete vertical numa placa de alumínio escovado, teclas de piano e régua de contador.

## Stack
- **Kotlin** + **Jetpack Compose** (Material 3)
- **Áudio lossless**: FLAC, WAV e ALAC (decodificador FFmpeg `org.jellyfin.media3:media3-ffmpeg-decoder` como reserva), saída em ponto flutuante para hi-res 24 bits, selo LOSSLESS no player e filtro na Biblioteca
- **Media3 / ExoPlayer** rodando dentro de um `MediaSessionService` (tocar em segundo plano, notificação de mídia e tela de bloqueio)
- **Navigation Compose** com barra de navegação inferior (Início, Buscar, Biblioteca)
- **MediaStore** para ler as músicas locais (ignora áudios de WhatsApp, Telegram e Messenger) (`READ_MEDIA_AUDIO` no Android 13+, `READ_EXTERNAL_STORAGE` em versões anteriores)
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
- **Playlists**: crie pelo "+" da Biblioteca ou por "Salvar na playlist" (toque longo nas músicas, botão no player, botão "Salvar" em álbuns/artistas); renomeie, exclua, remova faixas e arraste pela alça ⠿ para reordenar na página da playlist.
- **Menu por toque longo**: nas músicas e nos itens da Biblioteca (tocar, aleatório, fila, salvar; nas suas playlists também editar, renomear e excluir). Nas músicas há também "Excluir do aparelho", com confirmação do sistema no Android 10+.
- **Configurações** (engrenagem na Início e na Biblioteca): equalizador, idioma, recarregar biblioteca, novidades e versão.
- **Edição de tags**: título, artista, álbum, artista do álbum, ano, faixa e gênero gravados no próprio arquivo (JAudioTagger); capa da galeria ou buscada online (iTunes e Deezer), com opção de aplicar ao álbum inteiro.
- **Renomear músicas**: toque longo → Renomear; altera o título nas tags e, opcionalmente, o nome do arquivo (o id no MediaStore é mantido, então playlists e favoritas continuam valendo).
- **Letras**: aba LETRA no player; letra da própria faixa, de um arquivo .lrc ou online pelo LRCLIB (grátis, sem chave). Letras sincronizadas destacam a linha atual e permitem pular tocando nela; ficam guardadas no aparelho.
- **Backup**: exporta playlists e favoritas para um arquivo .json e restaura (mesclando), inclusive em outro aparelho — as músicas são reencontradas por título/artista/álbum.
- **Tema**: escuro (padrão), claro ou seguindo o sistema.
- **Idiomas**: português (padrão), inglês e espanhol. No Android 13+ usa o idioma por app do sistema.
- **Equalizador integrado**: bandas do aparelho em faders verticais, predefinições, reforço de graves; configurações salvas e reaplicadas à sessão de áudio do ExoPlayer.
- **A seguir**: fila de reprodução na ordem real (respeita o aleatório), tocar qualquer item ou remover da fila.
- **Seek**: toque ou arraste na régua; o contador mostra o tempo de destino enquanto você arrasta.
- **Lista**: título, artista e álbum, com menu de três pontos (Tocar, Tocar a seguir, Adicionar à fila).

## Como compilar
Abra a pasta no Android Studio (Ladybug ou mais recente, JDK 17) ou rode:
```
./gradlew assembleDebug
```
minSdk 26, target/compileSdk 35.
