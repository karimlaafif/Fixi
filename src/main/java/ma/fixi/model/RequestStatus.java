package ma.fixi.model;

import java.util.List;

/**
 * Cycle de vie d'une demande, tel que décrit dans le cahier des charges :
 * Demande envoyée → Devis reçu → Devis accepté → Intervention planifiée → En cours
 * → Terminée par l'artisan → Validée par le client → Clôturée et évaluée.
 * Sorties possibles : expirée, annulée, litige ouvert.
 *
 * <p>L'interface affiche une frise de 6 étapes ({@link #TIMELINE_STEPS}) ;
 * {@link #getTimelineStep()} indique l'étape atteinte.</p>
 */
public enum RequestStatus {
    SENT("Envoyée", Tone.NEUTRAL, 0),
    QUOTE_RECEIVED("Devis reçu", Tone.INFO, 1),
    QUOTE_ACCEPTED("Devis accepté", Tone.INFO, 2),
    SCHEDULED("Planifiée", Tone.INFO, 3),
    IN_PROGRESS("En cours", Tone.WARNING, 3),
    DONE_BY_ARTISAN("À valider", Tone.WARNING, 4),
    VALIDATED("Terminée", Tone.SUCCESS, 4),
    CLOSED("Clôturée", Tone.SUCCESS, 5),
    EXPIRED("Expirée", Tone.DANGER, -1),
    CANCELLED("Annulée", Tone.DANGER, -1),
    DISPUTED("Litige ouvert", Tone.DANGER, -1);

    /** Libellés de la frise de suivi, dans l'ordre. */
    public static final List<String> TIMELINE_STEPS = List.of(
            "Demande envoyée", "Devis reçu", "Devis accepté", "Intervention", "Terminée", "Évaluée");

    /** Couleur sémantique du badge de statut. */
    public enum Tone { NEUTRAL, INFO, WARNING, SUCCESS, DANGER }

    private final String label;
    private final Tone tone;
    private final int timelineStep;

    RequestStatus(String label, Tone tone, int timelineStep) {
        this.label = label;
        this.tone = tone;
        this.timelineStep = timelineStep;
    }

    public String getLabel() {
        return label;
    }

    public Tone getTone() {
        return tone;
    }

    /** Index de l'étape atteinte dans {@link #TIMELINE_STEPS}, ou -1 pour un statut de sortie. */
    public int getTimelineStep() {
        return timelineStep;
    }

    /** Vrai tant que la demande n'est ni terminée ni interrompue. */
    public boolean isActive() {
        return timelineStep >= 0 && this != VALIDATED && this != CLOSED;
    }

    /** Vrai si le travail est fini (validé ou clôturé). */
    public boolean isFinished() {
        return this == VALIDATED || this == CLOSED;
    }

    /** Le client peut annuler tant que les travaux n'ont pas commencé. */
    public boolean canBeCancelledByClient() {
        return this == SENT || this == QUOTE_RECEIVED || this == QUOTE_ACCEPTED || this == SCHEDULED;
    }

    /** Le client peut changer la date souhaitée avant le début des travaux. */
    public boolean canChangeDate() {
        return canBeCancelledByClient();
    }

    /** Le client ne valide la fin qu'après que l'artisan a déclaré les travaux terminés. */
    public boolean canBeValidatedByClient() {
        return this == DONE_BY_ARTISAN;
    }
}
