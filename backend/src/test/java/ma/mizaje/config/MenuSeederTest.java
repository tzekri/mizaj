package ma.mizaje.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MenuSeederTest {

	@Test
	void rebrandReplacesFormerBrandNames() {
		assertEquals("Marokoffy Signature", MenuSeeder.rebrand("Mizaje Signature"));
		assertEquals("Purple Marokoffy", MenuSeeder.rebrand("Purple Mizaj"));
		assertEquals("Café turc", MenuSeeder.rebrand("Café turc"));
		assertNull(MenuSeeder.rebrand(null));
	}
}
