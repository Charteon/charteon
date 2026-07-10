package tech.charteon;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.export.JRTextExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleTextReportConfiguration;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exports the overview report to plain text. The JasperReports text exporter
 * has no generic-element extension point, so the chart elements themselves
 * are skipped (documented limitation); the surrounding report texts must
 * still export correctly.
 */
public class TextExportTest
{
	@Test
	void exportText() throws Exception
	{
		File file = TestReports.outputDir().resolve("charteon-overview.txt").toFile();

		JRTextExporter exporter = new JRTextExporter();
		exporter.setExporterInput(new SimpleExporterInput(TestReports.overviewPrint()));
		exporter.setExporterOutput(new SimpleWriterExporterOutput(file, StandardCharsets.UTF_8.name()));
		SimpleTextReportConfiguration configuration = new SimpleTextReportConfiguration();
		configuration.setCharWidth(7f);
		configuration.setCharHeight(14f);
		exporter.setConfiguration(configuration);
		exporter.exportReport();

		assertTrue(file.isFile() && file.length() > 0, "text export must exist and not be empty");

		String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
		assertTrue(text.contains("Charteon Chart Overview"),
			"text export must contain the report texts");
	}
}
