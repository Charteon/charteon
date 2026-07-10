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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignComponentElement;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.xml.JRXmlWriter;

import tech.charteon.component.CategorySeries;
import tech.charteon.component.ChartCategoryDataset;
import tech.charteon.component.ChartComponent;
import tech.charteon.component.ChartTypeEnum;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end smoke test of the whole pipeline with a programmatically built
 * report: design → JRXML round trip → compile → fill → PDF (vector SVG via
 * GraalJS SSR).
 */
public class ChartComponentSmokeTest
{
	private static Path outputDir;

	@BeforeAll
	static void createOutputDir() throws Exception
	{
		outputDir = Path.of("target", "test-output");
		Files.createDirectories(outputDir);
	}

	@Test
	void barChartEndToEnd() throws Exception
	{
		JasperDesign design = new JasperDesign();
		design.setName("charteon-smoke");
		design.setPageWidth(595);
		design.setPageHeight(842);
		design.setOrientation(OrientationEnum.PORTRAIT);
		design.setColumnWidth(535);
		design.setLeftMargin(30);
		design.setRightMargin(30);
		design.setTopMargin(30);
		design.setBottomMargin(30);

		JRDesignField month = new JRDesignField();
		month.setName("month");
		month.setValueClass(String.class);
		design.addField(month);

		JRDesignField amount = new JRDesignField();
		amount.setName("amount");
		amount.setValueClass(Integer.class);
		design.addField(amount);

		ChartComponent chart = new ChartComponent();
		chart.setChartType(ChartTypeEnum.BAR);
		chart.setTitleExpression(new JRDesignExpression("\"Monthly Sales\""));

		ChartCategoryDataset dataset = new ChartCategoryDataset();
		CategorySeries series = new CategorySeries();
		series.setSeriesExpression(new JRDesignExpression("\"2026\""));
		series.setCategoryExpression(new JRDesignExpression("$F{month}"));
		series.setValueExpression(new JRDesignExpression("$F{amount}"));
		dataset.addSeries(series);
		chart.setCategoryDataset(dataset);

		JRDesignComponentElement element = new JRDesignComponentElement();
		element.setComponent(chart);
		element.setX(0);
		element.setY(0);
		element.setWidth(535);
		element.setHeight(300);

		JRDesignBand summary = new JRDesignBand();
		summary.setHeight(320);
		summary.addElement(element);
		design.setSummary(summary);

		// write the canonical JRXML - serves as syntax reference and validates
		// the Jackson mapping of the component model
		String jrxml = JRXmlWriter.writeReport(design, "UTF-8");
		Path jrxmlFile = outputDir.resolve("charteon-smoke.jrxml");
		Files.writeString(jrxmlFile, jrxml);
		assertTrue(jrxml.contains("kind=\"chart\""), "component must be written with kind=\"chart\"");

		// compile from the written JRXML to also cover the loading path
		JasperReport report = JasperCompileManager.compileReport(jrxmlFile.toString());
		assertNotNull(report);

		List<Map<String, ?>> records = new ArrayList<>();
		String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
		int[] amounts = {120, 200, 150, 80, 70, 110};
		for (int i = 0; i < months.length; i++)
		{
			Map<String, Object> record = new HashMap<>();
			record.put("month", months[i]);
			record.put("amount", amounts[i]);
			records.add(record);
		}

		JasperPrint print = JasperFillManager.fillReport(
			report, new HashMap<>(), new JRMapCollectionDataSource(records));
		assertNotNull(print);
		assertTrue(print.getPages().size() > 0, "filled report must have pages");

		File pdfFile = outputDir.resolve("charteon-smoke.pdf").toFile();
		JasperExportManager.exportReportToPdfFile(print, pdfFile.getAbsolutePath());
		assertTrue(pdfFile.isFile() && pdfFile.length() > 0, "PDF must exist and be non-empty");
	}
}
