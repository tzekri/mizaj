package ma.mizaj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ma.mizaj.web.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumbersTest {

	@ParameterizedTest
	@ValueSource(strings = {"0612345678", "06 12 34 56 78", "06.12.34.56.78", "+212612345678", "+212 6 12 34 56 78",
			"00212612345678", "212612345678"})
	void normalisesMoroccanMobiles(String raw) {
		assertEquals("+212612345678", PhoneNumbers.normalize(raw));
	}

	@Test
	void acceptsSevenPrefixAndForeignNumbers() {
		assertEquals("+212712345678", PhoneNumbers.normalize("07 12 34 56 78"));
		assertEquals("+33612345678", PhoneNumbers.normalize("+33 6 12 34 56 78"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "12345", "0512345678", "+21261234567", "06123456789", "abc"})
	void rejectsInvalidNumbers(String raw) {
		assertThrows(ApiException.class, () -> PhoneNumbers.normalize(raw));
	}

	@Test
	void formatsForDisplay() {
		assertEquals("06 12 34 56 78", PhoneNumbers.pretty("+212612345678"));
		assertEquals("+212 6 •• •• •6 78", PhoneNumbers.mask("+212612345678"));
	}
}
