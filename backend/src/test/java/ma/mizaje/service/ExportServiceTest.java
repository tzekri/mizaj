package ma.mizaje.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExportServiceTest {

	@Test
	void escapesSeparatorsAndQuotes() {
		assertEquals("Maârif", ExportService.cell("Maârif"));
		assertEquals("\"Lait; Douceur\"", ExportService.cell("Lait; Douceur"));
		assertEquals("\"Le \"\"classique\"\"\"", ExportService.cell("Le \"classique\""));
		assertEquals("", ExportService.cell(null));
	}

	/** Un prénom saisi par un client ne doit jamais devenir une formule exécutée par Excel. */
	@Test
	void neutralisesSpreadsheetFormulas() {
		assertEquals("'=1+1", ExportService.cell("=1+1"));
		assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", ExportService.cell("=HYPERLINK(\"x\")"));
		assertEquals("'+212", ExportService.cell("+212"));
		assertEquals("'@cmd", ExportService.cell("@cmd"));
	}
}
