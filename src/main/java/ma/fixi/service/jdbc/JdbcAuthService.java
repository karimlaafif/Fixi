package ma.fixi.service.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import ma.fixi.dto.RegistrationForm;
import ma.fixi.model.AccountStatus;
import ma.fixi.model.ClientType;
import ma.fixi.model.Role;
import ma.fixi.model.User;
import ma.fixi.service.AuthService;
import ma.fixi.service.ServiceException;
import ma.fixi.util.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

/** Authentification et gestion de compte basées sur les tables MySQL {@code user} et {@code user_role}. */
public class JdbcAuthService implements AuthService {

    @Override
    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new ServiceException("E-mail ou mot de passe incorrect.");
        }

        String sqlUser = "SELECT id, full_name, client_type, email, phone, password_hash, account_status FROM user WHERE LOWER(email) = LOWER(?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sqlUser)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ServiceException("E-mail ou mot de passe incorrect.");
                }

                String hash = rs.getString("password_hash");
                if (hash == null || !BCrypt.checkpw(password, hash)) {
                    throw new ServiceException("E-mail ou mot de passe incorrect.");
                }

                String statusStr = rs.getString("account_status");
                AccountStatus status = statusStr == null ? AccountStatus.ACTIVE : AccountStatus.valueOf(statusStr);
                if (status == AccountStatus.SUSPENDED) {
                    throw new ServiceException("Ce compte est suspendu. Contactez le support Fixi.");
                }

                User user = new User();
                user.setId(rs.getString("id"));
                user.setFullName(rs.getString("full_name"));
                String ct = rs.getString("client_type");
                if (ct != null && !ct.isBlank()) {
                    user.setClientType(ClientType.valueOf(ct));
                }
                user.setEmail(rs.getString("email"));
                user.setPhone(rs.getString("phone"));
                user.setAccountStatus(status);

                loadRoles(c, user);
                return user;
            }
        } catch (SQLException e) {
            throw new ServiceException("Impossible de se connecter. Vérifiez la connexion à la base.", e);
        }
    }

    @Override
    public User register(RegistrationForm form) {
        if (form.email() == null || form.email().isBlank()) {
            throw new ServiceException("L'adresse e-mail est obligatoire.");
        }
        if (form.password() == null || form.password().isBlank()) {
            throw new ServiceException("Le mot de passe est obligatoire.");
        }

        ensureUniqueEmail(form.email().trim(), null);

        String userId = UUID.randomUUID().toString();
        String hash = BCrypt.hashpw(form.password(), BCrypt.gensalt(10));
        String clientTypeStr = (form.role() == Role.CLIENT && form.clientType() != null) ? form.clientType().name() : null;

        String insertUser = "INSERT INTO user (id, full_name, client_type, email, phone, password_hash, account_status) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')";
        String insertRole = "INSERT INTO user_role (user_id, role_code) VALUES (?, ?)";

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(insertUser)) {
                    ps.setString(1, userId);
                    ps.setString(2, form.fullName().trim());
                    ps.setString(3, clientTypeStr);
                    ps.setString(4, form.email().trim().toLowerCase());
                    ps.setString(5, form.phone());
                    ps.setString(6, hash);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(insertRole)) {
                    ps.setString(1, userId);
                    ps.setString(2, form.role().name());
                    ps.executeUpdate();
                }

                // Si c'est un artisan, créer un profil artisan initial si la table existe
                if (form.role() == Role.ARTISAN) {
                    createInitialArtisanProfile(c, userId, form.fullName().trim());
                }

                c.commit();

                User user = new User();
                user.setId(userId);
                user.setFullName(form.fullName().trim());
                user.setEmail(form.email().trim().toLowerCase());
                user.setPhone(form.phone());
                user.setClientType(form.role() == Role.CLIENT ? form.clientType() : null);
                user.setRoles(EnumSet.of(form.role()));
                user.setAccountStatus(AccountStatus.ACTIVE);
                return user;
            } catch (java.sql.SQLIntegrityConstraintViolationException e) {
                c.rollback();
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("phone")) {
                    throw new ServiceException("Ce numéro de téléphone est déjà utilisé.", e);
                }
                throw new ServiceException("Cet e-mail est déjà utilisé.", e);
            } catch (SQLException e) {
                c.rollback();
                throw new ServiceException("Impossible de créer le compte.", e);
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServiceException("Impossible d'accéder à la base de données.", e);
        }
    }

    @Override
    public User updateProfile(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ServiceException("L'adresse e-mail est obligatoire.");
        }

        ensureUniqueEmail(user.getEmail().trim(), user.getId());

        String sql = "UPDATE user SET full_name = ?, email = ?, phone = ?, client_type = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.getFullName() == null ? "" : user.getFullName().trim());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getClientType() == null ? null : user.getClientType().name());
            ps.setString(5, user.getId());
            if (ps.executeUpdate() == 0) {
                throw new ServiceException("Ce compte n'existe plus.");
            }
            return user;
        } catch (SQLException e) {
            throw new ServiceException("Impossible de mettre à jour le profil.", e);
        }
    }

    private void ensureUniqueEmail(String email, String excludeUserId) {
        String sql = "SELECT COUNT(*) FROM user WHERE LOWER(email) = LOWER(?)"
                + (excludeUserId != null ? " AND id <> ?" : "");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            if (excludeUserId != null) {
                ps.setString(2, excludeUserId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    if (excludeUserId != null) {
                        throw new ServiceException("Cet e-mail est déjà utilisé par un autre compte.");
                    } else {
                        throw new ServiceException("Cet e-mail est déjà utilisé.");
                    }
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Impossible de vérifier l'adresse e-mail.", e);
        }
    }

    private void loadRoles(Connection c, User user) throws SQLException {
        String sqlRoles = "SELECT role_code FROM user_role WHERE user_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sqlRoles)) {
            ps.setString(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                Set<Role> roles = EnumSet.noneOf(Role.class);
                while (rs.next()) {
                    roles.add(Role.valueOf(rs.getString("role_code")));
                }
                user.setRoles(roles);
            }
        }
    }

    private void createInitialArtisanProfile(Connection c, String userId, String displayName) {
        String findCity = "SELECT id FROM city WHERE is_active = TRUE ORDER BY name_fr LIMIT 1";
        String insertProfile = "INSERT INTO artisan_profile (user_id, display_name, city_id, years_of_experience, verification_status, average_rating, review_count) VALUES (?, ?, ?, 0, 'PENDING', 0, 0)";
        try (PreparedStatement psCity = c.prepareStatement(findCity);
             ResultSet rsCity = psCity.executeQuery()) {
            if (rsCity.next()) {
                String cityId = rsCity.getString("id");
                try (PreparedStatement psProfile = c.prepareStatement(insertProfile)) {
                    psProfile.setString(1, userId);
                    psProfile.setString(2, displayName);
                    psProfile.setString(3, cityId);
                    psProfile.executeUpdate();
                }
            }
        } catch (SQLException ignored) {
            // Si la table artisan_profile n'est pas encore utilisée ou contrainte non bloquante
        }
    }
}
