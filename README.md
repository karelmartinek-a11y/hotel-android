# HOTEL Android (ASC Hotel Chodov) – build + podpis APK (release)

Doména (kanonická): `https://hotel.hcasc.cz`

Tento projekt se distribuuje mimo Google Play jako APK.
APK se na server nahrává na přesnou cestu:

- `/var/www/hotelapp/download/app.apk`
- veřejná URL: `https://hotel.hcasc.cz/download/app.apk`

Zakázáno: FCM / Firebase / Play Integrity / jakákoliv runtime závislost na Google službách.

---

## 1) Požadavky na build stroji

Build doporučeně dělej na vývojářském PC (Linux/macOS/Windows). Build na serveru je možný, ale není doporučen.

Nainstaluj:

1. **JDK 17** (doporučeno)
2. **Android SDK** (přes Android Studio nebo `sdkmanager`)
3. (volitelné) Android Studio pro pohodlí

Ověření v terminálu:

```bash
java -version
```

---

## 2) Otevření projektu

Projekt je v této složce:

- `hotel/android/`

Android Studio:

1. File → Open
2. Vyber adresář `hotel/android`
3. Nech doběhnout Gradle sync (stahuje závislosti – to je povoleno)

---

## 3) Nastavení verze aplikace

V souboru:

- `hotel/android/app/build.gradle.kts`

uprav:

- `versionCode` – vždy zvyš (integer)
- `versionName` – např. `1.0.0`

Doporučení:

- `versionCode`: inkrement o 1 při každém releasu
- `versionName`: semver

---

## 4) Vygenerování release keystore (jednorázově)

Keystore vytvoř jen jednou a bezpečně ulož mimo repozitář.

Vytvoření:

```bash
keytool -genkeypair \
  -v \
  -keystore hotel-release.jks \
  -alias hotel \
  -keyalg RSA \
  -keysize 4096 \
  -validity 3650
```

Poznámky:

- Heslo keystore a heslo klíče si bezpečně ulož.
- Keystore nikdy necommituj.

---

## 5) Konfigurace signing pro Gradle

Doporučený způsob je přes `gradle.properties` (lokálně) nebo přes environment proměnné v CI.

### Varianta A (lokální `~/.gradle/gradle.properties`)

Otevři (nebo vytvoř):

- `~/.gradle/gradle.properties`

Přidej:

```properties
HOTEL_STORE_FILE=/absolutni/cesta/k/hotel-release.jks
HOTEL_STORE_PASSWORD=***
HOTEL_KEY_ALIAS=hotel
HOTEL_KEY_PASSWORD=***
```

### Varianta B (env proměnné v shellu/CI)

V CI nastav stejné hodnoty jako env a v `build.gradle.kts` je mapuj.

---

## 6) Build release APK

V terminálu přejdi do:

```bash
cd hotel/android
```

Build:

```bash
./gradlew clean :app:assembleRelease
```

Výstupní APK:

- `hotel/android/app/build/outputs/apk/release/app-release.apk`

---

## 7) (Volitelné) zipalign + apksigner (pokud nepodepisuje Gradle)

V produkci preferuj, aby release APK bylo podepsané přímo z Gradle signingConfig.
Pokud by bylo potřeba ručně:

1) zipalign:

```bash
zipalign -v -p 4 \
  app/build/outputs/apk/release/app-release.apk \
  app/build/outputs/apk/release/app-release-aligned.apk
```

2) apksigner:

```bash
apksigner sign \
  --ks /absolutni/cesta/k/hotel-release.jks \
  --ks-key-alias hotel \
  --out app/build/outputs/apk/release/app-release-signed.apk \
  app/build/outputs/apk/release/app-release-aligned.apk
```

3) ověření podpisu:

```bash
apksigner verify --verbose app/build/outputs/apk/release/app-release-signed.apk
```

---

## 8) Nahrání APK na server

APK musí na serveru skončit jako:

- `/var/www/hotelapp/download/app.apk`

### 8.1) Přenos na server (scp)

Na build stroji:

```bash
scp app/build/outputs/apk/release/app-release.apk \
  root@TVUJ_SERVER:/tmp/app.apk
```

### 8.2) Přesun do cílové cesty

Na serveru (SSH):

```bash
ssh root@TVUJ_SERVER
```

Zkontroluj, že existuje adresář:

```bash
mkdir -p /var/www/hotelapp/download
```

Přesuň soubor a nastav práva:

```bash
mv /tmp/app.apk /var/www/hotelapp/download/app.apk
chown -R hotelapp:hotelapp /var/www/hotelapp
chmod 0644 /var/www/hotelapp/download/app.apk
```

---

## 9) Ověření stažení APK z webu

Na serveru ověř, že Nginx servíruje soubor:

```bash
curl -I https://hotel.hcasc.cz/download/app.apk
```

Zkontroluj:

- HTTP 200
- `Content-Type` je vhodný pro APK (typicky `application/vnd.android.package-archive`)
- hlavičky cache (doporučeno) jsou nastavené dle server konfigurace

---

## 10) Ověření instalace na zařízení

1. V Androidu povol „Instalaci z neznámých zdrojů“ pro prohlížeč/správce souborů
2. Otevři `https://hotel.hcasc.cz` na zařízení
3. Klepni na velké tlačítko stažení APK
4. Nainstaluj

Po spuštění aplikace:

- Pokud není internet a zařízení ještě není aktivované: stav **„Připojte OnLine“**
- Pokud je internet a zařízení je PENDING/REVOKED: stav **„Aktivujte“** (čeká na aktivaci adminem)
- Po aktivaci adminem: stav **„Vítejte doma“** a dvojité klepnutí otevře aplikaci

---

## 11) Poznámky k provozu

- Android aplikace nepoužívá žádné heslo.
- Aktivace probíhá pouze přes web admin na `https://hotel.hcasc.cz/admin`.
- Notifikace jsou řešeny periodickým pollingem přes WorkManager (bez FCM).
- Offline vytvořené hlášení se uloží do Room fronty a po úspěšném odeslání se lokálně smaže.
