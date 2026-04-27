# Como gerar o APK SEM instalar nada no PC

Você vai usar o **GitHub Actions** — um serviço gratuito do GitHub que
compila o app pra você na nuvem. Leva ~3 minutos.

## Passo 1 — Conta no GitHub
Se você ainda não tem, crie uma conta grátis em https://github.com/signup
(é igual criar conta em qualquer site).

## Passo 2 — Criar um repositório novo
1. Faça login no GitHub.
2. Canto superior direito, clique em **+** → **New repository**.
3. Dê um nome qualquer (ex.: `wifi-radar`).
4. Marque **Private** se não quiser que seja público (tanto faz, ambos
   funcionam de graça).
5. **NÃO** marque "Add a README file" — vamos subir os nossos arquivos.
6. Clique em **Create repository**.

## Passo 3 — Subir os arquivos do projeto
Com o repositório criado e vazio, o GitHub mostra a tela "quick setup".

1. Clique no link **uploading an existing file** (ou vá em **Add file
   → Upload files**).
2. Abra a pasta `WifiRadar` no seu computador (a que veio do zip).
3. **Selecione todos os arquivos e pastas dentro de WifiRadar** e
   arraste pra janela do navegador. Importante:
   - Suba o **conteúdo** da pasta WifiRadar, não a pasta inteira.
   - Tem que vir junto a pasta oculta `.github` (com o workflow dentro).
   - Se o seu sistema esconde pastas começadas com `.`, ative
     "mostrar arquivos ocultos" antes de arrastar.
4. Role até o final da página e clique em **Commit changes**.

> 💡 Alternativa mais robusta: se você sabe usar o Git pela linha
> de comando, pode clonar o repo e dar `git push`. Mas não precisa.

## Passo 4 — Aguardar o build
1. Assim que você commita, vá na aba **Actions** do seu repositório.
2. Você vai ver um item "Build APK" rodando (com um círculo amarelo
   girando). Espere ele virar verde ✅. Demora cerca de 2 a 4 minutos
   na primeira vez.
3. Se virar vermelho ❌, clique em cima pra ver o erro — me copia a
   mensagem que eu te ajudo a resolver.

## Passo 5 — Baixar o APK
1. Quando o build terminar, clique em cima da execução verde.
2. Role até a seção **Artifacts** no rodapé.
3. Clique em **WifiRadar-APK** — vai baixar um arquivo zip.
4. Abra o zip — dentro tem o `app-debug.apk`. **Esse é o seu APK.**

## Passo 6 — Instalar no Microwear Ultra AI 3
Veja a seção "Como instalar no Microwear Ultra AI 3" do `README.md`
principal — é a mesma coisa.

---

## Build novo a cada mudança
Se depois você quiser alterar algo no código:
- Edita o arquivo direto no GitHub (clica no arquivo → ícone do lápis).
- Commita.
- O Actions roda de novo automaticamente e gera um APK atualizado.

## Rodar o build manualmente
Aba **Actions** → escolhe **Build APK** na barra lateral → botão
**Run workflow** no canto direito → **Run workflow**.
