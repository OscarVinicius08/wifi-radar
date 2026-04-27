# WiFi Radar — para Microwear Ultra AI 3

App Android que escaneia redes Wi-Fi próximas e mostra cada uma como
um ponto num radar circular, em distância proporcional à força do sinal.
Pensado para a tela AMOLED de ~2.06" (466x466 px) do Microwear Ultra AI 3.

---

## ⚠️ O que esse app FAZ e o que NÃO FAZ — leia antes

### Faz
- Lista todas as redes Wi-Fi visíveis (SSID, BSSID, RSSI, canal).
- **Estima a distância** de cada AP em metros usando o modelo Free Space
  Path Loss (FSPL).
- Plota cada rede num radar de 4 anéis (5m, 15m, 30m, 60m).
- Cor do ponto = força do sinal (verde forte → vermelho fraco).
- Atualiza a cada ~30 s ou ao toque na tela.

### NÃO faz (limitação física, não preguiça)
- **Não mostra a direção real de onde vem o sinal.** Determinar azimute
  de um sinal Wi-Fi exige um array de antenas direcionais — o smartwatch
  tem uma única antena omnidirecional. O ângulo no radar é derivado de
  forma estável do BSSID (cada rede sempre aparece no mesmo lugar entre
  scans), mas é puramente visual.
- **Não localiza o roteador num mapa.** Para isso seria necessário
  consultar uma base externa tipo WiGLE com o BSSID — fica como evolução
  futura (item "ideias" abaixo).
- A distância em metros é uma **estimativa** com erro típico de ±50% em
  ambientes fechados (paredes, multipath, potência real desconhecida do AP).

---

## Como compilar

### 🚀 Opção mais fácil — sem instalar NADA no PC
Veja o arquivo **`COMO_GERAR_APK_SEM_PC.md`** — usa GitHub Actions e
gera o APK na nuvem em ~3 minutos. Recomendado se você não quer baixar
o Android Studio (5 GB).

### Opção A — Android Studio (recomendado se você for programar)

1. Abra o Android Studio (qualquer versão recente — Hedgehog, Iguana, Koala).
2. **File → Open** e selecione a pasta `WifiRadar`.
3. Aguarde o Gradle sync. Se pedir para atualizar o Gradle plugin, aceite.
4. Clique em **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. Quando aparecer "APK(s) generated successfully", clique em **locate** —
   o APK estará em `app/build/outputs/apk/debug/app-debug.apk`.

### Opção B — Linha de comando

```bash
cd WifiRadar
# Se você não tem o gradle wrapper, gere com:
gradle wrapper
# Depois:
./gradlew assembleDebug
```

APK final: `app/build/outputs/apk/debug/app-debug.apk`

> **Nota:** este zip não inclui `gradlew` e `gradle-wrapper.jar` para
> ficar leve. Da primeira vez que abrir no Android Studio ele baixa
> tudo automaticamente. Em CLI use o comando `gradle wrapper` acima
> (precisa ter Gradle 8+ instalado).

---

## Como instalar no Microwear Ultra AI 3

Esses smartwatches Android rodam um sistema **Android padrão** (não Wear OS),
então é instalação igual a celular:

1. **Habilite "Fontes desconhecidas"** nas configurações do relógio
   (Settings → Security → Unknown sources, ou similar — varia por lote).
2. **Transferência do APK** — três caminhos comuns:
   - **Cabo USB:** conecte o relógio ao PC, copie o `app-debug.apk` para
     o armazenamento interno, e use um file manager no relógio para
     instalar.
   - **Bluetooth:** envie o APK do celular para o relógio.
   - **ADB sem fio:** se o relógio expõe ADB pela rede (alguns lotes
     fazem por padrão na porta 5555), use:
     ```
     adb connect <IP_DO_RELOGIO>:5555
     adb install app-debug.apk
     ```
   - **Download direto:** abra um navegador no relógio e baixe o APK
     de um link próprio.
3. Toque no APK para instalar e aceite os avisos de "instalação de
   app desconhecido".
4. Ao abrir, conceda **Permissões de Localização** (sem isso o
   Android 8.1+ NÃO retorna a lista de redes Wi-Fi, mesmo que você só
   queira escanear).

---

## Como usar

- A tela é o radar inteiro, fundo preto (economia de bateria AMOLED).
- **Toque em qualquer parte** = força um novo scan.
- Texto verde no topo = status do scan.
- Texto cinza embaixo = quantidade de redes detectadas.
- Cada ponto colorido = uma rede.
  - 🟢 verde claro: sinal excelente (≥ -50 dBm, ~5m)
  - 🟡 amarelo:    sinal médio   (-60 a -70 dBm, ~15-30m)
  - 🟠 laranja:    sinal fraco   (-70 a -80 dBm, ~30-60m)
  - 🔴 vermelho:   sinal péssimo (≤ -80 dBm, > 60m)

---

## Throttling do scan no Android 9

A partir do Android 9, `WifiManager.startScan()` é limitado a
**~4 chamadas a cada 2 minutos** por app. O app respeita isso —
quando o sistema bloqueia um novo scan, ele exibe "cache (aguarde)"
e mostra o último resultado conhecido. Não é bug.

---

## Ideias para evoluir

Se você quiser expandir, esses são caminhos viáveis:
- **Triangulação manual:** caminhar 3 pontos diferentes anotando RSSI
  em cada → calcular interseção. Daria localização ~real do AP.
- **WiGLE API:** consultar `https://api.wigle.net` com o BSSID retorna
  lat/long se a rede já tiver sido mapeada por outro usuário.
- **Histórico de RSSI:** gravar últimos 60s e mostrar tendência
  (você está se aproximando ou afastando do AP).
- **Filtro por SSID:** modo "encontrar minha rede" que destaca apenas
  um SSID alvo no radar.

---

## Estrutura

```
WifiRadar/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/wifiradar/app/
│       │   ├── MainActivity.java   ← orquestra permissões e scan
│       │   ├── RadarView.java      ← desenha o radar circular animado
│       │   └── WifiNetwork.java    ← modelo + cálculo de distância
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/{strings,styles,colors}.xml
│           ├── drawable/ic_launcher_foreground.xml
│           └── mipmap-*/ic_launcher.png
├── build.gradle
├── settings.gradle
└── gradle.properties
```
