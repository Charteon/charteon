package tech.charteon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleGraphics2DExporterOutput;
import net.sf.jasperreports.export.SimpleGraphics2DReportConfiguration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders the first page of the overview report through the Graphics2D
 * exporter (the code path used by the Swing viewer and java.awt printing)
 * and verifies that the chart actually painted pixels.
 */
public class Graphics2DExportTest
{
	@Test
	void exportGraphics2D() throws Exception
	{
		JasperPrint print = TestReports.overviewPrint();

		BufferedImage image = new BufferedImage(
			print.getPageWidth(), print.getPageHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

		try
		{
			JRGraphics2DExporter exporter = new JRGraphics2DExporter();
			exporter.setExporterInput(new SimpleExporterInput(print));
			SimpleGraphics2DExporterOutput output = new SimpleGraphics2DExporterOutput();
			output.setGraphics2D(graphics);
			exporter.setExporterOutput(output);
			SimpleGraphics2DReportConfiguration configuration = new SimpleGraphics2DReportConfiguration();
			configuration.setPageIndex(0);
			exporter.setConfiguration(configuration);
			exporter.exportReport();
		}
		finally
		{
			graphics.dispose();
		}

		File file = TestReports.outputDir().resolve("charteon-overview-page1.png").toFile();
		ImageIO.write(image, "png", file);
		assertTrue(file.isFile() && file.length() > 0, "PNG must exist and not be empty");

		int nonWhite = 0;
		for (int y = 0; y < image.getHeight(); y += 2)
		{
			for (int x = 0; x < image.getWidth(); x += 2)
			{
				if ((image.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF)
				{
					nonWhite++;
				}
			}
		}
		assertTrue(nonWhite > 1000, "the rendered page must not be blank, non-white samples: " + nonWhite);
	}
}
