package ma.fixi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RequestStatusTest {

    @Test
    void clientCanCancelOnlyBeforeWorkStarts() {
        assertTrue(RequestStatus.SENT.canBeCancelledByClient());
        assertTrue(RequestStatus.SCHEDULED.canBeCancelledByClient());
        assertFalse(RequestStatus.IN_PROGRESS.canBeCancelledByClient());
        assertFalse(RequestStatus.CLOSED.canBeCancelledByClient());
    }

    @Test
    void clientValidatesOnlyAfterArtisanDeclaredDone() {
        assertTrue(RequestStatus.DONE_BY_ARTISAN.canBeValidatedByClient());
        assertFalse(RequestStatus.IN_PROGRESS.canBeValidatedByClient());
        assertFalse(RequestStatus.VALIDATED.canBeValidatedByClient());
    }

    @Test
    void exitStatusesAreOutsideTheTimeline() {
        assertEquals(-1, RequestStatus.CANCELLED.getTimelineStep());
        assertEquals(-1, RequestStatus.EXPIRED.getTimelineStep());
        assertFalse(RequestStatus.CANCELLED.isActive());
    }

    @Test
    void timelineStepsAreOrdered() {
        assertEquals(0, RequestStatus.SENT.getTimelineStep());
        assertTrue(RequestStatus.QUOTE_ACCEPTED.getTimelineStep() < RequestStatus.IN_PROGRESS.getTimelineStep());
        assertEquals(RequestStatus.TIMELINE_STEPS.size() - 1, RequestStatus.CLOSED.getTimelineStep());
    }
}
