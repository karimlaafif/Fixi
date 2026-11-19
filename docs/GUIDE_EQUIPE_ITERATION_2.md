# Fixi — Guide de Répartition du Travail Back-End

         

## 1. Vue d'ensemble & Organisation du Groupe

Le professeur exige un **travail d'équipe collaboratif, traçable sur GitHub** et respectant strictement l'architecture **MVC**.
L'interface graphique JavaFX est déjà prête et fonctionnelle. Le travail de l'équipe consiste à brancher la persistance réelle en base de données sans altérer les interfaces graphiques, en remplaçant les classes `Mock...` par des classes `Jdbc...`.

### Matrice des Responsabilités de l'Équipe

| Membre | Rôle Attribué | Branche Git Dédiée | Modules & Tâches Attribués | Livrables Attendus |
|---|---|---|---|---|

| **Collègue 1 (Backend Dev A)** | Responsable Métier Demandes | `feature/request-service` | Gestion des demandes de travaux (`service_request`, `request_recipient`, `status_event`) | `JdbcRequestService.java`, tests cycle de vie des demandes |
| **Collègue 2 (Backend Dev B)** | Responsable Métier Artisans | `feature/artisan-service` | Profils artisans, recherche multi-critères, statistiques (`artisan_profile`, `artisan_trade`, `review`) | `JdbcArtisanService.java`, requêtes de recherche optimisées |
| **Collègue 3 (QA & Intégration)** | Responsable Tests & Messagerie | `feature/tests-messaging` | Tests unitaires JUnit 5 (Tâche 5 Prof), Messagerie interne (`conversation`, `message`) | `JdbcTradeServiceTest`, `JdbcAuthServiceTest`, `JdbcMessageService.java`, rapport de tests |

---

## 2. Guide Détaillé par Membre

---

### 👤 FICHE DE MISSION : Collègue 1 — Module Demandes de Service

* **Branche Git :** `feature/request-service`
* **Interface à implémenter :** `ma.fixi.service.RequestService`
* **Fichier à créer :** `src/main/java/ma/fixi/service/jdbc/JdbcRequestService.java`
* **Tables concernées :** `service_request`, `request_recipient`, `request_attachment`, `status_event` (voir `src/main/resources/db/schema.sql` section 8 de `docs/INTEGRATION_BACKEND.md`).

#### Tâches concrètes à réaliser :
1. **`create(String clientId, NewRequestForm form)`** :
   - Insérer la demande dans `service_request` avec statut initial `'SENT'` et génération d'une référence lisible (ex: `"D-" + (int)(Math.random() * 9000 + 1000)`).
   - Si `form.targetArtisanId()` est renseigné, insérer une ligne dans `request_recipient` pour cet artisan ; sinon, insérer pour tous les artisans du métier dans la ville.
   - Enregistrer l'événement initial dans `status_event` (`from_status=null`, `to_status='SENT'`).
   - Tout exécuter au sein d'une transaction SQL (`c.setAutoCommit(false)` ... `c.commit()`).
2. **`findByClient(String clientId)`** :
   - Charger toutes les demandes du client triées par `created_at DESC`.
   - Remplir `assignedArtisan` et `acceptedQuoteTotal` si existants.
3. **`findIncomingForArtisan(String artisanId)`** :
   - Récupérer les demandes associées à l'artisan ayant `response_status = 'PENDING'`.
4. **Gestion des transitions d'état :**
   - `cancel(requestId)` : vérifier `status.canBeCancelledByClient()`. Si faux, lever `ServiceException("Cette demande ne peut plus être annulée.")`.
   - `changePreferredDate(requestId, newDate)` : vérifier `status.canChangeDate()`.
   - `confirmCompletion(requestId)` : vérifier `status.canBeValidatedByClient()`.
   - `decline(artisanId, requestId)` : passer `response_status` à `'DECLINED'` dans `request_recipient`.

#### Critère de validation :
Dans l'IHM, en étant connecté en tant que Client (`client@fixi.ma`), publier une nouvelle demande, vérifier qu'elle apparaît dans « Mes demandes », puis se connecter en Artisan (`artisan@fixi.ma`) pour vérifier sa réception.

---

### 👤 FICHE DE MISSION : Collègue 2 — Module Annuaire & Profils Artisans

* **Branche Git :** `feature/artisan-service`
* **Interface à implémenter :** `ma.fixi.service.ArtisanService`
* **Fichier à créer :** `src/main/java/ma/fixi/service/jdbc/JdbcArtisanService.java`
* **Tables concernées :** `artisan_profile`, `artisan_trade`, `artisan_coverage`, `review`, `appointment`.

#### Tâches concrètes à réaliser :
1. **`search(ArtisanSearchCriteria criteria)`** :
   - Construire une requête SQL dynamique selon les filtres présents :
     - `text` : `WHERE LOWER(display_name) LIKE ?`
     - `trade` : jointure avec `artisan_trade` sur `trade_id = ?`
     - `city` : jointure avec `artisan_coverage` ou ville principale
     - `minRating` : `average_rating >= ?`
     - `sort` : tri par note décroissante, tarif croissant ou expérience
2. **`findById(String artisanId)`** :
   - Charger les données du profil complet, incluant la liste des zones couvertes (`coveredAreas`) et les derniers avis clients (`review`).
3. **`findTopRated(int limit)`** :
   - Récupérer les artisans vérifiés (`verification_status = 'VERIFIED'`), triés par note descendante, limités à `limit`.
4. **`getStats(String artisanId)`** :
   - Compter les demandes en attente, interventions en cours et avis reçus pour alimenter le tableau de bord artisan.
5. **`findUpcomingAppointments(String artisanId, int limit)`** :
   - Récupérer les prochains rendez-vous (`starts_at > NOW() ORDER BY starts_at`).

#### Critère de validation :
Dans l'écran d'accueil Client et dans l'écran « Rechercher un artisan », la saisie d'un métier ou d'une ville filtre dynamiquement les résultats depuis la base de données.

---

### 👤 FICHE DE MISSION : Collègue 3 — Module Tests Unitaires & Messagerie

* **Branche Git :** `feature/tests-messaging`
* **Missions :**
  1. Automatisation des tests JUnit 5 (Tâche 5 du Professeur).
  2. Implémentation du service de messagerie `JdbcMessageService`.
* **Fichiers à créer :**
  - `src/test/java/ma/fixi/service/jdbc/JdbcTradeServiceTest.java`
  - `src/test/java/ma/fixi/service/jdbc/JdbcAuthServiceTest.java`
  - `src/main/java/ma/fixi/service/jdbc/JdbcMessageService.java`
* **Tables concernées :** `conversation`, `message`.

#### Tâches concrètes à réaliser :
1. **Tests Unitaires JUnit 5 (Obligatoire pour la note) :**
   - `JdbcTradeServiceTest` : étendre `TradeServiceContractTest` et injecter `JdbcTradeService`. Vérifier : création d'un métier, tri alphabétique, modification, suppression, et refus des doublons de noms.
   - `JdbcAuthServiceTest` : tester l'authentification avec succès, l'échec avec mauvais mot de passe (vérification que le message d'erreur est masqué et identique), l'unicité de l'e-mail, et le hachage effectif en base (le mot de passe en base commence par `$2a$`).
2. **Service de Messagerie (`JdbcMessageService`) :**
   - `findConversations(userId)` : liste des conversations actives d'un utilisateur.
   - `findMessages(conversationId, readerId)` : liste des messages d'une conversation et marquage automatique des messages non lus (`read_at = NOW()`).
   - `send(conversationId, senderId, body, attachmentName)` : enregistrer le message envoyé.
   - `openWithArtisan(clientId, artisanId)` : retrouver la conversation existante ou la créer si première prise de contact.

#### Critère de validation :
L'exécution de la commande de tests JUnit réussit à 100% sans échec, et deux utilisateurs peuvent échanger des messages via l'onglet « Messages » de l'interface JavaFX.

---

## 3. Workflow Git & GitHub pour toute l'équipe

Pour éviter les conflits et assurer une traçabilité parfaite pour le professeur :

```mermaid
gitGraph
   commit id: "Init Backend"
   branch feature/request-service
   checkout feature/request-service
   commit id: "Implémenter JdbcRequestService"
   checkout backend
   branch feature/artisan-service
   checkout feature/artisan-service
   commit id: "Implémenter JdbcArtisanService"
   checkout backend
   branch feature/tests-messaging
   checkout feature/tests-messaging
   commit id: "Tests JUnit 5 & Messagerie"
   checkout backend
   merge feature/request-service id: "PR #1 Merge Demandes"
   merge feature/artisan-service id: "PR #2 Merge Artisans"
   merge feature/tests-messaging id: "PR #3 Merge Tests & Messagerie"
   commit id: "Validation Finale Itération 2"
```

### Commandes pour chaque collègue :

1. **Cloner le projet et récupérer la branche commune :**
   ```bash
   git clone git@github.com:karimlaafif/Fixi.git
   cd Fixi
   git checkout backend
   git pull origin backend
   ```

2. **Créer sa branche de travail :**
   ```bash
   # Collègue 1 :
   git checkout -b feature/request-service

   # Collègue 2 :
   git checkout -b feature/artisan-service

   # Collègue 3 :
   git checkout -b feature/tests-messaging
   ```

3. **Travailler, tester localement avec les lanceurs :**
   - Double-clic sur `run.bat` (ou `./run.ps1`) pour tester l'application JavaFX.
   - Ne jamais modifier les fichiers FXML ni les contrôleurs.

4. **Commiter régulièrement avec des messages conventionnels :**
   ```bash
   git add src/
   git commit -m "feat(demandes): ajout de la création de demande dans JdbcRequestService"
   ```

5. **Pousser sa branche et ouvrir une Pull Request :**
   ```bash
   git push -u origin feature/votre-nom-de-branche
   ```
   Rendez-vous sur GitHub pour ouvrir une **Pull Request** vers la branche `backend`.

---

## 4. Les 7 Règles d'Or du Contrat Backend

Chaque développeur doit respecter scrupuleusement ces règles pour garantir que son code s'intègre parfaitement avec l'IHM JavaFX :

1. **Ne pas toucher à l'interface graphique :** Les fichiers `.fxml`, les contrôleurs dans `controller/` et le CSS ne doivent pas être modifiés.
2. **Respect des signatures :** Implémenter strictement l'interface Java sans changer ses paramètres ni son type de retour.
3. **Exceptions métier :** Ne jamais laisser remonter une `SQLException` brute. Toujours lever une `ServiceException` avec un message clair en français pour l'utilisateur.
4. **Requêtes paramétrées :** Toujours utiliser `PreparedStatement` avec `?` pour contrer les injections SQL.
5. **Gestion des ressources :** Toujours utiliser `try-with-resources` (`Connection`, `PreparedStatement`, `ResultSet`).
6. **Mots de passe :** Toujours hacher avec `BCrypt.hashpw(pass, BCrypt.gensalt(10))` et vérifier avec `BCrypt.checkpw`.
7. **Thread Safety :** L'UI gère déjà le multithreading via `Async.run`. Ne jamais utiliser `Platform.runLater` dans les classes de service.

---

## 5. Rôle du Chef de Projet (Karim) lors de l'Intégration

En tant que chef de projet :
1. **Revue de code (Code Review) :** Vérifier que chaque PR respecte les 7 règles d'or avant de la merger.
2. **Câblage final dans `ServiceConfig.java` :**
   Activer les services JDBC terminés au fur et à mesure :
   ```java
   Services.register(
       new JdbcAuthService(),
       new JdbcTradeService(),
       new JdbcCityService(),
       new JdbcArtisanService(),    // PR Collègue 2
       new JdbcRequestService(),    // PR Collègue 1
       new JdbcMessageService());   // PR Collègue 3
   ```
3. **Validation finale globale :** Lancer l'application complète, vérifier qu'aucun mock n'est actif, et préparer la démonstration pour l'évaluation.
