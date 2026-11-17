package ma.fixi.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Règles de validation partagées par l'interface et les services.
 * Chaque méthode renvoie {@code null} si la valeur est valide, sinon le message d'erreur à afficher.
 */
public final class Validators {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[a-zA-Z]{2,}$");
    /** Mobile ou fixe marocain : +212 suivi de 9 chiffres commençant par 5, 6 ou 7 (ou 0 + 9 chiffres). */
    private static final Pattern MOROCCAN_PHONE = Pattern.compile("^(\\+212|0)[5-7]\\d{8}$");

    private Validators() {
    }

    public static String required(String value, String fieldLabel) {
        return value == null || value.isBlank() ? fieldLabel + " est obligatoire." : null;
    }

    public static String email(String value) {
        if (value == null || value.isBlank()) {
            return "L'e-mail est obligatoire.";
        }
        return EMAIL.matcher(value.trim()).matches() ? null : "Saisissez un e-mail valide, par exemple nom@exemple.ma.";
    }

    /** Le téléphone est facultatif ; s'il est saisi, il doit être marocain. */
    public static String optionalMoroccanPhone(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return MOROCCAN_PHONE.matcher(normalizePhone(value)).matches()
                ? null : "Saisissez un numéro marocain, par exemple +212 6 12 34 56 78.";
    }

    /** Retire espaces, points et tirets : « +212 6 12-34 » → « +212612 34 ». */
    public static String normalizePhone(String value) {
        return value == null ? null : value.replaceAll("[\\s.-]", "");
    }

    public static String password(String value) {
        if (value == null || value.isEmpty()) {
            return "Le mot de passe est obligatoire.";
        }
        return value.length() < MIN_PASSWORD_LENGTH
                ? "Le mot de passe doit contenir au moins " + MIN_PASSWORD_LENGTH + " caractères." : null;
    }

    public static String passwordConfirmation(String password, String confirmation) {
        return password != null && password.equals(confirmation) ? null : "Les mots de passe ne correspondent pas.";
    }

    /** Montant facultatif en MAD : nombre positif, au plus 2 décimales. */
    public static String optionalAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        BigDecimal amount = parseAmount(value);
        if (amount == null) {
            return "Saisissez un montant en chiffres, par exemple 500.";
        }
        return amount.signum() <= 0 ? "Le montant doit être supérieur à 0." : null;
    }

    /** Convertit « 1 500,50 » ou « 1500.5 » en BigDecimal ; {@code null} si invalide ou vide. */
    public static BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(value.replaceAll("[\\s  ]", "").replace(',', '.'));
            return amount.scale() > 2 ? null : amount;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
