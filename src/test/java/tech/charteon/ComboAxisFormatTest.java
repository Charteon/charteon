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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import tech.charteon.component.ChartComponent;
import tech.charteon.component.ChartTypeEnum;
import tech.charteon.export.ssr.EChartsSvgRenderer;
import tech.charteon.model.CategoryChartData;
import tech.charteon.model.ChartData;
import tech.charteon.model.EChartsOptionBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Combo charts (per-series type), the dual value axis, and the {@code valueFormat}
 * number formatting: verifies both the generated ECharts option structure and
 * that the generated {@code js:} formatter actually evaluates in the SSR engine.
 */
public class ComboAxisFormatTest
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static ChartData comboData()
	{
		CategoryChartData data = new CategoryChartData();
		data.addValue("Expenses", "Jan", 1200.5, null);
		data.addValue("Expenses", "Feb", 2400.0, null);
		data.addValue("Balance", "Jan", 8000.0, null);
		data.addValue("Balance", "Feb", 6000.0, null);
		data.setStyle("Expenses", null, false, null);
		data.setStyle("Balance", "line", true, null);
		return new ChartData(data, null, null, null, null, null);
	}

	@Test
	void comboSeriesTypeAndDualAxis() throws Exception
	{
		ChartComponent component = new ChartComponent();
		component.setChartType(ChartTypeEnum.BAR);
		component.setYAxisTitle("Expenses");
		component.setSecondaryAxisTitle("Balance");

		JsonNode option = MAPPER.readTree(
			EChartsOptionBuilder.buildOption(component, null, null, comboData(), null));

		// two value axes (primary + secondary)
		assertTrue(option.get("yAxis").isArray(), "dual axis -> yAxis is an array");
		assertEquals(2, option.get("yAxis").size());
		assertEquals("Expenses", option.get("yAxis").get(0).get("name").asText());
		assertEquals("Balance", option.get("yAxis").get(1).get("name").asText());

		// per-series type override + secondary-axis binding
		JsonNode series = option.get("series");
		assertEquals("bar", series.get(0).get("type").asText());
		assertEquals("line", series.get(1).get("type").asText());
		assertEquals(1, series.get(1).get("yAxisIndex").asInt());
	}

	@Test
	void valueFormatProducesAxisAndLabelFormatter()
	{
		ChartComponent component = new ChartComponent();
		component.setChartType(ChartTypeEnum.BAR);
		component.setShowValues(Boolean.TRUE);
		component.setValueFormat("#,##0.00 €");
		component.setGroupingSeparator(".");
		component.setDecimalSeparator(",");

		String json = EChartsOptionBuilder.buildOption(component, null, null, comboData(), null);
		assertTrue(json.contains("\"axisLabel\""), "value axis gets an axisLabel formatter");
		assertTrue(json.contains("js:function"), "formatter is a revived js function");
	}

	@Test
	void formatterEvaluatesInSsrEngine() throws Exception
	{
		ChartComponent component = new ChartComponent();
		component.setChartType(ChartTypeEnum.BAR);
		component.setValueFormat("#,##0.00 €");
		component.setGroupingSeparator(".");
		component.setDecimalSeparator(",");

		String json = EChartsOptionBuilder.buildOption(component, null, null, comboData(), null);
		String svg = EChartsSvgRenderer.renderSvg(json, 600, 400, null);

		assertTrue(svg != null && svg.startsWith("<svg"), "SSR must render the chart");
		// the German-formatted axis label proves the js: formatter ran without error
		assertTrue(svg.contains("8.000,00"), "axis label must be formatted as 8.000,00 €");
	}
}
