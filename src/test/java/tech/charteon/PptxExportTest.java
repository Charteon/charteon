package tech.charteon;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exports the overview report to PPTX and verifies that the charts are
 * embedded as images (high-resolution PNG fallback).
 */
public class PptxExportTest
{
	@Test
	void exportPptx() throws Exception
	{
		File file = TestReports.outputDir().resolve("charteon-overview.pptx").toFile();

		JRPptxExporter exporter = new JRPptxExporter();
		exporter.setExporterInput(new SimpleExporterInput(TestReports.overviewPrint()));
		try (FileOutputStream out = new FileOutputStream(file))
		{
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
			exporter.exportReport();
		}

		assertTrue(file.isFile() && file.length() > 0, "PPTX must exist and not be empty");
		long images = ZipTestUtil.countEntries(file, "ppt/media/");
		assertTrue(images >= TestReports.CHART_COUNT,
			"PPTX must embed one image per chart, found " + images);
	}
}
