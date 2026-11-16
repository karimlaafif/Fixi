# Fixi — Guide d'intégration backend

> **Pour qui ?** L'équipe qui branche la base MySQL sur l'interface JavaFX.
> **Temps de lecture :** 10 minutes. Lisez au moins les sections 1 à 4 avant de coder.

---

## 1. En bref

- **L'interface est terminée et fonctionne déjà**, avec des **données fictives en mémoire** (rien n'est enregistré).
- Les écrans ne parlent **jamais** à la base directement. Ils appellent **6 interfaces Java** (`AuthService`, `TradeService`…).
- Aujourd'hui, ces interfaces sont implémentées par des classes `Mock…`. **Votre travail : écrire les classes `Jdbc…` équivalentes**, puis changer **une ligne par service** dans `ServiceConfig.java`.
- **Vous n'avez pas besoin de toucher aux écrans** (FXML, contrôleurs, CSS).

```
 Écran (FXML)  →  Contrôleur  →  Services.trades()  →  TradeService (interface)
                                                          ├── MockTradeService   ← aujourd'hui
                                                          └── JdbcTradeService   ← à écrire (MySQL)
```

---

## 2. Lancer l'application

```bash
cd fixi
mvn javafx:run        # lance l'application
mvn test              # lance les 25 tests
```

Comptes de démonstration (tant que `MockAuthService` est utilisé, l'écran de connexion les propose en un clic) :

| Rôle | E-mail | Mot de passe |
|---|---|---|
| Client | `client@fixi.ma` | `fixi1234` |
| Artisan | `artisan@fixi.ma` | `fixi1234` |
| Administrateur | `admin@fixi.ma` | `fixi1234` |

**Captures de tous les écrans d'un coup** (utile pour un rapport) :

```bash
FIXI_SCREENSHOTS=/chemin/vers/dossier mvn javafx:run
```

---

## 3. Où se trouve quoi

```
src/main/java/ma/fixi/
├── App.java                     Point d'entrée
├── config/ServiceConfig.java    ★ LE fichier où l'on choisit Mock ou Jdbc
├── service/                     ★ Les 6 interfaces = le contrat à respecter
│   ├── AuthService.java
│   ├── TradeService.java
│   ├── CityService.java
│   ├── ArtisanService.java
│   ├── RequestService.java
│   ├── MessageService.java
│   ├── ServiceException.java    Erreur à lever (message affiché à l'utilisateur)
│   ├── Services.java            Accès aux services depuis les contrôleurs
│   └── mock/                    Implémentations fictives (modèles à imiter, puis à supprimer)
├── model/                       Entités (User, Trade, City, Artisan, ServiceRequest…)
├── dto/                         Données des formulaires (RegistrationForm, NewRequestForm…)
├── util/
│   ├── DBConnection.java        Connexion MySQL (lit config.properties)
│   ├── Validators.java          Règles de validation (réutilisables côté service)
│   └── Async.java               Exécute les appels hors du fil JavaFX (déjà géré)
├── controller/                  Contrôleurs des écrans (ne pas modifier)
└── view/                        Navigation et composants visuels (ne pas modifier)

src/main/resources/
├── view/*.fxml                  Écrans
├── css/fixi.css                 Style
├── db/schema.sql                Tables existantes
└── db/seed.sql                  Jeu d'essai (métiers, villes, comptes de démo)
```

**Vous créez vos classes dans un nouveau dossier :** `src/main/java/ma/fixi/service/jdbc/`.

---

## 4. Les 7 règles du contrat

Respectez-les et l'interface fonctionnera sans modification.

1. **Implémentez l'interface telle quelle.** Ne changez pas les signatures. Si une méthode vous manque, discutez-en avec l'équipe interface.
2. **En cas d'erreur, levez `ServiceException` avec un message en français, compréhensible par un utilisateur.**
   - ✅ `throw new ServiceException("Cet e-mail est déjà utilisé.");`
   - ❌ laisser remonter une `SQLException` : l'utilisateur verrait « Une erreur inattendue… »
   - Pour une erreur technique : `throw new ServiceException("Impossible de charger les métiers.", e);` (gardez `e` pour le débogage).
3. **Toujours `PreparedStatement`** avec des `?` : jamais de concaténation de valeurs dans le SQL (injection SQL).
4. **Toujours `try-with-resources`** pour fermer `Connection`, `PreparedStatement` et `ResultSet`.
5. **Les id sont des UUID en texte** (`CHAR(36)`) : `UUID.randomUUID().toString()`.
6. **Mots de passe : BCrypt uniquement** (`org.mindrot.jbcrypt.BCrypt`, déjà dans le `pom.xml`).
   - Inscription : `BCrypt.hashpw(motDePasse, BCrypt.gensalt(10))`
   - Connexion : `BCrypt.checkpw(motDePasse, hashEnBase)`
7. **Ne vous occupez pas des threads.** Les contrôleurs appellent déjà vos services en arrière-plan (`Async.run`). Vos méthodes peuvent être lentes sans figer l'écran. N'utilisez **pas** `Platform.runLater` dans un service.

---

## 5. Exemple complet : `JdbcTradeService` (à copier)

C'est la fonctionnalité CRUD demandée pour l'itération 1. Ce code a été **compilé et testé** contre le `schema.sql` actuel.

Fichier : `src/main/java/ma/fixi/service/jdbc/JdbcTradeService.java`

```java
package ma.fixi.service.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import ma.fixi.model.Trade;
import ma.fixi.service.ServiceException;
import ma.fixi.service.TradeService;
import ma.fixi.util.DBConnection;

/** Métiers stockés dans la table MySQL {@code trade}. */
public class JdbcTradeService implements TradeService {

    private static final String SELECT = "SELECT id, name_fr, name_ar, is_active FROM trade ";

    @Override
    public List<Trade> findAll() {
        return query(SELECT + "ORDER BY name_fr");
    }

    @Override
    public List<Trade> findActive() {
        return query(SELECT + "WHERE is_active = TRUE ORDER BY name_fr");
    }

    @Override
    public Trade create(Trade trade) {
        ensureUniqueName(trade);
        String id = (trade.getId() == null || trade.getId().isBlank()) ? UUID.randomUUID().toString() : trade.getId();
        String sql = "INSERT INTO trade (id, name_fr, name_ar, is_active) VALUES (?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, trade.getNameFr().trim());
            ps.setString(3, trade.getNameAr().trim());
            ps.setBoolean(4, trade.isActive());
            ps.executeUpdate();
            return new Trade(id, trade.getNameFr().trim(), trade.getNameAr().trim(), trade.isActive());
        } catch (SQLException e) {
            throw new ServiceException("Impossible d'enregistrer le métier.", e);
        }
    }

    @Override
    public Trade update(Trade trade) {
        ensureUniqueName(trade);
        String sql = "UPDATE trade SET name_fr = ?, name_ar = ?, is_active = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, trade.getNameFr().trim());
            ps.setString(2, trade.getNameAr().trim());
            ps.setBoolean(3, trade.isActive());
            ps.setString(4, trade.getId());
            if (ps.executeUpdate() == 0) {
                throw new ServiceException("Ce métier n'existe plus. Actualisez la liste.");
            }
            return trade;
        } catch (SQLException e) {
            throw new ServiceException("Impossible de modifier le métier.", e);
        }
    }

    @Override
    public void delete(String tradeId) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM trade WHERE id = ?")) {
            ps.setString(1, tradeId);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            // Une clé étrangère (artisan_trade, service_request…) référence encore ce métier.
            throw new ServiceException("Ce métier est utilisé par des artisans ou des demandes. Désactivez-le plutôt.", e);
        } catch (SQLException e) {
            throw new ServiceException("Impossible de supprimer le métier.", e);
        }
    }

    private void ensureUniqueName(Trade trade) {
        String sql = "SELECT COUNT(*) FROM trade WHERE LOWER(name_fr) = LOWER(?) AND id <> ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, trade.getNameFr().trim());
            ps.setString(2, trade.getId() == null ? "" : trade.getId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    throw new ServiceException("Un métier porte déjà ce nom.");
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Impossible de vérifier le nom du métier.", e);
        }
    }

    private List<Trade> query(String sql) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Trade> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Trade(rs.getString("id"), rs.getString("name_fr"),
                        rs.getString("name_ar"), rs.getBoolean("is_active")));
            }
            return list;
        } catch (SQLException e) {
            throw new ServiceException("Impossible de charger les métiers. Vérifiez la connexion à la base.", e);
        }
    }
}
```

**Puis, dans `ServiceConfig.java`, changez une seule ligne :**

```java
new MockTradeService(store),     // avant
new JdbcTradeService(),          // après  (+ import ma.fixi.service.jdbc.JdbcTradeService)
```

Relancez `mvn javafx:run`, connectez-vous en administrateur : l'écran **Métiers** lit et écrit maintenant dans MySQL. Ajoutez un métier, fermez l'application, rouvrez-la : il est toujours là.

> **Important — mélange Mock / Jdbc.** Pendant la transition, certains services seront fictifs et d'autres réels. Les données fictives (artisans, demandes) utilisent leurs propres métiers en mémoire : c'est normal si un métier ajouté en base n'apparaît pas encore sur les profils d'artisans fictifs. Tout redevient cohérent quand tous les services sont branchés.

---

## 6. Préparer la base (une fois par poste)

```bash
# 1. Créer les tables
mysql -u root -p < src/main/resources/db/schema.sql
# 2. Remplir les métiers, villes et comptes de démo
mysql -u root -p < src/main/resources/db/seed.sql
# 3. Configurer la connexion
cp src/main/resources/config.example.properties config.properties
#    puis mettez votre mot de passe MySQL dans config.properties (fichier ignoré par Git)
```

`DBConnection` lit `config.properties` **à la racine du dossier `fixi/`**. Ne versionnez jamais ce fichier.

---

## 7. Liste des tâches

Cochez au fur et à mesure. Travaillez dans l'ordre : chaque étape débloque des écrans.

### Étape 1 — Itération 1 (obligatoire) : tables déjà existantes

#### ☐ 1.1 `JdbcTradeService` — CRUD des métiers
- **Table :** `trade` (existe déjà)
- **Écrans concernés :** Administration › Métiers (CRUD), listes « Métier » de l'accueil, de la recherche et du formulaire de demande
- **Code :** section 5 ci-dessus, prêt à copier
- **Fait quand :** ajouter / modifier / désactiver / supprimer un métier fonctionne et persiste après redémarrage

#### ☐ 1.2 `JdbcCityService` — lecture des villes
- **Table :** `city` (existe déjà)
- **Méthode :** `findActive()` → `SELECT id, name_fr, name_ar, latitude, longitude, is_active FROM city WHERE is_active = TRUE ORDER BY name_fr`
- **Attention :** `latitude` / `longitude` → `rs.getBigDecimal(...)` puis `city.setLatitude(...)`
- **Fait quand :** les listes « Ville » affichent les villes de la base

#### ☐ 1.3 `JdbcAuthService` — connexion, inscription, profil
- **Tables :** `user`, `user_role` (existent déjà)
- **`login(email, password)`**
  1. `SELECT * FROM user WHERE LOWER(email) = LOWER(?)`
  2. Introuvable **ou** `BCrypt.checkpw` faux → `ServiceException("E-mail ou mot de passe incorrect.")` (même message dans les deux cas : on ne révèle pas si l'e-mail existe)
  3. `account_status = 'SUSPENDED'` → `ServiceException("Ce compte est suspendu. Contactez le support Fixi.")`
  4. Charger les rôles : `SELECT role_code FROM user_role WHERE user_id = ?` → `user.setRoles(...)`
- **`register(form)`**
  1. E-mail déjà présent → `ServiceException("Cet e-mail est déjà utilisé.")`
  2. Dans **une transaction** (`c.setAutoCommit(false)` … `c.commit()`) : `INSERT INTO user` (statut `ACTIVE` pour l'itération 1) puis `INSERT INTO user_role`
  3. `client_type` = `form.clientType()` (null pour un artisan)
  4. Renvoyer le `User` créé (l'interface le connecte directement)
- **`updateProfile(user)`** : `UPDATE user SET full_name=?, email=?, phone=?, client_type=? WHERE id=?` ; e-mail pris par un autre compte → `ServiceException("Cet e-mail est déjà utilisé par un autre compte.")`
- **Mapping enum ↔ colonne :** `ClientType.valueOf(rs.getString("client_type"))` (attention au `null`), `AccountStatus.valueOf(...)`, `Role.valueOf(...)`, et `enum.name()` pour écrire
- **Fait quand :** on peut créer un compte, se déconnecter, se reconnecter, modifier son profil

> ✅ **À la fin de l'étape 1, les livrables BD de l'itération 1 sont couverts** (connexion MySQL, tables, insertion / lecture, CRUD complet sur les métiers).

### Étape 2 — Itération 2 : nouvelles tables à créer

Le SQL proposé est en **section 8**. Validez-le en équipe avant de l'ajouter à `schema.sql`.

#### ☐ 2.1 `JdbcArtisanService`
- **Tables :** `artisan_profile`, `artisan_trade`, `artisan_coverage`, `review`, `appointment`
- **`search(criteria)`** — chaque champ `null` = pas de filtre :
  - `text` → `display_name LIKE ?` ou nom du métier `LIKE ?` (avec `%texte%`)
  - `trade` → `artisan_trade.trade_id = ?`
  - `city` → ville de l'artisan **ou** ville dans ses zones couvertes
  - `minRating` → `average_rating >= ?`
  - `availableBy` → voir le calcul de `Availability` ci-dessous
  - `verifiedOnly` → `verification_status = 'VERIFIED'`
  - `sort` → `ORDER BY average_rating DESC` / `indicative_rate_min ASC` / `years_of_experience DESC`
  - Objectif du cahier des charges : **moins de 2 secondes** → index sur `trade_id`, `city_id`
- **`findById(id)`** : profil + `coveredAreas` (noms des villes) + 3 à 5 derniers avis
- **`findTopRated(limit)`** : vérifiés, triés par note, `LIMIT ?`
- **`getStats(artisanId)`** : `COUNT` des demandes en attente (`request_recipient.response_status = 'PENDING'`), des rendez-vous futurs, des interventions terminées
- **`findUpcomingAppointments(artisanId, limit)`** : `starts_at > NOW() ORDER BY starts_at LIMIT ?`
- **`Availability`** (enum de l'interface) se calcule à partir du prochain créneau libre : aujourd'hui → `TODAY`, demain → `TOMORROW`, sous 7 jours → `THIS_WEEK`, sinon `LATER`. Pour commencer, une colonne `next_available_on DATE` dans `artisan_profile` suffit.

#### ☐ 2.2 `JdbcRequestService`
- **Tables :** `service_request`, `request_recipient`, `request_attachment`, `status_event`
- **Règles de statut :** elles sont déjà codées dans `RequestStatus` — **réutilisez-les**, ne les réécrivez pas :
  - `cancel` autorisé seulement si `status.canBeCancelledByClient()`
  - `changePreferredDate` seulement si `status.canChangeDate()` et date ≥ aujourd'hui
  - `confirmCompletion` seulement si `status.canBeValidatedByClient()` (statut `DONE_BY_ARTISAN`)
  - Sinon → `ServiceException` avec un message clair (voir `MockRequestService`)
- **`create(clientId, form)`** dans une transaction :
  1. `INSERT INTO service_request` (statut `SENT`, `reference` lisible type `D-4587`)
  2. Destinataires : `form.targetArtisanId()` s'il est renseigné, sinon tous les artisans du métier et de la ville → `INSERT INTO request_recipient`
  3. Photos : `form.photos()` est une liste de `File` — copiez les fichiers dans un dossier de stockage (pas en base !) et enregistrez le chemin dans `request_attachment`
  4. `INSERT INTO status_event` (historique)
- **`findByClient`** : trier par `created_at DESC` ; remplir `assignedArtisan` et `acceptedQuoteTotal` si un devis est accepté (sinon `null`)
- **`findIncomingForArtisan`** : demandes où `request_recipient.artisan_id = ?` et `response_status = 'PENDING'`
- **`decline`** : `UPDATE request_recipient SET response_status = 'DECLINED'`

#### ☐ 2.3 `JdbcMessageService`
- **Tables :** `conversation`, `message`
- Une conversation = **un client + un artisan** (+ la demande si connue). `openWithArtisan` renvoie l'existante ou la crée.
- `Conversation` est **vue depuis l'utilisateur connecté** : `otherPartyName` = l'artisan si le lecteur est le client, et inversement. Remplir `artisanId` seulement quand l'interlocuteur est un artisan (bouton « Voir le profil »).
- `findMessages(conversationId, readerId)` marque comme lus (`read_at = NOW()`) les messages reçus par `readerId`
- `unreadCount` = messages reçus avec `read_at IS NULL`

### Étape 3 — Finitions

- ☐ Supprimer `service/mock/` et `MockStore` quand plus aucun `Mock…` n'est utilisé (l'encart « Mode démonstration » de la connexion disparaît automatiquement)
- ☐ Écrire les tests JDBC (section 9)
- ☐ Mettre à jour le diagramme de classes avec les classes `Jdbc…`

---

## 8. SQL proposé pour l'étape 2

Aligné sur `Fixi_Diagramme_classes.pdf`, simplifié pour l'itération 2. **À valider en équipe.**

```sql
CREATE TABLE IF NOT EXISTS artisan_profile (
    user_id             CHAR(36)     PRIMARY KEY,
    display_name        VARCHAR(150) NOT NULL,
    description         TEXT         NULL,
    city_id             CHAR(36)     NOT NULL,
    years_of_experience INT          NOT NULL DEFAULT 0,
    verification_status ENUM('PENDING', 'VERIFIED', 'REJECTED', 'SUSPENDED') NOT NULL DEFAULT 'PENDING',
    average_rating      DECIMAL(3,2) NOT NULL DEFAULT 0,
    review_count        INT          NOT NULL DEFAULT 0,
    next_available_on   DATE         NULL,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (city_id) REFERENCES city(id)
);

CREATE TABLE IF NOT EXISTS artisan_trade (
    artisan_id          CHAR(36)      NOT NULL,
    trade_id            CHAR(36)      NOT NULL,
    indicative_rate_min DECIMAL(12,2) NULL,
    indicative_rate_max DECIMAL(12,2) NULL,
    PRIMARY KEY (artisan_id, trade_id),
    FOREIGN KEY (artisan_id) REFERENCES artisan_profile(user_id) ON DELETE CASCADE,
    FOREIGN KEY (trade_id)   REFERENCES trade(id)
);

CREATE TABLE IF NOT EXISTS artisan_coverage (
    artisan_id CHAR(36) NOT NULL,
    city_id    CHAR(36) NOT NULL,
    PRIMARY KEY (artisan_id, city_id),
    FOREIGN KEY (artisan_id) REFERENCES artisan_profile(user_id) ON DELETE CASCADE,
    FOREIGN KEY (city_id)    REFERENCES city(id)
);

CREATE TABLE IF NOT EXISTS service_request (
    id                CHAR(36)      PRIMARY KEY,
    reference         VARCHAR(20)   NOT NULL UNIQUE,
    client_id         CHAR(36)      NOT NULL,
    trade_id          CHAR(36)      NOT NULL,
    city_id           CHAR(36)      NOT NULL,
    title             VARCHAR(150)  NOT NULL,
    description       TEXT          NOT NULL,
    address_line      VARCHAR(255)  NULL,
    urgency           ENUM('NORMAL', 'URGENT') NOT NULL DEFAULT 'NORMAL',
    budget            DECIMAL(12,2) NULL,
    preferred_date    DATE          NOT NULL,
    status            ENUM('SENT', 'QUOTE_RECEIVED', 'QUOTE_ACCEPTED', 'SCHEDULED', 'IN_PROGRESS',
                           'DONE_BY_ARTISAN', 'VALIDATED', 'CLOSED', 'EXPIRED', 'CANCELLED', 'DISPUTED')
                      NOT NULL DEFAULT 'SENT',
    assigned_artisan_id   CHAR(36)      NULL,
    accepted_quote_total  DECIMAL(12,2) NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES user(id),
    FOREIGN KEY (trade_id)  REFERENCES trade(id),
    FOREIGN KEY (city_id)   REFERENCES city(id),
    FOREIGN KEY (assigned_artisan_id) REFERENCES artisan_profile(user_id),
    INDEX idx_request_client (client_id, created_at)
);

CREATE TABLE IF NOT EXISTS request_recipient (
    request_id      CHAR(36) NOT NULL,
    artisan_id      CHAR(36) NOT NULL,
    sent_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen_at         DATETIME NULL,
    response_status ENUM('PENDING', 'QUOTED', 'DECLINED') NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (request_id, artisan_id),
    FOREIGN KEY (request_id) REFERENCES service_request(id) ON DELETE CASCADE,
    FOREIGN KEY (artisan_id) REFERENCES artisan_profile(user_id)
);

CREATE TABLE IF NOT EXISTS request_attachment (
    request_id  CHAR(36)     NOT NULL,
    file_path   VARCHAR(255) NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (request_id, file_path),
    FOREIGN KEY (request_id) REFERENCES service_request(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS status_event (
    id          CHAR(36)    PRIMARY KEY,
    request_id  CHAR(36)    NOT NULL,
    actor_id    CHAR(36)    NULL,
    from_status VARCHAR(30) NULL,
    to_status   VARCHAR(30) NOT NULL,
    occurred_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES service_request(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS review (
    id           CHAR(36) PRIMARY KEY,
    artisan_id   CHAR(36) NOT NULL,
    request_id   CHAR(36) NOT NULL UNIQUE,
    author_id    CHAR(36) NOT NULL,
    rating       TINYINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      TEXT     NULL,
    published_on DATE     NOT NULL,
    FOREIGN KEY (artisan_id) REFERENCES artisan_profile(user_id),
    FOREIGN KEY (request_id) REFERENCES service_request(id),
    FOREIGN KEY (author_id)  REFERENCES user(id)
);

CREATE TABLE IF NOT EXISTS appointment (
    id         CHAR(36)     PRIMARY KEY,
    artisan_id CHAR(36)     NOT NULL,
    request_id CHAR(36)     NOT NULL,
    starts_at  DATETIME     NOT NULL,
    ends_at    DATETIME     NULL,
    FOREIGN KEY (artisan_id) REFERENCES artisan_profile(user_id),
    FOREIGN KEY (request_id) REFERENCES service_request(id)
);

CREATE TABLE IF NOT EXISTS conversation (
    id         CHAR(36) PRIMARY KEY,
    request_id CHAR(36) NULL,
    client_id  CHAR(36) NOT NULL,
    artisan_id CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (client_id, artisan_id),
    FOREIGN KEY (client_id)  REFERENCES user(id),
    FOREIGN KEY (artisan_id) REFERENCES user(id)
);

CREATE TABLE IF NOT EXISTS message (
    id              CHAR(36)     PRIMARY KEY,
    conversation_id CHAR(36)     NOT NULL,
    sender_id       CHAR(36)     NOT NULL,
    body            TEXT         NOT NULL,
    attachment_name VARCHAR(255) NULL,
    sent_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at         DATETIME     NULL,
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id)       REFERENCES user(id),
    INDEX idx_message_conv (conversation_id, sent_at)
);
```

> Les colonnes `assigned_artisan_id` et `accepted_quote_total` sont un raccourci tant que les devis (`quote`, `quote_version`, `quote_line`) ne sont pas implémentés. Elles pourront être remplacées par une jointure sur le devis accepté.

---

## 9. Tests

Des tests « contrat » décrivent le comportement attendu de chaque service. Ils passent avec les classes fictives ; **ils doivent aussi passer avec vos classes `Jdbc…`.**

| Test | Ce qu'il vérifie |
|---|---|
| `service/TradeServiceContractTest` | CRUD des métiers, tri, unicité du nom, refus de suppression d'un métier utilisé |
| `service/AuthAndRequestServiceContractTest` | Connexion, inscription, règles d'annulation et de validation des demandes |
| `util/ValidatorsTest` | E-mail, téléphone marocain, mot de passe, montants |
| `model/RequestStatusTest` | Règles du cycle de vie d'une demande |

**Pour tester `JdbcTradeService` :**
1. Créez une base de test séparée (`fixi_test`) avec `schema.sql` + `seed.sql` — **jamais votre base de travail**.
2. Copiez `TradeServiceContractTest` en `JdbcTradeServiceTest` et remplacez `createService()` par `return new JdbcTradeService();`.
3. Videz / rechargez la base de test avant chaque test (`@BeforeEach`) pour que les tests ne dépendent pas les uns des autres.

Le test `deleteRefusesTradeInUse` ne passera en JDBC que lorsque `artisan_trade` / `service_request` existeront (clé étrangère). D'ici là, marquez-le `@Disabled("en attente de l'étape 2")`.

---

## 10. Correspondance écran → service

| Écran | Rôle | Méthodes appelées |
|---|---|---|
| Connexion | tous | `auth.login` |
| Créer un compte | tous | `auth.register` |
| Accueil | client | `trades.findActive`, `cities.findActive`, `requests.findByClient`, `artisans.findTopRated` |
| Rechercher | client | `trades.findActive`, `cities.findActive`, `artisans.search` |
| Profil de l'artisan | client | `artisans.findById`, `messages.openWithArtisan` |
| Nouvelle demande | client | `trades.findActive`, `cities.findActive`, `requests.create` |
| Mes demandes | client | `requests.findByClient`, `cancel`, `changePreferredDate`, `confirmCompletion` |
| Messages | client, artisan | `messages.findConversations`, `findMessages`, `send` |
| Mon profil | tous | `auth.updateProfile` |
| Tableau de bord | artisan | `artisans.findById`, `getStats`, `findUpcomingAppointments`, `requests.findIncomingForArtisan` |
| Demandes reçues | artisan | `requests.findIncomingForArtisan`, `requests.decline` |
| Métiers | admin | `trades.findAll`, `create`, `update`, `delete` |

Pages volontairement « à venir » (affichent un message, aucune donnée) : **Mon agenda**, **Villes** (admin), **Validation des artisans**. Les boutons « Proposer un devis » et la comparaison des devis sont désactivés en attendant le module Devis.

---

## 11. Problèmes fréquents

| Symptôme | Cause probable | Solution |
|---|---|---|
| « Impossible de charger… Vérifiez la connexion » | MySQL arrêté ou `config.properties` absent | Démarrer MySQL ; vérifier `config.properties` à la racine de `fixi/` |
| `Access denied for user` | Mauvais mot de passe dans `config.properties` | Corriger `db.password` |
| « Une erreur inattendue s'est produite » | Une exception autre que `ServiceException` est remontée | Entourer le code JDBC d'un `try/catch (SQLException e)` et lever `ServiceException` |
| Les accents s'affichent mal | Base créée sans `utf8mb4` | Recréer avec `schema.sql` (il fixe `utf8mb4`) |
| La liste ne se met pas à jour | `findAll()` renvoie une liste mise en cache | Toujours relire la base à chaque appel |
| Écran figé | Appel au service hors de `Async.run` | Ne pas appeler les services depuis un contrôleur sans `Async.run` (déjà fait partout) |

---

## 12. Checklist avant de fusionner (pull request)

- [ ] `mvn test` passe
- [ ] `mvn javafx:run` : l'écran concerné fonctionne avec MySQL
- [ ] Les données persistent après fermeture / réouverture
- [ ] Aucune `SQLException` brute n'atteint l'interface (toujours `ServiceException`)
- [ ] Aucune valeur concaténée dans du SQL (`PreparedStatement` partout)
- [ ] Aucun mot de passe en clair (ni en base, ni dans Git)
- [ ] `ServiceConfig.java` mis à jour pour ce service uniquement
- [ ] Une ligne cochée dans la section 7 de ce document

**Une question sur un écran ou une méthode manquante ?** Contactez l'équipe interface plutôt que de modifier les contrôleurs : on ajoute la méthode à l'interface ensemble.
