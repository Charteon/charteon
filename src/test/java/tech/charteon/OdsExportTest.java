package tech.charteon;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exports the overview report to ODS and verifies that the charts are
 * embedded as images (high-resolution PNG fallback).
 */
public class OdsExportTest
{
	@Test
	void exportOds() throws Exception
	{
		File file = TestReports.outputDir().resolve("charteon-overview.ods").toFile();

		JROdsExporter exporter = new JROdsExporter();
		exporter.setExporterInput(new SimpleExporterInput(TestReports.overviewPrint()));
		try (FileOutputStream out = new FileOutputStream(file))
		{
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
			exporter.exportReport();
		}

		assertTrue(file.isFile() && file.length() > 0, "ODS must exist and not be empty");
		long images = ZipTestUtil.countEntries(file, "Pictures/");
		assertTrue(images >= TestReports.CHART_COUNT,
			"ODS must embed one image per chart, found " + images);
	}
}
