# Fixi — Suivi d'avancement de l'intégration Backend (Itération 2)

> **Fichier d'état persistant** : Ce document sert de journal de bord et de point de reprise. En cas de coupure de session ou d'épuisement de tokens, il permet de reprendre exactement là où le travail s'est arrêté sans perte de progression.

---

## 📊 Matrice d'avancement globale

| Étape | Composant | Priorité | Statut | Responsable / Livrable |
|---|---|---|---|---|
| **0** | Configuration & Environnement (DB, properties) | 🔴 Haute (Bloquant) | ✅ TERMINÉ | `config.properties`, `schema_postgres.sql`, `seed_postgres.sql` |
| **1** | CRUD Métiers (`JdbcTradeService`) | 🔴 Haute (Pilote CRUD) | ✅ TERMINÉ | `src/main/java/ma/fixi/service/jdbc/JdbcTradeService.java` |
| **2** | Lecture Villes (`JdbcCityService`) | 🔴 Haute (Simple) | ✅ TERMINÉ | `src/main/java/ma/fixi/service/jdbc/JdbcCityService.java` |
| **3** | Auth & Profil (`JdbcAuthService`) | 🔴 Haute (Sécurité) | ✅ TERMINÉ | `src/main/java/ma/fixi/service/jdbc/JdbcAuthService.java` |
| **4** | Câblage initial (`ServiceConfig.java`) | 🔴 Haute (Intégration) | ✅ TERMINÉ | Services JDBC branchés (Trade, City, Auth) |
| **5** | Scripts d'exécution & compilation | 🔴 Haute (Exécution) | ✅ TERMINÉ | `run.bat`, `run.ps1`, compilation `target/classes` |
| **6** | Tests Unitaires JUnit 5 (Livrable Prof) | 🔴 Haute (Note) | ⏳ EN COURS | `JdbcTradeServiceTest`, `JdbcAuthServiceTest` |
| **7** | Tables Étape 2 (Artisans & Demandes) | 🟡 Moyenne (Métier) | ⏹️ À FAIRE | Extension `schema.sql` (8 tables) |
| **8** | Service Artisans (`JdbcArtisanService`) | 🟡 Moyenne (Métier) | ⏹️ À FAIRE | Recherche, profils, stats |
| **9** | Service Demandes (`JdbcRequestService`) | 🟡 Moyenne (Métier) | ⏹️ À FAIRE | Création, cycle de vie, annulation |
| **10** | Messagerie (`JdbcMessageService`) | 🟢 Basse (Secondaire) | ⏹️ À FAIRE | Conversations, messages |
| **11** | Nettoyage & Finitions | 🟢 Basse (Fin de projet) | ⏹️ À FAIRE | Suppression mocks, rapport |

---

## 📝 Journal des Étapes Détaillées

### [x] Étape 0 : Environnement & Base de données (Tâche 1 & 3 du Prof)
- [x] Créer `config.properties` à la racine configuré pour PostgreSQL (user: `postgres`, password: `2727`, port 5432/2727) et MySQL (XAMPP).
- [x] Ajouter la dépendance `postgresql-42.7.3` dans `pom.xml`.
- [x] Créer `src/main/resources/db/schema_postgres.sql` et `seed_postgres.sql`.
- [x] Créer les scripts de démarrage rapide `run.bat` et `run.ps1`.

### [x] Étape 1 : CRUD Métiers (`JdbcTradeService`) (Tâche 2 & 4 du Prof)
- [x] Créer le package `ma.fixi.service.jdbc`.
- [x] Implémenter `JdbcTradeService.java` (`findAll`, `findActive`, `create`, `update`, `delete`).
- [x] Câbler dans `ServiceConfig.java` : branché sur `JdbcTradeService`.

### [x] Étape 2 : Service Référentiel Villes (`JdbcCityService`) (Tâche 3 du Prof)
- [x] Implémenter `JdbcCityService.java` (`findActive` avec gestion de `BigDecimal` pour lat/long).
- [x] Câbler dans `ServiceConfig.java` : branché sur `JdbcCityService`.

### [x] Étape 3 : Authentification & Gestion Utilisateurs (`JdbcAuthService`) (Tâche 2, 3, 4 du Prof)
- [x] Implémenter `JdbcAuthService.java` :
  - `login(email, password)` : vérification BCrypt, gestion table `"user"`, contrôle des rôles et statuts.
  - `register(form)` : transaction SQL atomique (`user` + `user_role`), hachage BCrypt.
  - `updateProfile(user)` : mise à jour avec contrôle d'unicité email.
- [x] Câbler dans `ServiceConfig.java` : branché sur `JdbcAuthService`.

### [ ] Étape 4 : Tests Unitaires JUnit 5 (Tâche 5 du Prof)
- [ ] Écrire `JdbcTradeServiceTest.java`.
- [ ] Écrire `JdbcAuthServiceTest.java`.
- [ ] Valider l'exécution automatisée des tests.

### [ ] Étape 5 : Domaine Métier Avancé
- [ ] Schéma étendu (Artisans, Demandes, Rendez-vous).
- [ ] `JdbcArtisanService`.
- [ ] `JdbcRequestService`.

### [ ] Étape 6 : Messagerie & Nettoyage (À la fin)
- [ ] `JdbcMessageService`.
- [ ] Nettoyage des mocks.

---

## 🔄 Protocole de reprise en cas de déconnexion / fin de tokens
1. Ouvrir ce fichier `docs/BACKEND_PROGRESS_TRACKER.md`.
2. Trouver la première tâche marquée `[ ]` ou `⏳ EN COURS`.
3. Reprendre la commande ou la classe associée spécifiée dans le plan.
