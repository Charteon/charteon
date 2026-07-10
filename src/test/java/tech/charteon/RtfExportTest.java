package tech.charteon;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exports the overview report to RTF and verifies that the charts are
 * embedded as PNG images (high-resolution fallback).
 */
public class RtfExportTest
{
	@Test
	void exportRtf() throws Exception
	{
		File file = TestReports.outputDir().resolve("charteon-overview.rtf").toFile();

		JRRtfExporter exporter = new JRRtfExporter();
		exporter.setExporterInput(new SimpleExporterInput(TestReports.overviewPrint()));
		exporter.setExporterOutput(new SimpleWriterExporterOutput(file));
		exporter.exportReport();

		assertTrue(file.isFile() && file.length() > 0, "RTF must exist and not be empty");

		String rtf = Files.readString(file.toPath(), StandardCharsets.ISO_8859_1);
		int images = countOccurrences(rtf, "pngblip");
		assertTrue(images >= TestReports.CHART_COUNT,
			"RTF must embed one PNG per chart, found " + images);
	}

	private static int countOccurrences(String text, String token)
	{
		int count = 0;
		for (int index = text.indexOf(token); index >= 0; index = text.indexOf(token, index + 1))
		{
			count++;
		}
		return count;
	}
}
