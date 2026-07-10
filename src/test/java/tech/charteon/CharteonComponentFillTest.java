/*
 * Charteon - Modern, interactive charts for JasperReports, powered by Apache ECharts.
 * Copyright (C) 2026 The Charteon Authors.
 *
 * Charteon is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Charteon is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Charteon. If not, see <https://www.gnu.org/licenses/>.
 */
package tech.charteon;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fills the overview report (all supported chart types) and exports it to PDF
 * and HTML in {@code target/test-output/}, so both files can be inspected
 * manually after {@code mvn test}.
 */
public class CharteonComponentFillTest
{
	private static final int CHART_COUNT = 31;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static Path outputDir;

	@BeforeAll
	static void createOutputDir() throws Exception
	{
		outputDir = Path.of("target", "test-output");
		Files.createDirectories(outputDir);
	}

	@Test
	void overviewReportToPdfAndHtml() throws Exception
	{
		JasperPrint print = TestReports.overviewPrint();
		assertNotNull(print);
		assertTrue(print.getPages().size() > 0, "filled report must have pages");

		// ---------- PDF (vector charts) ----------
		File pdfFile = outputDir.resolve("charteon-overview.pdf").toFile();
		JasperExportManager.exportReportToPdfFile(print, pdfFile.getAbsolutePath());
		assertTrue(pdfFile.isFile(), "PDF must exist");
		assertTrue(pdfFile.length() > 0, "PDF must not be empty");

		try (PDDocument pdf = Loader.loadPDF(pdfFile))
		{
			assertTrue(pdf.getNumberOfPages() > 0, "PDF must have pages");
		}

		// ---------- HTML (interactive charts) ----------
		File htmlFile = outputDir.resolve("charteon-overview.html").toFile();
		HtmlExporter htmlExporter = new HtmlExporter();
		htmlExporter.setExporterInput(new SimpleExporterInput(print));
		htmlExporter.setExporterOutput(new SimpleHtmlExporterOutput(htmlFile));
		htmlExporter.exportReport();
		assertTrue(htmlFile.isFile(), "HTML must exist");
		assertTrue(htmlFile.length() > 0, "HTML must not be empty");

		String html = Files.readString(htmlFile.toPath(), StandardCharsets.UTF_8);
		assertEquals(CHART_COUNT, countOccurrences(html, "<div id=\"charteon_"),
			"HTML must contain one chart container per chart");
		assertEquals(CHART_COUNT, countOccurrences(html, "echarts.init(document.getElementById"),
			"HTML must contain one chart init script per chart");
		assertEquals(1, countOccurrences(html, "charteon:echarts-library"),
			"the ECharts library must be embedded exactly once");
		assertEquals(1, countOccurrences(html, "charteon:geo-map world"),
			"the world GeoJSON must be embedded exactly once");
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
