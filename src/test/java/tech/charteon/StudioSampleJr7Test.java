package tech.charteon;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import tech.charteon.component.ChartComponent;
import tech.charteon.component.ChartTypeEnum;
import tech.charteon.model.ChartSamplePreview;

/**
 * Validates the Charteon Studio smoke-test sample report in the JasperReports 7
 * dialect (the format Jaspersoft Studio 7.x reads/writes): compile, fill against
 * a single empty record (chart data comes from the inline parameter defaults)
 * and export to a non-trivial PDF — exactly what "Preview" does inside Studio.
 * Also exercises the design-time sample preview used by the Studio canvas.
 */
class StudioSampleJr7Test
{
	@Test
	void sampleReportCompilesFillsAndExports() throws Exception
	{
		Path jrxml = extractSample();
		JasperReport report = JasperCompileManager.compileReport(jrxml.toString());
		JasperPrint print = JasperFillManager.fillReport(
			report, new java.util.HashMap<>(), new JREmptyDataSource(1));

		File outDir = new File("target/test-output");
		outDir.mkdirs();
		File pdf = new File(outDir, "charteon-studio-sample.pdf");
		JasperExportManager.exportReportToPdfFile(print, pdf.getAbsolutePath());
		assertTrue(pdf.length() > 8000,
			"expected a non-trivial PDF with two charts, got " + pdf.length());
	}

	@Test
	void designPreviewRendersSample()
	{
		ChartComponent bar = new ChartComponent();
		bar.setChartType(ChartTypeEnum.BAR);
		bar.setShowValues(Boolean.TRUE);
		byte[] svg = ChartSamplePreview.renderPreviewSvg(bar, 535, 300);
		assertNotNull(svg, "design preview SVG expected (needs JVM >= 21)");
		String text = new String(svg, java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(text.contains("<svg"), "SVG output expected");
	}

	private Path extractSample() throws Exception
	{
		// the sample lives in the sibling charteon-studio project; fall back to
		// the test resources copy so this test is self-contained in CI
		Path external = Path.of("..", "charteon-studio", "samples", "charteon-smoke-test.jrxml");
		if (Files.exists(external))
		{
			return external;
		}
		Path tmp = Files.createTempFile("charteon-studio-sample", ".jrxml");
		try (InputStream in = getClass().getResourceAsStream("/reports/charteon-studio-sample.jrxml"))
		{
			assertNotNull(in, "sample report resource missing");
			Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		return tmp;
	}
}
