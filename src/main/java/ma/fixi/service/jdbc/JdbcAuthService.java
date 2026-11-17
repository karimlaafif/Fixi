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

/** Authentification, inscription et gestion des profils via JDBC (tables {@code user} et {@code user_role}). */
public class JdbcAuthService implements AuthService {

    @Override
    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new ServiceException("E-mail ou mot de passe incorrect.");
        }

        try (Connection c = DBConnection.getConnection()) {
            String tbl = userTable(c);
            String sql = "SELECT id, full_name, client_type, email, phone, password_hash, account_status FROM "
                    + tbl + " WHERE LOWER(email) = LOWER(?)";

            try (PreparedStatement ps = c.prepareStatement(sql)) {
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
                    user.setEmail(rs.getString("email"));
                    user.setPhone(rs.getString("phone"));
                    user.setAccountStatus(status);

                    String clientTypeStr = rs.getString("client_type");
                    if (clientTypeStr != null && !clientTypeStr.isBlank()) {
                        user.setClientType(ClientType.valueOf(clientTypeStr));
                    }

                    user.setRoles(loadRoles(c, user.getId()));
                    return user;
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Impossible de se connecter. Vérifiez la connexion à la base.", e);
        }
    }

    @Override
    public User register(RegistrationForm form) {
        if (form.email() == null || form.email().isBlank() || form.password() == null || form.password().isBlank()) {
            throw new ServiceException("L'e-mail et le mot de passe sont obligatoires.");
        }

        try (Connection c = DBConnection.getConnection()) {
            String tbl = userTable(c);
            ensureEmailAvailable(c, tbl, form.email(), null);

            boolean prevAuto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                String userId = UUID.randomUUID().toString();
                String hash = BCrypt.hashpw(form.password(), BCrypt.gensalt(10));

                String insertUser = "INSERT INTO " + tbl
                        + " (id, full_name, client_type, email, phone, password_hash, account_status) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(insertUser)) {
                    ps.setString(1, userId);
                    ps.setString(2, form.fullName().trim());
                    ps.setString(3, form.clientType() == null ? null : form.clientType().name());
                    ps.setString(4, form.email().trim());
                    ps.setString(5, form.phone() == null || form.phone().isBlank() ? null : form.phone().trim());
                    ps.setString(6, hash);
                    ps.setString(7, AccountStatus.ACTIVE.name());
                    ps.executeUpdate();
                }

                String insertRole = "INSERT INTO user_role (user_id, role_code) VALUES (?, ?)";
                try (PreparedStatement ps = c.prepareStatement(insertRole)) {
                    ps.setString(1, userId);
                    ps.setString(2, form.role().name());
                    ps.executeUpdate();
                }

                c.commit();

                User user = new User();
                user.setId(userId);
                user.setFullName(form.fullName().trim());
                user.setEmail(form.email().trim());
                user.setPhone(form.phone() == null || form.phone().isBlank() ? null : form.phone().trim());
                user.setClientType(form.clientType());
                user.setAccountStatus(AccountStatus.ACTIVE);
                user.setRoles(EnumSet.of(form.role()));
                return user;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(prevAuto);
            }
        } catch (SQLException e) {
            throw new ServiceException("Impossible de créer le compte.", e);
        }
    }

    @Override
    public User updateProfile(User user) {
        if (user.getId() == null || user.getId().isBlank()) {
            throw new ServiceException("Identifiant utilisateur manquant.");
        }

        try (Connection c = DBConnection.getConnection()) {
            String tbl = userTable(c);
            ensureEmailAvailable(c, tbl, user.getEmail(), user.getId());

            String sql = "UPDATE " + tbl + " SET full_name = ?, email = ?, phone = ?, client_type = ? WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, user.getFullName().trim());
                ps.setString(2, user.getEmail().trim());
                ps.setString(3, user.getPhone() == null || user.getPhone().isBlank() ? null : user.getPhone().trim());
                ps.setString(4, user.getClientType() == null ? null : user.getClientType().name());
                ps.setString(5, user.getId());
                if (ps.executeUpdate() == 0) {
                    throw new ServiceException("Cet utilisateur n'existe plus.");
                }
            }
            return user;
        } catch (SQLException e) {
            throw new ServiceException("Impossible de mettre à jour le profil.", e);
        }
    }

    private Set<Role> loadRoles(Connection c, String userId) throws SQLException {
        String sql = "SELECT role_code FROM user_role WHERE user_id = ?";
        Set<Role> roles = EnumSet.noneOf(Role.class);
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roles.add(Role.valueOf(rs.getString("role_code")));
                }
            }
        }
        return roles.isEmpty() ? EnumSet.of(Role.CLIENT) : roles;
    }

    private void ensureEmailAvailable(Connection c, String tbl, String email, String currentUserId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tbl + " WHERE LOWER(email) = LOWER(?)";
        if (currentUserId != null) {
            sql += " AND id <> ?";
        }
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            if (currentUserId != null) {
                ps.setString(2, currentUserId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    throw new ServiceException(currentUserId == null
                            ? "Cet e-mail est déjà utilisé."
                            : "Cet e-mail est déjà utilisé par un autre compte.");
                }
            }
        }
    }

    private static String userTable(Connection c) {
        try {
            if (c.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql")) {
                return "\"user\"";
            }
        } catch (SQLException ignored) {
        }
        return "user";
    }
}
