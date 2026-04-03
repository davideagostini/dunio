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
- cambio mese non scarica o ricalcola l'intero storico percepibilmente
- shimmer loading appare su cold load senza restare bloccati sulla dashboard precedente
- passaggio `Dashboard -> Entries` non genera ANR o freeze percepibili su device lenti
- uscita da `Entries` verso altre tab non genera freeze o crash
- filtro `All / Expenses / Income`
- search su descrizione/categoria
- grouping per giorno corretto
- unusual spending insight ancora coerente
- report categoria coerente con il solo mese selezionato
- il tab `Reports` risponde senza blocchi evidenti anche con molti movimenti nel mese
- edit entry funziona
- delete entry funziona
- success state e dismiss sheet corretti
- mese chiuso rende la schermata read-only

**6. Assets**

- month picker mostra snapshot corretto
- cambio mese resta fluido anche con storico asset lungo
- shimmer loading appare su cold load senza overlay full-screen del shell
- summary card asset/liabilities/net worth corretta
- search funziona
- add asset funziona
- edit asset funziona
- delete asset funziona
- `Copy previous month` copia solo gli asset mancanti
- mese chiuso rende la schermata read-only
- change percent per asset coerente rispetto al mese precedente
- nessuna regressione nel fullscreen editor

**6b. Dashboard**

- skeleton loading mostra una hero card e due card metriche in verticale
- il get started continua a comparire solo per household globalmente incompleti
- cambio tab rapido `Dashboard / Entries / Assets / Settings` ripristina lo stato senza reset evidenti entro pochi secondi

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
- cambio mese resta fluido anche con storico lungo
- mese chiuso blocca edit su entries
- mese chiuso blocca edit su assets
- riapertura mese, se prevista dal flusso, funziona
- banner read-only coerente col mese selezionato

**9. Quick access**

- Quick Entry flow salva correttamente
- widget quick entry apre il flow
- spending summary widget mostra dati coerenti
- spending summary widget continua ad aggiornarsi dopo insert/update/delete entry
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

====

Ecco una versione finale, pronta da usare per `Google Play`.

**App title**  
`Summ: Shared Finance Tracker`

**Short description**  
`Track shared household finances with net worth, expenses, assets, recurring payments, widgets, and quick entry.`

**Full description**  
`Summ is a shared finance tracker for couples and households to track net worth, expenses, income, assets, liabilities, and recurring payments.

Summ is a mobile-first finance app built for shared households.

Track net worth, income, expenses, assets, liabilities, and recurring transactions in one clean and simple app. Summ is designed for couples, families, or anyone managing money together inside a shared household.

With Summ you can:
• track net worth month by month
• log income and expenses quickly
• manage assets and liabilities
• organize transactions with categories
• automate recurring transactions
• review cash flow, savings rate, and financial runway
• close each month to keep history clean
• use home-screen widgets for quick access
• add transactions faster with the Quick Settings tile

Summ focuses on clarity, speed, and daily usability. It avoids the complexity of traditional accounting software and gives you a lightweight financial dashboard that is easy to maintain over time.

Built for shared finances:
• one household
• multiple members
• shared dashboard
• shared financial data

Whether you want a household finance tracker, a net worth app, or a lightweight money manager for two people, Summ helps you stay organized without unnecessary complexity.`



- shared finance tracker
- household finance
- couple finance
- net worth tracker
- expense tracker
- income tracker
- recurring payments
- assets and liabilities
- money manager
- personal finance

Se vuoi, nel prossimo messaggio posso anche prepararti una versione `più premium e meno SEO`, da confrontare prima di pubblicare.
