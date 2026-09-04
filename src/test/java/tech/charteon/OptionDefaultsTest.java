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

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tech.charteon.component.ChartComponent;
import tech.charteon.component.ChartTypeEnum;
import tech.charteon.model.CategoryChartData;
import tech.charteon.model.ChartData;
import tech.charteon.model.EChartsOptionBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the option defaults that are not derived from the dataset: the
 * built-in colour palette, the locale-driven number separators and the
 * accessibility layer.
 */
public class OptionDefaultsTest
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static JsonNode option(ChartComponent component, Locale locale) throws Exception
	{
		return option(component, new EChartsOptionBuilder.ReportEnvironment(locale, null, null));
	}

	private static JsonNode option(
		ChartComponent component, EChartsOptionBuilder.ReportEnvironment env) throws Exception
	{
		CategoryChartData category = new CategoryChartData();
		category.addValue("Series 1", "A", 1234.5, null);
		ChartData data = new ChartData(category, null, null, null, null, null);
		return MAPPER.readTree(
			EChartsOptionBuilder.buildOption(component, null, null, data, null, env));
	}

	private static ChartComponent barComponent()
	{
		ChartComponent component = new ChartComponent();
		component.setChartType(ChartTypeEnum.BAR);
		return component;
	}

	@Test
	void appliesDefaultPaletteWhenNeitherColorsNorThemeAreSet() throws Exception
	{
		JsonNode color = option(barComponent(), Locale.GERMANY).get("color");

		assertTrue(color != null && color.isArray() && color.size() > 1,
			"an un-themed chart must get the built-in palette, not the ECharts stock one");
		assertEquals("#2a78d6", color.get(0).asText(),
			"the first categorical slot is fixed, so colour follows the entity across reports");
	}

	@Test
	void explicitColorsWinOverTheDefaultPalette() throws Exception
	{
		ChartComponent component = barComponent();
		component.setColors("#111111, #222222");

		JsonNode color = option(component, Locale.GERMANY).get("color");

		assertEquals(2, color.size());
		assertEquals("#111111", color.get(0).asText());
	}

	@Test
	void aThemeKeepsItsOwnPalette() throws Exception
	{
		ChartComponent component = barComponent();
		component.setTheme("dark");

		assertFalse(option(component, Locale.GERMANY).has("color"),
			"overriding a theme's palette would make the theme attribute pointless");
	}

	@Test
	void numberSeparatorsFollowTheReportLocale() throws Exception
	{
		ChartComponent component = barComponent();
		component.setValueFormat("#,##0.00");
		component.setShowValues(Boolean.TRUE);

		String german = option(component, Locale.GERMANY).toString();
		String us = option(component, Locale.US).toString();

		assertTrue(german.contains("'.'") && german.contains("','"),
			"a German report groups with '.' and separates decimals with ',': " + german);
		assertTrue(us.contains("','") && us.contains("'.'"),
			"a US report does it the other way round: " + us);
		assertFalse(german.equals(us), "the two locales must not produce the same formatter");
	}

	@Test
	void explicitSeparatorsWinOverTheLocale() throws Exception
	{
		ChartComponent component = barComponent();
		component.setValueFormat("#,##0.00");
		component.setShowValues(Boolean.TRUE);
		component.setGroupingSeparator(" ");
		component.setDecimalSeparator(",");

		assertTrue(option(component, Locale.US).toString().contains("' '"),
			"an explicit separator must survive a locale that disagrees");
	}

	@Test
	void ariaIsEnabledAndDecalIsOptIn() throws Exception
	{
		JsonNode aria = option(barComponent(), Locale.GERMANY).get("aria");
		assertTrue(aria.get("enabled").asBoolean(), "the accessible description is always on");
		assertFalse(aria.get("decal").get("show").asBoolean(),
			"decals change every existing chart visibly, so they stay opt-in");

		ChartComponent component = barComponent();
		component.setDecal(Boolean.TRUE);
		assertTrue(option(component, Locale.GERMANY)
			.get("aria").get("decal").get("show").asBoolean());
	}

	@Test
	void inheritsTheReportFontAndKeepsTextStyleOutOfAnUnconfiguredChart() throws Exception
	{
		assertFalse(option(barComponent(), Locale.GERMANY).has("textStyle"),
			"a chart with no font and no textColor must not carry a textStyle");

		JsonNode inherited = option(barComponent(),
			new EChartsOptionBuilder.ReportEnvironment(Locale.GERMANY, "DejaVu Sans", 11f))
			.get("textStyle");
		assertEquals("DejaVu Sans", inherited.get("fontFamily").asText(),
			"the chart must use the report's font, not the ECharts default face");
		assertEquals(11.0, inherited.get("fontSize").asDouble(), 0.001);
	}

	@Test
	void explicitFontWinsOverTheReportStyle() throws Exception
	{
		ChartComponent component = barComponent();
		component.setFontName("Courier New");

		JsonNode textStyle = option(component,
			new EChartsOptionBuilder.ReportEnvironment(Locale.GERMANY, "DejaVu Sans", 11f))
			.get("textStyle");
		assertEquals("Courier New", textStyle.get("fontFamily").asText());
	}

	@Test
	void textColorReplacesTheHardCodedLabelColour() throws Exception
	{
		// value labels are the site that carries the colour on a bar chart
		ChartComponent labelled = barComponent();
		labelled.setShowValues(Boolean.TRUE);
		assertTrue(option(labelled, Locale.GERMANY).toString().contains("#333"),
			"the default label colour is unchanged");

		ChartComponent component = barComponent();
		component.setShowValues(Boolean.TRUE);
		component.setTextColor("#f5f5f5");
		JsonNode light = option(component, Locale.GERMANY);

		assertEquals("#f5f5f5", light.get("textStyle").get("color").asText(),
			"an explicit textColor must also drive title/legend/axis text");
		assertFalse(light.toString().contains("#333"),
			"no label may keep the hard-coded colour: " + light);
	}

	@Test
	void barsAreCappedSoFewCategoriesDoNotProduceColourFields() throws Exception
	{
		JsonNode series = option(barComponent(), Locale.GERMANY).get("series").get(0);
		assertTrue(series.has("barMaxWidth"),
			"an uncapped bar grows to its share of the category band: " + series);
		assertTrue(series.get("barMaxWidth").asInt() > 0 && series.get("barMaxWidth").asInt() <= 120,
			"cap must be a sane bar thickness");
	}

	@Test
	void lineSeriesGetNoBarCap() throws Exception
	{
		ChartComponent component = new ChartComponent();
		component.setChartType(ChartTypeEnum.LINE);
		assertFalse(option(component, Locale.GERMANY).get("series").get(0).has("barMaxWidth"));
	}

	@Test
	void legendPagesInsteadOfGrowingIntoThePlot() throws Exception
	{
		JsonNode legend = option(barComponent(), Locale.GERMANY).get("legend");
		assertEquals("scroll", legend.get("type").asText(),
			"a report element cannot scroll, so the legend must page itself");
	}

	@Test
	void sequentialRampIsSingleHueAndPrintable() throws Exception
	{
		ChartComponent component = new ChartComponent();
		component.setChartType(ChartTypeEnum.HEATMAP);
		JsonNode ramp = option(component, Locale.GERMANY).get("visualMap").get("inRange").get("color");

		assertTrue(ramp != null && ramp.size() >= 3, "a magnitude scale needs an explicit ramp");

		double previous = Double.MAX_VALUE;
		for (JsonNode step : ramp)
		{
			double luminance = relativeLuminance(step.asText());
			assertTrue(luminance < previous,
				"the ramp must get monotonically darker, broke at " + step.asText());
			previous = luminance;
		}
		assertTrue(contrastAgainstWhite(ramp.get(0).asText()) >= 2.0,
			"the light end must stay visible on paper, was "
				+ contrastAgainstWhite(ramp.get(0).asText()) + ":1");
	}

	@Test
	void colourScaleAndLegendDoNotShareTheBottomEdge() throws Exception
	{
		ChartComponent component = new ChartComponent();
		component.setChartType(ChartTypeEnum.HEATMAP);
		component.setShowLegend(Boolean.TRUE);
		JsonNode option = option(component, Locale.GERMANY);

		assertEquals(0, option.get("legend").get("bottom").asInt());
		assertTrue(option.get("visualMap").get("bottom").asInt() > 0,
			"the colour scale must move up when a legend claims the bottom edge");
	}

	private static double relativeLuminance(String hex)
	{
		String h = hex.substring(1);
		double[] channel = new double[3];
		for (int i = 0; i < 3; i++)
		{
			double v = Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16) / 255.0;
			channel[i] = v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
		}
		return 0.2126 * channel[0] + 0.7152 * channel[1] + 0.0722 * channel[2];
	}

	private static double contrastAgainstWhite(String hex)
	{
		return 1.05 / (relativeLuminance(hex) + 0.05);
	}

	@Test
	void defaultPaletteSlotsAreDistinct()
	{
		ChartComponent component = barComponent();
		List<String> slots;
		try
		{
			JsonNode color = option(component, Locale.GERMANY).get("color");
			slots = MAPPER.convertValue(color, MAPPER.getTypeFactory()
				.constructCollectionType(List.class, String.class));
		}
		catch (Exception e)
		{
			throw new AssertionError(e);
		}
		assertEquals(slots.size(), slots.stream().distinct().count(),
			"a repeated hue would make two series indistinguishable");
	}
}
