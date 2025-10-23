# Gestionnaire de dépense – Application Android

## Aperçu

Cette application Android (Jetpack Compose) permet à un utilisateur de :

- S’authentifier via un couple e-mail / mot de passe auprès de l’API (`/api/auth/login` et `/api/auth/register`).
- Consulter son tableau de bord budget : solde disponible, expiration du jeton JWT, informations de session.
- Mettre à jour le montant disponible, réaliser des ajustements rapides (revenus/dépenses) et créer de nouvelles entrées budgétaires.
- Parcourir l’historique paginé des mouvements avec différenciation revenus/dépenses.
- Être automatiquement déconnecté lorsque le jeton JWT expire ou qu’un endpoint renvoie HTTP 401.

## Pile technique

- **Langage** : Kotlin (Compose UI, coroutines, kotlinx.serialization).
- **Architecture** : MVVM léger (ViewModels + repositories).
- **Réseau** : Retrofit + OkHttp (intercepteur logging), endpoints configurés pour `http://alass-code.com:8080`.
- **Persistance** : DataStore Preferences pour stocker le JWT et les métadonnées de session.
- **Tests** : JUnit + kotlinx.coroutines-test ; MockWebServer recommandé pour les tests réseau (exemples à étendre).

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
- JDK compatible (Java 11 minimum comme défini dans `build.gradle.kts`). Sur poste local, définissez la variable d’environnement `JAVA_HOME`.
- Device ou émulateur Android API 31+ (minSdk 31).

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

## Configuration API

- Les URLs d’authentification sont définies dans `AuthNetworkModule`.
- Les endpoints budget utilisent `BudgetNetworkModule` avec la base `http://alass-code.com:8080/`.
- Les endpoints protégés exigent un header `Authorization: Bearer <token>` ; la logique est déjà gérée par `AuthRepositoryImpl` et `BudgetRepositoryImpl`.

## Fonctionnalités principales

- **Authentification** : validation locale des champs, gestion des erreurs HTTP (400/401/409) et stockage du JWT.
- **Dashboard budget** :
  - Carte session (email, expiration JWT, compte à rebours).
  - Solde disponible avec rafraîchissement.
  - Formulaire de mise à jour du montant et ajustements rapides (`PUT` / `PATCH`).
  - Historique paginé (`GET` entries) avec scroll infini.
  - Dialogue de création d’entrée (`POST` entries) mettant à jour la liste localement.
- **Gestion des expirations** : les réponses 401 déclenchent un effet `SessionExpired` remonté à `MainActivity` pour revenir sur l’écran d’authentification.

## Tests

- Authentification : `AuthRepositoryImplTest`, `AuthViewModelTest`.
- Budget : `BudgetViewModelTest` (chargement initial, validations, création d’entrée).
- Pour étendre la couverture :
  - Ajouter des tests réseau via `MockWebServer`.
  - Couvrir la pagination (`loadMore`) et les cas d’erreurs côté budget.

## Contributions

- Respectez la convention de commit Conventional Commits (ex. `feat: …`, `fix: …`).
- Ajoutez des captures d’écran pour les changements UI dans vos PRs.
- Conservez les réponses en français lors des revues ou discussions liées au dépôt.

## Feuille de route possible

- Écran de profil / préférences.
- Gestion multi-devises ou budgets multiples.
- Notifications locales d’expiration de session ou seuils de budget.
