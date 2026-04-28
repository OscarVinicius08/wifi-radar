# 📡 WiFi Radar V3 — Microwear Ultra AI 3

Ferramenta de reconhecimento e auditoria Wi-Fi para smartwatch Android.
Desenvolvida para a tela AMOLED de 2.06" do Microwear Ultra AI 3.

---

## 🧭 Navegação — 9 abas (deslize para os lados)

| # | Aba | Função |
|---|-----|--------|
| 1 | 📡 **Radar** | Radar circular animado — redes como pontos por distância estimada |
| 2 | 📋 **Lista** | Tabela completa: SSID, canal, banda, dBm, segurança, distância |
| 3 | 🎯 **Medidor** | Gauge analógico da rede mais forte com ponteiro e zonas de cor |
| 4 | 📊 **Canais** | Spectrum analyzer — curvas por canal, alterna 2.4GHz / 5GHz |
| 5 | 🛡 **Ameaças** | Evil Twin · ARP Spoofing · DNS suspeito · Honeypot · SSID oculto |
| 6 | 📱 **Dispositivos** | Scanner da rede local + port scan por dispositivo |
| 7 | 🔐 **Auditoria** | Score 0-100 · WPS · Fabricante (OUI) · Risco por rede |
| 8 | 🛠 **Ferramentas** | Ping · Traceroute · Port Scan · DNS Lookup |
| 9 | 📋 **Relatório** | Timeline de eventos · Estatísticas · Exportar TXT |

---

## 🔍 Detecções de Segurança

- **Evil Twin** — detecta dois APs com mesmo SSID em canais diferentes (possível AP falso)
- **ARP Spoofing** — monitora mudança de MAC do gateway (ataque man-in-the-middle)
- **DNS suspeito** — alerta se o DNS configurado não é privado nem público conhecido
- **Honeypot** — detecta rede aberta com mesmo SSID de rede protegida
- **SSID oculto** — conta redes sem nome visível
- **WPS ativo** — flag em redes com WPS habilitado (vulnerabilidade conhecida)
- **Score de risco** — nota 0 a 100 para o ambiente Wi-Fi ao redor

---

## 🛠 Ferramentas de Rede

- **Ping** — 5 sequências com latência em ms e código de cor
- **Traceroute** — caminho dos pacotes até o destino
- **Port Scan** — verifica portas comuns (FTP, SSH, Telnet, HTTP, HTTPS, SMB, RDP...)
- **DNS Lookup** — resolve nomes para IPs
- **Device Scanner** — varre todos os 254 hosts da sub-rede local
- **Port Scan por dispositivo** — testa portas em qualquer host encontrado

---

## ⚙️ Comportamento

- **Auto-scan**: atualiza a cada ~17s automaticamente (limite do Android 9)
- **Toque na tela**: força scan imediato no Radar
- **Timeline**: log cronológico de todos os eventos e ameaças detectadas
- **Exportar relatório**: gera TXT completo compartilhável por WhatsApp/e-mail

---

## ⚠️ Limitações honestas

- **Distância estimada** tem erro de ±50% em ambientes fechados (paredes atenuam o sinal)
- **Ângulo no radar** é derivado do BSSID — estável entre scans, mas não é direção real
- **Deauth/injeção de pacotes** é impossível sem modo monitor no driver Wi-Fi (bloqueado no Android)
- **Device Scanner** pode ser lento (~30s) dependendo do tamanho da rede

---

## 📲 Instalação

1. Baixe o APK na aba **Actions → Build APK → Artifacts**
2. No relógio: **Configurações → Segurança → Fontes desconhecidas → Ativar**
3. Transfira o APK (USB, Bluetooth ou ADB)
4. Instale e conceda permissão de **Localização** ao abrir

---

## 🏗 Compilar você mesmo

Sem Android Studio — use o **GitHub Actions** incluso:
1. Faça fork ou upload para seu repositório
2. A aba **Actions** compila automaticamente a cada push
3. Baixe o APK em **Actions → execução mais recente → Artifacts → WifiRadar-APK**

---

## 📁 Estrutura do projeto

WifiRadar/
├── app/src/main/java/com/wifiradar/app/
│   ├── MainActivity.java       — orquestra scan, permissões e abas
│   ├── WatchPagerAdapter.java  — 9 abas com swipe
│   ├── WifiNetwork.java        — modelo de rede + cálculos
│   ├── ThreatDetector.java     — Evil Twin, ARP Spoof, DNS, Honeypot
│   ├── NetworkScanner.java     — scanner de dispositivos e portas
│   ├── OuiDatabase.java        — lookup offline de fabricante por MAC
│   ├── RadarView/Fragment      — aba 1: radar circular
│   ├── ListFragment            — aba 2: lista de redes
│   ├── MeterView/Fragment      — aba 3: gauge analógico
│   ├── ChannelView/Fragment    — aba 4: spectrum analyzer
│   ├── ThreatsFragment         — aba 5: ameaças ativas
│   ├── DevicesFragment         — aba 6: dispositivos na rede
│   ├── AuditFragment           — aba 7: auditoria de segurança
│   ├── ToolsFragment           — aba 8: ferramentas de rede
│   └── ReportFragment          — aba 9: relatório e timeline
└── .github/workflows/build.yml — CI/CD automático

---

*WiFi Radar V3 — reconhecimento passivo profissional no pulso*

Feito com ajuda do Claude code PRO.
