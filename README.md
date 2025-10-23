# Gestionnaire de dépense – Application Android

## Structure du projet

```
app/
 ├─ src/main/java/com/example/gestionnaire_de_depense/
 │   ├─ auth/        # Authentification : data, domaine, présentation
 │   ├─ budget/      # Fonctionnalités budget (API, repo, ViewModel, modèles)
 │   ├─ di/          # Conteneur d’injection simple (AppContainer)
 │   ├─ ui/          # Écrans Compose (auth, home, thème)
 │   └─ MainActivity.kt
 ├─ src/main/res/    # Ressources Android (layouts, thèmes, etc.)
 └─ src/test/java/   # Tests unitaires (auth & budget)
```

## Prérequis

- Android Studio Giraffe ou version supérieure (AGP conforme au `gradle/libs.versions.toml`).
- JDK compatible (Java 11 minimum comme défini dans `build.gradle.kts`).
- Device ou émulateur Android API 31+ (minSdk 31). Pensez à activer le mode développeur si vous utilisez votre téléphone.

## Installation rapide

1. Cloner le dépôt puis ouvrir le dossier dans Android Studio.
2. Vérifier `local.properties` (SDK path) et approvisionner un émulateur/API physique.
3. Lancer une **Sync Gradle**.

## Commandes Gradle utiles

```bash
./gradlew :app:assembleDebug        # Génère l’APK de debug
./gradlew :app:installDebug         # Installe l’APK sur l’émulateur/appareil connecté
./gradlew test                      # Lancement des tests unitaires JVM
./gradlew connectedAndroidTest      # Tests instrumentés sur device/emulateur
./gradlew lint                      # Analyse statique et lint Compose
```

> **Note** : si `./gradlew test` échoue avec “JAVA_HOME is not set”, configurez votre JDK (variable d’environnement ou paramètre Gradle dans Android Studio).
