package tech.charteon;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exports the overview report to CSV. CSV cannot display images, so Charteon
 * exports each chart as its data (title + name=value pairs) — this test
 * verifies that fallback.
 */
public class CsvExportTest
{
	@Test
	void exportCsv() throws Exception
	{
		File file = TestReports.outputDir().resolve("charteon-overview.csv").toFile();

		JRCsvExporter exporter = new JRCsvExporter();
		exporter.setExporterInput(new SimpleExporterInput(TestReports.overviewPrint()));
		exporter.setExporterOutput(new SimpleWriterExporterOutput(file, StandardCharsets.UTF_8.name()));
		exporter.exportReport();

		assertTrue(file.isFile() && file.length() > 0, "CSV must exist and not be empty");

		String csv = Files.readString(file.toPath(), StandardCharsets.UTF_8);
		assertTrue(csv.contains("Monthly Sales by Product"),
			"CSV must contain the chart title of the data fallback");
		assertTrue(csv.contains("Jan="),
			"CSV must contain category=value pairs of the data fallback");
		assertTrue(csv.contains("Espresso="),
			"CSV must contain the pie slice values of the data fallback");
		assertTrue(csv.contains("Checkout > Purchase=120"),
			"CSV must contain the sankey link values of the data fallback");
	}
}
