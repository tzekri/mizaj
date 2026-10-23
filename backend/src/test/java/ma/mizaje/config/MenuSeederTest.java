package ma.mizaje.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MenuSeederTest {

	@Test
	void rebrandReplacesFormerBrandNames() {
		assertEquals("Kidima Signature", MenuSeeder.rebrand("Mizaje Signature"));
		assertEquals("Purple Kidima", MenuSeeder.rebrand("Purple Mizaj"));
		assertEquals("Kidima Intense", MenuSeeder.rebrand("Marokoffy Intense"));
		assertEquals("Café turc", MenuSeeder.rebrand("Café turc"));
		assertNull(MenuSeeder.rebrand(null));
	}
}
