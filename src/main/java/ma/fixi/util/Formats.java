package ma.fixi.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Mise en forme française des montants et des dates. */
public final class Formats {

    private static final Locale FR = Locale.FRANCE;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", FR);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM", FR);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", FR);
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", FR);

    private Formats() {
    }

    /** 12450 → « 12 450 MAD ». */
    public static String mad(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(FR);
        nf.setMaximumFractionDigits(amount.stripTrailingZeros().scale() > 0 ? 2 : 0);
        nf.setMinimumFractionDigits(amount.stripTrailingZeros().scale() > 0 ? 2 : 0);
        return nf.format(amount).replace(' ', ' ').replace(' ', ' ') + " MAD";
    }

    public static String date(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }

    public static String date(LocalDateTime dateTime) {
        return dateTime == null ? "—" : DATE.format(dateTime);
    }

    public static String time(LocalDateTime dateTime) {
        return dateTime == null ? "" : TIME.format(dateTime);
    }

    /** « samedi 15 novembre ». */
    public static String longDate(LocalDate date) {
        return date == null ? "" : LONG_DATE.format(date);
    }

    /** « 15 nov. ». */
    public static String dayMonth(LocalDate date) {
        return date == null ? "" : DAY_MONTH.format(date);
    }

    /** « Aujourd'hui », « Hier », sinon la date. Utilisé dans les listes. */
    public static String relativeDay(LocalDate date) {
        if (date == null) {
            return "";
        }
        long days = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (days == 0) {
            return "Aujourd'hui";
        }
        if (days == 1) {
            return "Hier";
        }
        if (days == -1) {
            return "Demain";
        }
        return DATE.format(date);
    }

    /** Heure si aujourd'hui, « Hier », sinon jour/mois. Utilisé dans la liste des conversations. */
    public static String conversationTime(LocalDateTime at) {
        if (at == null) {
            return "";
        }
        long days = ChronoUnit.DAYS.between(at.toLocalDate(), LocalDate.now());
        if (days == 0) {
            return time(at);
        }
        if (days == 1) {
            return "Hier";
        }
        return DateTimeFormatter.ofPattern("dd/MM", FR).format(at);
    }

    /** 4.8 → « 4,8 ». */
    public static String rating(double value) {
        return String.format(FR, "%.1f", value);
    }

    /** « 1 an », « 5 ans ». */
    public static String years(int years) {
        return years + (years > 1 ? " ans" : " an") + " d'expérience";
    }

    /** « 1 avis », « 120 avis ». */
    public static String reviews(int count) {
        return count + " avis";
    }

    /** Majuscule sur la première lettre. */
    public static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
