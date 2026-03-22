Build an Android app in Kotlin called "Summ" using Jetpack Compose.
The app has a Quick Settings tile that opens a minimal Dialog Activity for fast income/expense entry without opening the main app.

Tech stack:

Kotlin
Jetpack Compose (no XML layouts at all)
Room Database
Material Design 3 (androidx.compose.material3)
ViewModel + StateFlow
Kotlin Coroutines
Hilt for dependency injection


Quick Settings Tile:

Implement a TileService that opens QuickEntryActivity on tap
On Android 13+ request the tile to be added programmatically on first app launch
Tile label: "Quick Entry", use a wallet or ledger icon


QuickEntryActivity:

A standalone ComponentActivity with a transparent/dim background that makes it appear as a floating dialog
Use Dialog composable or set the window to dialog-style programmatically
excludeFromRecents="true" in manifest
Composable UI contains:

Income / Expense toggle using SingleChoiceSegmentedButtonRow
OutlinedTextField for Description
OutlinedTextField for Price (numeric keyboard, decimal allowed)
Category selector: a horizontally scrollable row of FilterChip composables. Categories loaded dynamically from Room so the user can add custom ones from the main app
Cancel and Confirm buttons at the bottom using TextButton and Button


On Confirm: validate all fields (nothing empty, price is a valid number), save entry via ViewModel, dismiss
On Cancel: dismiss without saving
Show a SnackbarHost on successful save before dismissing


Room Database:
Entry entity:
id: Long (autogenerate)
type: String ("income" or "expense")
description: String
price: Double
category: String
date: Long (System.currentTimeMillis())
Category entity:
id: Long (autogenerate)
name: String
emoji: String
Prepopulate default categories on first launch using a RoomDatabase.Callback:
🍕 Food, 🚗 Transport, 🏠 Home, 💊 Health, 🎉 Leisure, 💼 Work, 📦 Other

Main Activity:

Full Compose UI using Scaffold
LazyColumn list of past entries
Each item shows: emoji + category, description, price (green for income, red for expense), formatted date
A total balance card at the top (sum of incomes minus expenses)
A FAB that opens QuickEntryActivity for convenience
Bottom navigation or top bar with a Categories screen where the user can add or delete custom categories using a LazyColumn + AlertDialog for adding new ones


Navigation:

Use androidx.navigation.compose for in-app navigation
Two destinations: Home and Categories


General requirements:

Minimum SDK: API 26 (Android 8)
Target SDK: API 36
No XML layouts anywhere, 100% Compose
All database operations on background threads via Coroutines and Dispatchers.IO
Fully offline, no internet required
MVVM architecture: Composable → ViewModel → Repository → Room
Material Design 3 with dynamic color support (dynamicColorScheme) and dark/light theme
Generate all necessary files: AndroidManifest.xml, Gradle files (libs.versions.toml), all Kotlin files
Use Gradle version catalogs (libs.versions.toml) for all dependencies



And append this at the end to avoid incomplete output:

"Generate the complete project with every single file. Do not abbreviate, do not skip any file, do not use placeholders like 'add the rest here'. Every Composable, ViewModel, Repository, DAO, and Gradle file must be fully implemented."
