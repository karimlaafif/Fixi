package ma.fixi.service.mock;

import java.util.EnumSet;
import ma.fixi.dto.RegistrationForm;
import ma.fixi.model.AccountStatus;
import ma.fixi.model.Role;
import ma.fixi.model.User;
import ma.fixi.service.AuthService;
import ma.fixi.service.ServiceException;
import org.mindrot.jbcrypt.BCrypt;

/** Authentification fictive en mémoire. Les mots de passe sont tout de même hachés avec BCrypt. */
public class MockAuthService implements AuthService {

    private final MockStore store;

    public MockAuthService(MockStore store) {
        this.store = store;
    }

    @Override
    public synchronized User login(String email, String password) {
        User user = store.users.values().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst()
                .orElse(null);
        if (user == null || !BCrypt.checkpw(password, store.passwordHashes.get(user.getId()))) {
            throw new ServiceException("E-mail ou mot de passe incorrect.");
        }
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new ServiceException("Ce compte est suspendu. Contactez le support Fixi.");
        }
        return user;
    }

    @Override
    public synchronized User register(RegistrationForm form) {
        boolean taken = store.users.values().stream()
                .anyMatch(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(form.email().trim()));
        if (taken) {
            throw new ServiceException("Cet e-mail est déjà utilisé.");
        }
        User user = new User();
        user.setId(MockStore.newId());
        user.setFullName(form.fullName().trim());
        user.setEmail(form.email().trim().toLowerCase());
        user.setPhone(form.phone());
        user.setClientType(form.role() == Role.CLIENT ? form.clientType() : null);
        user.setRoles(EnumSet.of(form.role()));
        user.setAccountStatus(AccountStatus.ACTIVE);
        store.users.put(user.getId(), user);
        store.passwordHashes.put(user.getId(), BCrypt.hashpw(form.password(), BCrypt.gensalt(10)));
        return user;
    }

    @Override
    public synchronized User updateProfile(User user) {
        boolean taken = store.users.values().stream()
                .anyMatch(u -> !u.getId().equals(user.getId())
                        && u.getEmail() != null && u.getEmail().equalsIgnoreCase(user.getEmail()));
        if (taken) {
            throw new ServiceException("Cet e-mail est déjà utilisé par un autre compte.");
        }
        store.users.put(user.getId(), user);
        return user;
    }
}
