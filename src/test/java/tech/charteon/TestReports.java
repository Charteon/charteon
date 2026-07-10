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

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

/**
 * Shared compile-and-fill setup of the export tests: the overview report
 * (all chart types) is compiled and filled once and reused by every
 * format-specific test.
 */
public final class TestReports
{
	/** The number of charts in the overview report. */
	public static final int CHART_COUNT = 31;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static JasperPrint overviewPrint;

	private TestReports()
	{
	}

	public static synchronized JasperPrint overviewPrint() throws Exception
	{
		if (overviewPrint == null)
		{
			JasperReport report;
			try (InputStream jrxml = resource("reports/charteon-overview.jrxml"))
			{
				report = JasperCompileManager.compileReport(jrxml);
			}

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("salesRecords", records("data/sales.json"));
			parameters.put("shareRecords", records("data/shares.json"));
			parameters.put("pointRecords", records("data/points.json"));
			parameters.put("funnelRecords", records("data/funnel.json"));
			parameters.put("gaugeRecords", records("data/gauge.json"));
			parameters.put("nodeRecords", records("data/nodes.json"));
			parameters.put("flowRecords", records("data/flows.json"));
			parameters.put("routeRecords", records("data/routes.json"));
			parameters.put("boxRecords", records("data/boxes.json"));
			parameters.put("ohlcRecords", records("data/ohlc.json"));
			parameters.put("heatRecords", records("data/heat.json"));
			parameters.put("geoRecords", records("data/geo.json"));
			parameters.put("customOption", resourceText("data/custom-option.json"));
			parameters.put("heatmapOption", resourceText("data/heatmap-option.json"));

			overviewPrint = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource(1));
		}
		return overviewPrint;
	}

	public static Path outputDir() throws Exception
	{
		Path dir = Path.of("target", "test-output");
		Files.createDirectories(dir);
		return dir;
	}

	public static Collection<Map<String, ?>> records(String resourceName) throws Exception
	{
		try (InputStream in = resource(resourceName))
		{
			return MAPPER.readValue(in, new TypeReference<List<Map<String, ?>>>() {});
		}
	}

	public static String resourceText(String resourceName) throws Exception
	{
		try (InputStream in = resource(resourceName))
		{
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	public static InputStream resource(String name)
	{
		InputStream in = TestReports.class.getClassLoader().getResourceAsStream(name);
		if (in == null)
		{
			throw new IllegalStateException("test resource not found: " + name);
		}
		return in;
	}
}
