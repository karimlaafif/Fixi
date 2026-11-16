package ma.fixi.service;

import ma.fixi.dto.RegistrationForm;
import ma.fixi.model.User;

/** Connexion, inscription et mise à jour du compte. Tables : {@code user}, {@code user_role}. */
public interface AuthService {

    /**
     * Vérifie l'e-mail et le mot de passe (BCrypt.checkpw).
     *
     * @return l'utilisateur connecté
     * @throws ServiceException « E-mail ou mot de passe incorrect. » si les identifiants sont faux,
     *                          ou « Ce compte est suspendu. » si {@code account_status = SUSPENDED}
     */
    User login(String email, String password);

    /**
     * Crée le compte (mot de passe haché avec BCrypt) et ses rôles, puis renvoie l'utilisateur connecté.
     *
     * @throws ServiceException « Cet e-mail est déjà utilisé. » si l'e-mail existe déjà
     */
    User register(RegistrationForm form);

    /**
     * Met à jour le nom, l'e-mail, le téléphone et le type de client.
     *
     * @return l'utilisateur tel qu'enregistré
     */
    User updateProfile(User user);
}
