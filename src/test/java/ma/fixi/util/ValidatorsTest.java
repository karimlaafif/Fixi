package ma.fixi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ValidatorsTest {

    @Test
    void requiredRejectsEmptyAndBlank() {
        assertNotNull(Validators.required(null, "Le nom"));
        assertNotNull(Validators.required("   ", "Le nom"));
        assertNull(Validators.required("Sarah", "Le nom"));
    }

    @Test
    void emailFormat() {
        assertNull(Validators.email("client@fixi.ma"));
        assertNull(Validators.email("prenom.nom+test@exemple.co.ma"));
        assertNotNull(Validators.email(""));
        assertNotNull(Validators.email("client@"));
        assertNotNull(Validators.email("pas un email"));
    }

    @Test
    void moroccanPhoneIsOptionalButMustBeValid() {
        assertNull(Validators.optionalMoroccanPhone(null));
        assertNull(Validators.optionalMoroccanPhone(""));
        assertNull(Validators.optionalMoroccanPhone("+212 6 12 34 56 78"));
        assertNull(Validators.optionalMoroccanPhone("0612345678"));
        assertNotNull(Validators.optionalMoroccanPhone("+33612345678"));
        assertNotNull(Validators.optionalMoroccanPhone("12345"));
    }

    @Test
    void passwordNeedsEightCharacters() {
        assertNotNull(Validators.password(""));
        assertNotNull(Validators.password("court"));
        assertNull(Validators.password("fixi1234"));
    }

    @Test
    void passwordConfirmationMustMatch() {
        assertNull(Validators.passwordConfirmation("fixi1234", "fixi1234"));
        assertNotNull(Validators.passwordConfirmation("fixi1234", "fixi12345"));
    }

    @Test
    void amountParsingAcceptsFrenchFormats() {
        assertEquals(new BigDecimal("1500"), Validators.parseAmount("1 500"));
        assertEquals(new BigDecimal("1500.50"), Validators.parseAmount("1500,50"));
        assertNull(Validators.parseAmount("abc"));
        assertNull(Validators.parseAmount("10.123"));
        assertNull(Validators.optionalAmount(""));
        assertNotNull(Validators.optionalAmount("0"));
        assertNotNull(Validators.optionalAmount("-20"));
    }
}
