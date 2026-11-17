package ma.fixi.dto;

import ma.fixi.model.ClientType;
import ma.fixi.model.Role;

/**
 * Données saisies dans l'écran « Créer un compte ».
 * Les champs sont déjà validés par l'interface (voir {@code Validators}) ;
 * le service doit tout de même revérifier l'unicité de l'e-mail.
 *
 * @param role       {@link Role#CLIENT} ou {@link Role#ARTISAN}
 * @param clientType renseigné seulement si {@code role == CLIENT}
 * @param phone      facultatif, peut être {@code null}
 * @param password   mot de passe en clair : à hacher avec BCrypt, ne jamais le stocker tel quel
 */
public record RegistrationForm(
        String fullName,
        String email,
        String phone,
        String password,
        Role role,
        ClientType clientType) {
}
