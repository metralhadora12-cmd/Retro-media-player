# Publicar o Retro Cassette na Google Play

Tudo o que é do app já está pronto neste repositório. Falta a parte que só o dono da conta pode fazer.

| O quê | Onde está |
|---|---|
| Pacote para a loja (.aab assinado) | Aba **Actions → Release (Google Play)** |
| Textos da loja (PT e EN) | `store/listing/pt-BR.md`, `store/listing/en-US.md` |
| Ícone 512×512 e imagem de destaque 1024×500 | `store/graphics/` |
| Capturas de tela 1080×1920 (PT e EN) | branch `store-screenshots` |
| Política de privacidade | `store/privacy-policy.md` |

---

## 1. Criar a conta de desenvolvedor

1. Acesse https://play.google.com/console e entre com a conta Google que será a dona do app.
2. Escolha **conta pessoal**, pague a taxa única de **US$ 25** e faça a verificação de identidade (documento com foto). A verificação pode levar alguns dias.
3. **Contas pessoais novas** precisam de um **teste fechado com pelo menos 12 testadores, ativos por 14 dias seguidos**, antes de liberar o app para todos (passo 7).

## 2. Guardar a chave de envio no GitHub

Você recebeu dois arquivos: `retro-cassette-upload.jks` (a chave) e `segredos-github.txt` (senhas). **Guarde os dois em local seguro e com cópia**, porque são eles que permitem enviar atualizações. Nunca coloque no repositório.

No GitHub: repositório → **Settings → Secrets and variables → Actions → New repository secret**. Crie os 4 segredos com os valores de `segredos-github.txt`:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

## 3. Gerar o pacote para a loja

1. Aba **Actions** → **Release (Google Play)** → **Run workflow**.
2. Quando terminar (~3 min), baixe o artefato **retro-cassette-release** e extraia o arquivo `.aab`.

## 4. Criar o app no Play Console

**Criar app** → nome **Retro Cassette: Player de Fita**, idioma padrão **Português (Brasil)**, **App**, **Gratuito**. Aceite as declarações.

A **Assinatura de apps do Google Play** vem ativada. Mantenha assim: o Google guarda a chave final e você usa só a chave de envio. Se um dia perder a chave de envio, dá para pedir outra ao suporte.

## 5. Ficha da loja

**Presença na loja → Ficha principal da loja**:

- **Nome**, **descrição curta** e **descrição completa**: copie de `store/listing/pt-BR.md`.
- **Ícone**: `store/graphics/icon-512.png`.
- **Imagem de destaque**: `store/graphics/feature-1024x500.png`.
- **Capturas de tela do telefone**: as da pasta `pt-BR` do branch `store-screenshots`.
- **Categoria**: Música e áudio.
- **Inglês**: em "Gerenciar traduções", adicione **Inglês (Estados Unidos)** com os textos de `store/listing/en-US.md` e as capturas da pasta `en-US`.

## 6. Conteúdo do app (Política → Conteúdo do app)

- **Política de privacidade**:
  https://github.com/metralhadora12-cmd/Retro-media-player/blob/claude/android-retro-audio-player-f5uwhy/store/privacy-policy.md
- **Anúncios**: não contém anúncios.
- **Acesso ao app**: todas as funções disponíveis sem login.
- **Classificação de conteúdo**: responda o questionário na categoria "Todos os outros tipos de app" e marque "não" em tudo. O resultado esperado é **Livre / Everyone**.
- **Público-alvo**: 13 anos ou mais. Não marque faixas infantis, porque isso ativa regras extras para apps de crianças.
- **Segurança dos dados**: o app não tem conta, anúncios nem análise de uso. As únicas conexões enviam nome da música, artista e álbum para buscar capas (iTunes, Deezer) e letras (LRCLIB), sem nenhum identificador seu. A resposta mais comum para esse caso é **"Não coleta nem compartilha dados"**. Se preferir ser mais conservador, declare **Atividade no app → Outras ações** como "compartilhado", "não vinculado ao usuário" e "opcional". Em ambos os casos, marque que os dados são **criptografados em trânsito**. A decisão é sua, como responsável pelo app.
- **Serviço em primeiro plano** (a Google pergunta por causa da reprodução em segundo plano). Tipo: **Reprodução de mídia**. Descrição sugerida:
  > "O app toca as músicas do usuário em segundo plano, com controles na notificação, tela de bloqueio, widget e Android Auto. O serviço só roda enquanto há reprodução."
- **Permissão de áudio** (Músicas e áudio): usada para listar e tocar as músicas do aparelho, que é a função principal do app.

## 7. Teste fechado (obrigatório para conta pessoal nova)

1. **Testar → Teste fechado → Criar faixa**. Crie uma lista de e-mails com **12 ou mais pessoas** (amigos, família).
2. **Criar versão** → envie o `.aab` → nas notas da versão, cole o texto da versão 3.0 do app (Configurações → Novidades).
3. Envie para revisão. Depois de aprovado, os testadores entram pelo link de participação e instalam pela Play Store.
4. Mantenha **pelo menos 12 testadores inscritos por 14 dias seguidos**.
5. Depois disso, em **Painel → Solicitar acesso à produção**, responda o formulário sobre o teste.

## 8. Produção

Com o acesso liberado: **Produção → Criar versão** → use o mesmo `.aab` (ou um mais novo) → **Enviar para revisão**. A primeira revisão costuma levar de alguns dias a uma semana.

### Android Auto (opcional)

O app já funciona no Android Auto. Para aparecer para usuários do Auto pela loja, ative **Configurações avançadas → Formatos → Android Auto**. Isso inclui uma revisão extra de qualidade para carros. Se preferir publicar antes sem ela, deixe desativado por enquanto.

---

## Atualizações futuras

Cada envio à loja precisa de um **versionCode** maior (em `app/build.gradle.kts`). É só pedir a próxima versão que eu aumento o número, atualizo as novidades e você roda o passo 3 de novo.

**Atenção:** o app instalado da Play Store é assinado com outra chave, diferente da dos APKs de teste do GitHub. Por isso, a primeira instalação pela loja exige desinstalar a versão de teste antes. Faça um **backup das playlists e favoritas** em Configurações antes de desinstalar.
