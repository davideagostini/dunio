# Wear OS Google Play Publishing Guide

## Obiettivo

Questa guida descrive come pubblicare su Google Play la versione Wear OS di Dunio, partendo dalla configurazione attuale del progetto Android.

È pensata come guida operativa interna, quindi include:

- passaggi Play Console
- controlli tecnici sul modulo Wear
- note specifiche per l'architettura attuale del progetto
- checklist finale prima del rilascio

## Stato attuale del progetto

Al momento il progetto contiene un modulo Wear dedicato:

- modulo: `:wear`
- namespace: `com.davideagostini.summ.wearapp`
- applicationId: `com.davideagostini.summ`
- minSdk: `30`
- targetSdk: `36`
- versione attuale:
  - `versionCode = 3601001`
  - `versionName = "0.1.0-wear"`

L'app Wear è configurata come **non standalone**:

- in manifest è presente `com.google.android.wearable.standalone=false`

Questo significa che, nella configurazione attuale, l'esperienza Wear dipende dal telefono per il salvataggio dati e per la sincronizzazione della quick entry.

## Implicazione importante prima della pubblicazione

Prima di caricare la prima release su Google Play, dobbiamo confermare la strategia di packaging.

### Configurazione attuale

Il modulo Wear usa:

- `applicationId = com.davideagostini.summ`

che coincide con il package dell'app Android principale.

Questa scelta ha implicazioni pratiche:

- la firma della release Wear deve essere coerente con quella dell'app phone
- la strategia Play Console deve essere allineata con il fatto che phone e Wear condividono la stessa app identity
- la prima pubblicazione va fatta con attenzione, perché cambiare questa struttura dopo il rilascio è molto più scomodo

### Decisione da confermare

Prima del rilascio produzione conviene verificare esplicitamente se vogliamo:

1. mantenere phone + Wear nello stesso ecosistema di app/package
2. oppure trattare Wear come esperienza separata con package dedicato

Con la configurazione attuale, la direzione naturale è la prima.

## Prerequisiti tecnici

Prima di aprire la Play Console, verifichiamo questi punti.

### 1. Release build funzionante

Eseguire almeno:

```bash
./gradlew :wear:assembleRelease
./gradlew :wear:bundleRelease
```

Per Google Play è preferibile il file:

- `.aab` prodotto da `bundleRelease`

### 2. Firma release

Verificare che il modulo Wear venga firmato correttamente in release.

Controlli:

- configurazione `signingConfig` release
- keystore corretta
- stessa signing identity dell'app phone se si mantiene lo stesso package

Se la firma non è coerente, Play può rifiutare l'upload oppure trattare male l'aggiornamento rispetto alla app già esistente.

### 3. Versioning

Prima di ogni upload:

- incrementare `versionCode`
- aggiornare `versionName` se necessario

Regola pratica:

- `versionCode` deve sempre crescere
- non riusare mai un `versionCode` già caricato su Play

### 4. Manifest Wear corretto

Verificare nel manifest Wear:

- feature watch presente
- launcher activity corretta
- servizi Wear dichiarati
- metadata standalone coerente

Controlli attuali già presenti:

- `android.hardware.type.watch`
- `WearQuickEntryActivity`
- `WearQuickEntrySyncService`
- `com.google.android.wearable.standalone=false`

### 5. Compatibilità funzionale

Prima della pubblicazione dobbiamo testare almeno:

1. avvio dell'app su watch
2. quick entry completa online
3. quick entry offline con coda
4. sync al ritorno online
5. badge queue che si aggiorna correttamente
6. back navigation e swipe dismiss nativi
7. ritorno alla prima schermata dopo success

### 6. Esperienza non-standalone verificata

Dato che l'app Wear non è standalone, bisogna testare in modo esplicito:

- cosa vede l'utente quando il telefono non è disponibile
- come si comporta la coda offline
- cosa succede se il telefono non è loggato o non è pronto

Questo punto è importante anche lato review Play, perché il comportamento reale dell'app deve essere coerente con la sua descrizione.

## Asset e contenuti Play Console

Per la pubblicazione serviranno anche i materiali di store listing.

### Minimo consigliato

- nome app Wear coerente con Dunio
- short description
- full description
- icona app finale
- screenshot reali Wear OS
- eventuale feature graphic, se richiesta dal flusso usato in Play Console

### Screenshot consigliati

Preparare screenshot almeno di:

1. schermata iniziale `Expense / Income`
2. schermata `Amount`
3. schermata `Category`
4. schermata `Confirm`
5. schermata `Success`

Meglio usare screenshot veri di device o emulatori Wear, non mockup generici.

## Contenuti policy e schede Play

Oltre all'upload tecnico, su Google Play vanno completate anche le sezioni di conformità.

### Data safety

Da compilare in modo coerente con il comportamento reale dell'app.

Visto che la Wear app:

- invia dati al telefono
- il telefono salva poi dati finanziari su backend Firebase

la scheda deve riflettere correttamente:

- dati raccolti
- dati condivisi
- finalità del trattamento

### Content rating

Compilare il questionario standard.

### App access

Se l'app richiede login o accesso condizionato, va dichiarato.

Nel nostro caso la quick entry Wear è legata a:

- utente autenticato
- household valida
- app phone disponibile e collegata

Quindi conviene preparare una descrizione chiara di questo requisito.

### Privacy policy

Se non già disponibile e collegata all'app principale, va verificato che la policy copra anche il comportamento della parte Wear.

## Strategia di rilascio consigliata

Per questa prima pubblicazione consiglio questa sequenza.

### 1. Internal testing

Usare l'`internal testing` come primo step.

Obiettivi:

- validare installazione da Play
- verificare pairing reale phone/watch
- validare sync queue fuori dall'ambiente di sviluppo

### 2. Closed testing

Passare poi a un gruppo piccolo di tester.

Obiettivi:

- verificare device Wear diversi
- verificare theme dinamico
- verificare differenze tra emulatori e dispositivi reali

### 3. Production rollout graduale

Se tutto è stabile:

- rilascio produzione graduale

Meglio evitare un rollout immediato al 100% per la prima release Wear.

## Procedura operativa in Play Console

### 1. Preparare la release

Nel progetto:

```bash
./gradlew :wear:bundleRelease
```

Recuperare il file `.aab` generato.

### 2. Aprire Play Console

Entrare nell'app corretta in Play Console.

Se questa è la prima release Wear, verificare con attenzione di essere nella scheda/app coerente con il package usato dal modulo.

### 3. Creare una release

Usare idealmente:

- Internal testing

Caricare il bundle Wear.

### 4. Controllare compatibilità dispositivi

Dopo l'upload, controllare:

- dispositivi supportati
- filtro Wear corretto
- eventuali warning di compatibilità

### 5. Compilare tutte le sezioni richieste

Completare:

- App content
- Data safety
- Store listing
- Content rating
- Privacy policy

### 6. Review finale release

Prima di inviare la release:

- rileggere note release
- verificare `versionCode`
- controllare che non siano rimasti riferimenti debug o funzionalità incomplete

## Checklist Dunio prima del rilascio

### Build e firma

- [ ] `:wear:bundleRelease` eseguito con successo
- [ ] firma release configurata correttamente
- [ ] firma coerente con l'app phone se stesso package
- [ ] `versionCode` incrementato
- [ ] `versionName` aggiornato

### Configurazione app

- [ ] `applicationId` confermato come scelta definitiva
- [ ] manifest Wear corretto
- [ ] `standalone=false` confermato come comportamento desiderato
- [ ] icona finale verificata

### Qualità prodotto

- [ ] quick entry online testata
- [ ] quick entry offline testata
- [ ] sync queue testata
- [ ] badge queue testato
- [ ] navigation back/swipe testata
- [ ] schermata success testata
- [ ] theme dinamico testato su almeno un device/emulatore supportato

### Store e policy

- [ ] descrizione store pronta
- [ ] screenshot Wear pronti
- [ ] data safety compilata
- [ ] privacy policy verificata
- [ ] content rating completato
- [ ] app access verificato

## Rischi pratici da evitare

### 1. Package o signing decisi troppo tardi

È il rischio più importante.

Se pubblichiamo con l'attuale `applicationId`, poi cambiare strategia diventa costoso.

### 2. Descrizione store non allineata al comportamento reale

Dato che la Wear app è non-standalone, la descrizione deve chiarire bene la dipendenza dal telefono.

### 3. Test fatti solo su emulatore

Per questa feature è importante validare anche su device reali, soprattutto:

- reconnect
- sync queue
- behavior del Data Layer

### 4. VersionCode non coordinato

Serve disciplina chiara sui rilasci Wear.

## Comandi utili

### Build release Wear

```bash
./gradlew :wear:assembleRelease
./gradlew :wear:bundleRelease
```

### Verifica compilazione Kotlin

```bash
./gradlew :wear:compileDebugKotlin
./gradlew :app:compileDebugKotlin :wear:compileDebugKotlin
```

## Struttura modulo Wear rilevante per il rilascio

File principali da tenere presenti:

- modulo Gradle:
  - `wear/build.gradle.kts`
- manifest:
  - `wear/src/main/AndroidManifest.xml`
- entrypoint activity:
  - `wear/src/main/java/com/davideagostini/summ/wearapp/presentation/WearQuickEntryActivity.kt`
- sync service:
  - `wear/src/main/java/com/davideagostini/summ/wearapp/sync/WearQuickEntrySyncService.kt`

## Riferimenti ufficiali

Prima del rilascio finale conviene rileggere la documentazione ufficiale Google:

- [Create and publish Wear OS apps](https://developer.android.com/training/wearables/packaging)
- [Wear OS quality guidelines](https://developer.android.com/docs/quality-guidelines/wear-app-quality)
- [Prepare your app for release](https://developer.android.com/studio/publish/preparing)
- [Google Play Console Help](https://support.google.com/googleplay/android-developer/)

## Nota finale

Per Dunio la pubblicazione Wear non va trattata come un semplice “upload di un modulo Android”.

Le decisioni davvero importanti da confermare prima della produzione sono:

1. package/signing strategy
2. posizione della Wear app rispetto all'app phone
3. comunicazione chiara del fatto che la versione attuale è non-standalone

Se questi tre punti sono chiari, il resto del processo Play diventa molto più lineare.
