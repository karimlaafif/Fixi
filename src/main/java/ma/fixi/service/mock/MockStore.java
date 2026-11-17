package ma.fixi.service.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ma.fixi.model.Appointment;
import ma.fixi.model.Artisan;
import ma.fixi.model.Availability;
import ma.fixi.model.City;
import ma.fixi.model.ClientType;
import ma.fixi.model.Message;
import ma.fixi.model.RequestStatus;
import ma.fixi.model.Review;
import ma.fixi.model.Role;
import ma.fixi.model.ServiceRequest;
import ma.fixi.model.Trade;
import ma.fixi.model.Urgency;
import ma.fixi.model.User;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Données de démonstration en mémoire, partagées par tous les services fictifs.
 * Elles sont recréées à chaque lancement : rien n'est enregistré.
 *
 * <p>À supprimer quand tous les services seront branchés sur MySQL.
 * Les valeurs ci-dessous peuvent servir de jeu d'essai SQL (voir {@code db/seed.sql}).</p>
 */
public class MockStore {

    public static final String DEMO_PASSWORD = "fixi1234";
    public static final String CLIENT_ID = "u-client-sarah";
    public static final String ARTISAN_ID = "u-art-ahmed";
    public static final String ADMIN_ID = "u-admin";

    final Map<String, User> users = new HashMap<>();
    final Map<String, String> passwordHashes = new HashMap<>();
    final List<Trade> trades = new ArrayList<>();
    final List<City> cities = new ArrayList<>();
    final List<Artisan> artisans = new ArrayList<>();
    final List<ServiceRequest> requests = new ArrayList<>();
    /** Demandes reçues par chaque artisan et pas encore traitées. */
    final Map<String, List<ServiceRequest>> incoming = new HashMap<>();
    final List<Appointment> appointments = new ArrayList<>();
    final List<ConversationRecord> conversations = new ArrayList<>();
    final List<Message> messages = new ArrayList<>();
    int nextReference = 4587;

    /** Conversation telle que stockée : un client, un artisan, une demande. */
    record ConversationRecord(String id, String requestId, String clientId, String artisanId) {
    }

    public MockStore() {
        seedTrades();
        seedCities();
        seedUsers();
        seedArtisans();
        seedRequests();
        seedIncoming();
        seedAppointments();
        seedMessages();
    }

    static String newId() {
        return UUID.randomUUID().toString();
    }

    Trade trade(String nameFr) {
        return trades.stream().filter(t -> t.getNameFr().equals(nameFr)).findFirst().orElseThrow();
    }

    City city(String nameFr) {
        return cities.stream().filter(c -> c.getNameFr().equals(nameFr)).findFirst().orElseThrow();
    }

    Artisan artisan(String id) {
        return artisans.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }

    private void seedTrades() {
        String[][] rows = {
                {"Plomberie", "سباكة"}, {"Électricité", "كهرباء"}, {"Peinture", "صباغة"},
                {"Menuiserie", "نجارة"}, {"Informatique", "إعلاميات"}, {"Climatisation", "تكييف"},
                {"Serrurerie", "أقفال"}, {"Bricolage", "إصلاحات منزلية"}};
        for (String[] r : rows) {
            trades.add(new Trade(newId(), r[0], r[1], true));
        }
    }

    private void seedCities() {
        cities.add(new City(newId(), "Agadir", "أكادير", 30.4278, -9.5981));
        cities.add(new City(newId(), "Casablanca", "الدار البيضاء", 33.5731, -7.5898));
        cities.add(new City(newId(), "Fès", "فاس", 34.0181, -5.0078));
        cities.add(new City(newId(), "Marrakech", "مراكش", 31.6295, -7.9811));
        cities.add(new City(newId(), "Rabat", "الرباط", 34.0209, -6.8416));
        cities.add(new City(newId(), "Salé", "سلا", 34.0331, -6.7985));
        cities.add(new City(newId(), "Tanger", "طنجة", 35.7595, -5.8340));
        cities.add(new City(newId(), "Témara", "تمارة", 33.9287, -6.9063));
    }

    private void seedUsers() {
        User sarah = new User(CLIENT_ID, "Sarah Benali", "client@fixi.ma", Role.CLIENT);
        sarah.setClientType(ClientType.PARTICULIER);
        sarah.setPhone("+212612345678");
        addUser(sarah);
        User ahmed = new User(ARTISAN_ID, "Ahmed El Amine", "artisan@fixi.ma", Role.ARTISAN);
        ahmed.setPhone("+212661234567");
        addUser(ahmed);
        addUser(new User(ADMIN_ID, "Équipe Fixi", "admin@fixi.ma", Role.ADMINISTRATOR));
    }

    private void addUser(User user) {
        users.put(user.getId(), user);
        passwordHashes.put(user.getId(), BCrypt.hashpw(DEMO_PASSWORD, BCrypt.gensalt(10)));
    }

    private void seedArtisans() {
        addArtisan(ARTISAN_ID, "Ahmed El Amine", "Plomberie", "Rabat", 4.8, 120, 5, 150, Availability.TODAY, true,
                List.of("Rabat", "Salé", "Témara"),
                "Plombier depuis 5 ans. Fuites, chauffe-eau, sanitaires et installations neuves. "
                        + "Devis gratuit et détaillé avant chaque intervention.");
        addArtisan("u-art-fatima", "Fatima Zahra Alaoui", "Électricité", "Casablanca", 4.7, 98, 4, 200,
                Availability.TOMORROW, true, List.of("Casablanca", "Mohammedia"),
                "Électricienne certifiée : mise aux normes, tableaux électriques, éclairage et dépannage.");
        addArtisan("u-art-youssef", "Youssef Bensli", "Peinture", "Salé", 4.9, 170, 7, 120, Availability.TODAY, true,
                List.of("Salé", "Rabat"),
                "Peinture intérieure et extérieure, enduits décoratifs et ravalement de façades.");
        addArtisan("u-art-omar", "Omar Idrissi", "Informatique", "Rabat", 4.6, 54, 6, 180, Availability.THIS_WEEK,
                true, List.of("Rabat", "Témara"),
                "Réparation d'ordinateurs, installation de réseaux et récupération de données.");
        addArtisan("u-art-hicham", "Hicham Tazi", "Plomberie", "Casablanca", 4.5, 63, 9, 130, Availability.TOMORROW,
                true, List.of("Casablanca"), "Dépannage plomberie 7j/7, débouchage et recherche de fuites.");
        addArtisan("u-art-nadia", "Nadia Chraibi", "Climatisation", "Marrakech", 4.8, 41, 8, 300,
                Availability.THIS_WEEK, true, List.of("Marrakech"),
                "Installation et entretien de climatiseurs et pompes à chaleur, toutes marques.");
        addArtisan("u-art-rachid", "Rachid Ouazzani", "Menuiserie", "Fès", 4.4, 37, 15, 250, Availability.LATER,
                true, List.of("Fès", "Meknès"), "Menuiserie bois sur mesure : portes, placards, cuisines.");
        addArtisan("u-art-said", "Saïd Amrani", "Électricité", "Rabat", 4.2, 19, 3, 150, Availability.TODAY, false,
                List.of("Rabat", "Salé"), "Petits travaux électriques, prises, luminaires et interrupteurs.");
        addArtisan("u-art-imane", "Imane Berrada", "Peinture", "Tanger", 4.6, 28, 4, 110, Availability.THIS_WEEK,
                true, List.of("Tanger"), "Peinture et décoration intérieure, conseils couleurs inclus.");
        addArtisan("u-art-khalid", "Khalid Naciri", "Serrurerie", "Casablanca", 4.3, 45, 11, 200, Availability.TODAY,
                true, List.of("Casablanca"), "Ouverture de portes, changement de serrures et blindage.");
        addArtisan("u-art-mehdi", "Mehdi Fassi", "Bricolage", "Témara", 4.1, 12, 2, 100, Availability.TOMORROW,
                false, List.of("Témara", "Rabat"), "Montage de meubles, fixations murales et petites réparations.");
        addArtisan("u-art-amina", "Amina Kettani", "Climatisation", "Agadir", 4.7, 33, 6, 280, Availability.LATER,
                true, List.of("Agadir"), "Climatisation résidentielle : installation, recharge de gaz, entretien.");
    }

    private void addArtisan(String id, String name, String trade, String city, double rating, int reviews,
                            int years, int priceFrom, Availability availability, boolean verified,
                            List<String> areas, String description) {
        Artisan a = new Artisan();
        a.setId(id);
        a.setDisplayName(name);
        a.setTrade(trade(trade));
        a.setCity(city(city));
        a.setAverageRating(rating);
        a.setReviewCount(reviews);
        a.setYearsOfExperience(years);
        a.setPriceFrom(BigDecimal.valueOf(priceFrom));
        a.setAvailability(availability);
        a.setVerified(verified);
        a.setCoveredAreas(areas);
        a.setDescription(description);
        LocalDate today = LocalDate.now();
        a.setReviews(List.of(
                new Review("Karim L.", 5, "Ponctuel, travail propre et prix conforme au devis.", today.minusDays(6)),
                new Review("Houda B.", 5, "Très professionnel, il a expliqué chaque étape.", today.minusDays(19)),
                new Review("Mounir A.", 4, "Bon travail. Arrivé avec 30 minutes de retard mais a prévenu.",
                        today.minusDays(41))));
        artisans.add(a);
    }

    private void seedRequests() {
        LocalDate today = LocalDate.now();
        addRequest("Réparation ordinateur", "Informatique", "Rabat",
                "L'ordinateur portable ne s'allume plus depuis hier, le voyant de charge reste éteint.",
                500, today.plusDays(1), RequestStatus.IN_PROGRESS, "u-art-omar", 450, today.minusDays(3));
        addRequest("Fuite d'eau sous l'évier", "Plomberie", "Salé",
                "Une fuite au niveau du siphon de l'évier de la cuisine, l'eau coule en continu.",
                400, today.minusDays(1), RequestStatus.DONE_BY_ARTISAN, ARTISAN_ID, 350, today.minusDays(6));
        addRequest("Peinture salon", "Peinture", "Rabat",
                "Repeindre un salon de 25 m², murs et plafond, couleur blanc cassé.",
                2500, today.plusDays(5), RequestStatus.SCHEDULED, "u-art-youssef", 2400, today.minusDays(9));
        ServiceRequest clim = addRequest("Installation climatiseur", "Climatisation", "Témara",
                "Installer un climatiseur split 12 000 BTU dans une chambre, unité extérieure sur balcon.",
                0, today.plusDays(10), RequestStatus.QUOTE_RECEIVED, null, 0, today.minusDays(2));
        clim.setQuoteCount(3);
        addRequest("Remplacement de serrure", "Serrurerie", "Rabat",
                "Changer le cylindre de la porte d'entrée après la perte des clés.",
                300, today.plusDays(2), RequestStatus.SENT, null, 0, today);
        addRequest("Panne électrique", "Électricité", "Rabat",
                "Le disjoncteur saute dès que le four est allumé.",
                0, today.minusDays(12), RequestStatus.CANCELLED, null, 0, today.minusDays(14));
        addRequest("Fixation étagère", "Bricolage", "Rabat",
                "Fixer trois étagères murales dans le bureau, mur en béton.",
                200, today.minusDays(20), RequestStatus.CLOSED, "u-art-mehdi", 180, today.minusDays(25));
    }

    private ServiceRequest addRequest(String title, String trade, String city, String description, int budget,
                                      LocalDate preferredDate, RequestStatus status, String artisanId,
                                      int quoteTotal, LocalDate createdOn) {
        ServiceRequest r = new ServiceRequest();
        r.setId(newId());
        r.setReference("D-" + nextReference++);
        r.setClientId(CLIENT_ID);
        r.setClientName("Sarah Benali");
        r.setTitle(title);
        r.setTrade(trade(trade));
        r.setCity(city(city));
        r.setDescription(description);
        r.setUrgency(Urgency.NORMAL);
        r.setBudget(budget > 0 ? BigDecimal.valueOf(budget) : null);
        r.setPreferredDate(preferredDate);
        r.setStatus(status);
        r.setCreatedAt(createdOn.atTime(10, 30));
        if (artisanId != null) {
            r.setAssignedArtisan(artisan(artisanId));
            r.setAcceptedQuoteTotal(BigDecimal.valueOf(quoteTotal));
            r.setQuoteCount(1);
        }
        requests.add(r);
        return r;
    }

    private void seedIncoming() {
        LocalDate today = LocalDate.now();
        List<ServiceRequest> list = new ArrayList<>();
        list.add(incomingRequest("Chauffe-eau qui ne chauffe plus", "Rabat", "Leila Mansouri",
                "Chauffe-eau électrique de 80 L, l'eau reste froide depuis ce matin.", 600, today.plusDays(1),
                Urgency.URGENT, today));
        list.add(incomingRequest("Fuite de la chasse d'eau", "Salé", "Youssef Kadiri",
                "La chasse d'eau coule en permanence dans la cuvette.", 250, today.plusDays(3),
                Urgency.NORMAL, today.minusDays(1)));
        list.add(incomingRequest("Installation d'un lavabo", "Témara", "Syndic Résidence Al Andalous",
                "Pose d'un lavabo neuf avec mitigeur dans les sanitaires du rez-de-chaussée.", 900,
                today.plusDays(6), Urgency.NORMAL, today.minusDays(1)));
        incoming.put(ARTISAN_ID, list);
    }

    private ServiceRequest incomingRequest(String title, String city, String clientName, String description,
                                           int budget, LocalDate preferredDate, Urgency urgency, LocalDate createdOn) {
        ServiceRequest r = new ServiceRequest();
        r.setId(newId());
        r.setReference("D-" + nextReference++);
        r.setClientId(newId());
        r.setClientName(clientName);
        r.setTitle(title);
        r.setTrade(trade("Plomberie"));
        r.setCity(city(city));
        r.setDescription(description);
        r.setBudget(BigDecimal.valueOf(budget));
        r.setPreferredDate(preferredDate);
        r.setUrgency(urgency);
        r.setStatus(RequestStatus.SENT);
        r.setCreatedAt(createdOn.atTime(9, 15));
        return r;
    }

    private void seedAppointments() {
        LocalDate today = LocalDate.now();
        appointments.add(new Appointment(newId(), "Réparation fuite d'évier", "Sarah Benali", city("Salé"),
                today.plusDays(1).atTime(10, 0)));
        appointments.add(new Appointment(newId(), "Remplacement chauffe-eau", "Nabil Haddad", city("Rabat"),
                today.plusDays(2).atTime(14, 0)));
        appointments.add(new Appointment(newId(), "Débouchage canalisation", "Résidence Les Orangers",
                city("Témara"), today.plusDays(4).atTime(9, 30)));
    }

    private void seedMessages() {
        ServiceRequest leak = requests.get(1);
        ConversationRecord withAhmed = new ConversationRecord(newId(), leak.getId(), CLIENT_ID, ARTISAN_ID);
        ConversationRecord withYoussef = new ConversationRecord(newId(), requests.get(2).getId(), CLIENT_ID,
                "u-art-youssef");
        ConversationRecord withOmar = new ConversationRecord(newId(), requests.get(0).getId(), CLIENT_ID,
                "u-art-omar");
        conversations.add(withAhmed);
        conversations.add(withYoussef);
        conversations.add(withOmar);

        LocalDateTime today = LocalDate.now().atStartOfDay();
        message(withOmar, "u-art-omar", "Bonjour, j'ai récupéré l'ordinateur. Je vous tiens au courant.",
                today.minusDays(2).with(LocalTime.of(16, 5)), true);
        message(withOmar, CLIENT_ID, "Merci beaucoup !", today.minusDays(2).with(LocalTime.of(16, 20)), true);

        message(withYoussef, "u-art-youssef", "Bonjour Sarah, je passerai prendre les mesures lundi matin.",
                today.minusDays(1).with(LocalTime.of(11, 2)), true);
        message(withYoussef, CLIENT_ID, "Parfait, je serai là à partir de 9h.",
                today.minusDays(1).with(LocalTime.of(11, 40)), true);
        message(withYoussef, "u-art-youssef", "Le devis a été mis à jour avec la peinture du plafond.",
                today.minusDays(1).with(LocalTime.of(18, 12)), true);

        message(withAhmed, ARTISAN_ID, "Bonjour Sarah, j'ai bien reçu votre demande pour la fuite sous l'évier.",
                today.with(LocalTime.of(9, 40)), true);
        message(withAhmed, CLIENT_ID, "Bonjour ! Pouvez-vous m'envoyer le devis ?", today.with(LocalTime.of(9, 52)),
                true);
        Message quote = message(withAhmed, ARTISAN_ID, "Voici le devis détaillé.", today.with(LocalTime.of(10, 5)),
                true);
        quote.setAttachmentName("Devis_plomberie_D-4588.pdf");
        message(withAhmed, CLIENT_ID, "Parfait, je l'accepte. On maintient le rendez-vous demain à 10h ?",
                today.with(LocalTime.of(10, 20)), true);
        message(withAhmed, ARTISAN_ID, "Oui, c'est confirmé. À demain !", today.with(LocalTime.of(10, 24)), false);
    }

    private Message message(ConversationRecord c, String senderId, String body, LocalDateTime at, boolean read) {
        Message m = new Message(newId(), c.id(), senderId, body, at);
        if (read) {
            m.setReadAt(at.plusMinutes(1));
        }
        messages.add(m);
        return m;
    }
}
