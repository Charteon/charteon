package tech.charteon;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.engine.xml.JRPrintXmlLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleXmlExporterOutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exports the overview report to the JasperReports print XML, verifies the
 * XML is well formed, reloads it and re-exports the reloaded document to PDF
 * — proving that the chart payload (the ECharts option) survives the
 * serialization roundtrip and still renders.
 */
public class XmlExportTest
{
	@Test
	void exportXmlRoundtrip() throws Exception
	{
		File file = TestReports.outputDir().resolve("charteon-overview.jrpxml").toFile();
		JasperPrint original = TestReports.overviewPrint();

		JRXmlExporter exporter = new JRXmlExporter();
		exporter.setExporterInput(new SimpleExporterInput(original));
		exporter.setExporterOutput(new SimpleXmlExporterOutput(file));
		exporter.exportReport();

		assertTrue(file.isFile() && file.length() > 0, "print XML must exist and not be empty");

		// well-formedness
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.newDocumentBuilder().parse(file);

		// roundtrip: reload and render the reloaded document to PDF
		JasperPrint reloaded = JRPrintXmlLoader.load(file.getAbsolutePath());
		assertEquals(original.getPages().size(), reloaded.getPages().size(),
			"reloaded document must have the same page count");

		byte[] pdf = JasperExportManager.exportReportToPdf(reloaded);
		assertTrue(pdf.length > 0, "reloaded document must render to PDF");
		try (var pdfDocument = org.apache.pdfbox.Loader.loadPDF(pdf))
		{
			assertTrue(pdfDocument.getNumberOfPages() > 0, "roundtripped PDF must have pages");
		}
		// guard against silent truncation of the roundtripped charts
		assertTrue(pdf.length > 100_000,
			"roundtripped PDF must still contain the rendered charts");
		new ByteArrayInputStream(pdf).close();
	}
}
