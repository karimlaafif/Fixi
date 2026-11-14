package ma.fixi.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Compte utilisateur. Correspond aux tables {@code user} et {@code user_role}.
 * Le mot de passe n'est jamais stocké ici : seul le service d'authentification le manipule.
 */
public class User {

    private String id;
    private String fullName;
    private ClientType clientType;
    private String email;
    private String phone;
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    public User() {
    }

    public User(String id, String fullName, String email, Role role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.roles = EnumSet.of(role);
    }

    /** Rôle principal, utilisé pour choisir l'espace affiché (client, artisan ou administration). */
    public Role getPrimaryRole() {
        if (roles.contains(Role.ADMINISTRATOR)) {
            return Role.ADMINISTRATOR;
        }
        if (roles.contains(Role.ARTISAN)) {
            return Role.ARTISAN;
        }
        return Role.CLIENT;
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    /** Prénom affiché dans les salutations (« Bonjour, Sarah ! »). */
    public String getFirstName() {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        return fullName.trim().split("\\s+")[0];
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
    }
}
