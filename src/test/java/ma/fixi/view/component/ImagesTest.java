package ma.fixi.view.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ImagesTest {

    @Test
    void tradeNameMapsToFileName() {
        assertEquals("electricite", Images.slug("Électricité"));
        assertEquals("plomberie", Images.slug("Plomberie"));
        assertEquals("pose-de-carrelage", Images.slug("Pose de carrelage"));
    }

    @Test
    void everySeededTradeHasAnIllustration() {
        for (String trade : new String[]{"plomberie", "electricite", "peinture", "menuiserie",
                "informatique", "climatisation", "serrurerie", "bricolage"}) {
            assertNotNull(Images.class.getResource("/images/trades/" + trade + ".png"), trade);
        }
        assertNotNull(Images.class.getResource(Images.MASCOT));
        assertNotNull(Images.class.getResource(Images.LOGO));
        assertNotNull(Images.class.getResource(Images.LOGO_WHITE));
    }
}
