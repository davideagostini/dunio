Le `firestore.rules` sono state deployate su `networth-monitor`.

Esito:

- compilazione rules ok
- release ok
- upload saltato perché la versione live era già uguale
- nota: il predeploy ha rigenerato le rules con `testing mode enabled` e `2 allowed email(s)`, quindi restano ancora limitate ai due tester

**Test da fare dopo tutte queste modifiche**

**1. Auth e onboarding**

- login Google con utente A consentito
- login Google con utente B consentito
- session restore dopo chiusura/rilancio app
- utente senza household vede il flow create/join
- utente con household entra direttamente nell’app

**2. Household create/join**

- utente A crea household
- household ID visibile/copiabile
- utente B senza invito prova join con `householdId` e viene bloccato
- utente A crea invito per email B
- utente B fa join con invito valido + `householdId`
- dopo il join, B vede gli stessi dati di A
- l’invito passa a `accepted`

**3. Ruoli e schermata membri**

- owner vede form inviti + lista pending invites
- member non owner non vede form/lista inviti
- member vede la card informativa read-only
- owner può ancora invitare correttamente
- member e owner possono fare CRUD sul resto dei dati household

**4. Dashboard**

- cambio mese dal month picker
- grafico patrimonio su `3 mesi`
- grafico patrimonio su `6 mesi`
- grafico patrimonio su `12 mesi`
- KPI corretti per il mese selezionato
- confronto col mese precedente corretto
- dashboard stabile dopo cambio rapido di mese
- nessun regressione visiva dopo il refactor `renderState`

**5. Entries / transactions**

- month picker mostra i dati del mese corretto
- filtro `All / Expenses / Income`
- search su descrizione/categoria
- grouping per giorno corretto
- unusual spending insight ancora coerente
- edit entry funziona
- delete entry funziona
- success state e dismiss sheet corretti
- mese chiuso rende la schermata read-only

**6. Assets**

- month picker mostra snapshot corretto
- summary card asset/liabilities/net worth corretta
- search funziona
- add asset funziona
- edit asset funziona
- delete asset funziona
- `Copy previous month` copia solo gli asset mancanti
- mese chiuso rende la schermata read-only
- change percent per asset ancora coerente
- nessuna regressione nel fullscreen editor

**7. Recurring**

- lista recurring visibile
- search recurring funziona
- add recurring funziona
- edit recurring funziona
- delete recurring funziona
- `Apply due` crea le transazioni dovute
- rilanciare `Apply due` non crea duplicati
- error state batch ancora mostrato correttamente

**8. Month close**

- chiusura mese consentita
- mese chiuso blocca edit su entries
- mese chiuso blocca edit su assets
- riapertura mese, se prevista dal flusso, funziona
- banner read-only coerente col mese selezionato

**9. Quick access**

- Quick Entry flow salva correttamente
- widget quick entry apre il flow
- spending summary widget mostra dati coerenti
- widget non crasha con utente signed out / senza household

**10. Regressioni tecniche minime**

- `./gradlew :app:compileDebugKotlin`
- test manuale su device/emulatore con owner
- test manuale su secondo device/account con member

Ordine pratico che farei io:

1. create/join/invite
2. members screen owner/member
3. dashboard 3/6/12 mesi
4. entries
5. assets
6. recurring apply due
7. month close
8. widget/quick entry

Se vuoi, il prossimo passo è che trasformo questa lista in una checklist markdown pronta da copiare in una issue o nel release note.
