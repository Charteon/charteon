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

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tech.charteon.component.ChartComponent;
import tech.charteon.component.ChartTypeEnum;
import tech.charteon.model.BoxplotChartData;
import tech.charteon.model.CandlestickChartData;
import tech.charteon.model.CategoryChartData;
import tech.charteon.model.ChartData;
import tech.charteon.model.EChartsOptionBuilder;
import tech.charteon.model.HierarchyChartData;
import tech.charteon.model.RelationChartData;
import tech.charteon.model.XyChartData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Per-type assertions on the generated ECharts option.
 *
 * <p>
 * The overview report exercises every chart type end to end, but it can only
 * fail when the export blows up: a type that builds a structurally valid yet
 * wrong option - a swapped axis, a series on the wrong coordinate system, a
 * data tuple in the wrong order - still produces a perfectly good PDF. These
 * tests look at the option itself, which is the artifact the whole library
 * exists to produce.
 */
class ChartTypeOptionTest
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** Types drawn on a cartesian grid; everything else must not carry axes. */
	private static final Set<ChartTypeEnum> CARTESIAN = EnumSet.of(
		ChartTypeEnum.LINE, ChartTypeEnum.BAR, ChartTypeEnum.SCATTER,
		ChartTypeEnum.EFFECT_SCATTER, ChartTypeEnum.BOXPLOT, ChartTypeEnum.CANDLESTICK,
		ChartTypeEnum.HEATMAP, ChartTypeEnum.PICTORIAL_BAR, ChartTypeEnum.CUSTOM,
		ChartTypeEnum.LINES,
		ChartTypeEnum.STACKED_BAR, ChartTypeEnum.HORIZONTAL_BAR, ChartTypeEnum.AREA,
		ChartTypeEnum.BUBBLE);

	/** Types that colour their marks from a magnitude scale rather than the palette. */
	private static final Set<ChartTypeEnum> MAGNITUDE_SCALED =
		EnumSet.of(ChartTypeEnum.HEATMAP, ChartTypeEnum.MAP);

	private static JsonNode option(ChartTypeEnum type) throws Exception
	{
		ChartComponent component = new ChartComponent();
		component.setChartType(type);
		return option(component);
	}

	private static JsonNode option(ChartComponent component) throws Exception
	{
		return MAPPER.readTree(
			EChartsOptionBuilder.buildOption(component, null, null, sampleData(), null));
	}

	/** One dataset of every kind, so any type finds the shape it needs. */
	private static ChartData sampleData()
	{
		CategoryChartData category = new CategoryChartData();
		for (String series : new String[] { "Nord", "Sued" })
		{
			category.addValue(series, "Q1", 10.0, null);
			category.addValue(series, "Q2", 20.0, null);
			category.addValue(series, "Q3", 30.0, null);
		}

		XyChartData xy = new XyChartData();
		xy.addPoint("Messung", 1.0, 2.0, 3.0, null);
		xy.addPoint("Messung", 4.0, 5.0, 6.0, null);

		HierarchyChartData hierarchy = new HierarchyChartData();
		hierarchy.addNode("Konzern", null, null);
		hierarchy.addNode("Sparte", "Konzern", 42.0);

		RelationChartData relation = new RelationChartData();
		relation.addEdge("Werk", "Lager", 7.0, 1.0, 2.0, 3.0, 4.0);

		BoxplotChartData boxplot = new BoxplotChartData();
		boxplot.addBox("Charge", 1.0, 2.0, 3.0, 4.0, 5.0);

		CandlestickChartData candlestick = new CandlestickChartData();
		candlestick.addCandle("Mo", 10.0, 12.0, 9.0, 13.0);

		return new ChartData(category, xy, hierarchy, relation, boxplot, candlestick);
	}

	// ---------------------------------------------------------------- universal

	@ParameterizedTest
	@EnumSource(ChartTypeEnum.class)
	void everyTypeProducesItsOwnSeriesType(ChartTypeEnum type) throws Exception
	{
		JsonNode series = option(type).path("series");
		assertTrue(series.isArray() && !series.isEmpty(),
			type.getName() + " produced no series at all");
		assertEquals(type.getEchartsSeriesType(), series.get(0).path("type").asText(),
			type.getName() + " must map to its declared ECharts series type");
	}

	@ParameterizedTest
	@EnumSource(ChartTypeEnum.class)
	void onlyCartesianTypesCarryAxes(ChartTypeEnum type) throws Exception
	{
		JsonNode option = option(type);
		boolean hasAxes = option.has("xAxis") || option.has("yAxis");
		assertEquals(CARTESIAN.contains(type), hasAxes,
			type.getName() + " axis presence is wrong - a stray axis draws a line across a "
				+ "pie or gauge, a missing one drops the scale");
	}

	@ParameterizedTest
	@EnumSource(ChartTypeEnum.class)
	void everyTypeCarriesTheAccessibilityLayerAndAPalette(ChartTypeEnum type) throws Exception
	{
		JsonNode option = option(type);
		assertTrue(option.path("aria").path("enabled").asBoolean(),
			type.getName() + " lost the accessibility layer");
		assertTrue(option.path("color").isArray() && !option.path("color").isEmpty(),
			type.getName() + " lost the default palette");
	}

	@ParameterizedTest
	@EnumSource(ChartTypeEnum.class)
	void magnitudeTypesGetAColourScaleAndOthersDoNot(ChartTypeEnum type) throws Exception
	{
		assertEquals(MAGNITUDE_SCALED.contains(type), option(type).has("visualMap"),
			type.getName() + " colour-scale presence is wrong");
	}

	// ------------------------------------------------------------- data shapes

	@Test
	void boxplotKeepsTheFiveNumberSummaryInOrder() throws Exception
	{
		JsonNode box = option(ChartTypeEnum.BOXPLOT).path("series").get(0).path("data").get(0);
		assertEquals(5, box.size(), "a box needs min, q1, median, q3, max");
		for (int i = 1; i < box.size(); i++)
		{
			assertTrue(box.get(i).asDouble() >= box.get(i - 1).asDouble(),
				"ECharts reads the box positionally - out of order it draws a wrong box: " + box);
		}
	}

	@Test
	void candlestickUsesTheOpenCloseLowHighOrderEchartsExpects() throws Exception
	{
		JsonNode candle =
			option(ChartTypeEnum.CANDLESTICK).path("series").get(0).path("data").get(0);
		assertEquals(4, candle.size());
		assertEquals(10.0, candle.get(0).asDouble(), 0.001, "open");
		assertEquals(12.0, candle.get(1).asDouble(), 0.001, "close");
		assertEquals(9.0, candle.get(2).asDouble(), 0.001, "low");
		assertEquals(13.0, candle.get(3).asDouble(), 0.001, "high");
	}

	@Test
	void heatmapEmitsColumnRowValueTriples() throws Exception
	{
		JsonNode cell = option(ChartTypeEnum.HEATMAP).path("series").get(0).path("data").get(0);
		assertEquals(3, cell.size(), "heatmap data is [xIndex, yIndex, value]");
		assertTrue(cell.get(0).isInt() && cell.get(1).isInt(),
			"the first two entries index the axes, they are not values: " + cell);
	}

	@Test
	void themeRiverEmitsIndexValueSeriesTriples() throws Exception
	{
		JsonNode point =
			option(ChartTypeEnum.THEME_RIVER).path("series").get(0).path("data").get(0);
		assertEquals(3, point.size());
		assertTrue(point.get(2).isTextual(), "the third entry names the stream: " + point);
	}

	@Test
	void hierarchyTypesNestChildrenUnderTheirParent() throws Exception
	{
		for (ChartTypeEnum type : new ChartTypeEnum[] {
			ChartTypeEnum.TREE, ChartTypeEnum.TREEMAP, ChartTypeEnum.SUNBURST })
		{
			JsonNode root = option(type).path("series").get(0).path("data").get(0);
			assertEquals("Konzern", root.path("name").asText(), type.getName() + " root");
			assertEquals("Sparte", root.path("children").get(0).path("name").asText(),
				type.getName() + " must nest the child, not flatten it");
		}
	}

	@Test
	void relationTypesCarryBothNodesAndLinks() throws Exception
	{
		for (ChartTypeEnum type : new ChartTypeEnum[] { ChartTypeEnum.GRAPH, ChartTypeEnum.SANKEY })
		{
			JsonNode series = option(type).path("series").get(0);
			assertEquals(2, series.path("data").size(), type.getName() + " needs both nodes");
			assertEquals("Werk", series.path("links").get(0).path("source").asText());
			assertEquals("Lager", series.path("links").get(0).path("target").asText());
		}
	}

	@Test
	void linesPlotsGeographicCoordinatesNotCategories() throws Exception
	{
		JsonNode series = option(ChartTypeEnum.LINES).path("series").get(0);
		JsonNode coords = series.path("data").get(0).path("coords");
		assertEquals(2, coords.size(), "a line needs a start and an end");
		assertEquals(1.0, coords.get(0).get(0).asDouble(), 0.001);
		assertEquals(4.0, coords.get(1).get(1).asDouble(), 0.001);
	}

	// ------------------------------------------------------- variant aliases

	@Test
	void stackedBarAliasSetsTheStackKeyAndStaysABar() throws Exception
	{
		JsonNode series = option(ChartTypeEnum.STACKED_BAR).path("series").get(0);
		assertEquals("bar", series.path("type").asText());
		assertFalse(series.path("stack").asText().isEmpty(),
			"without a shared stack key the series draw side by side, not stacked");
	}

	@Test
	void horizontalBarAliasPutsTheCategoriesOnTheYAxis() throws Exception
	{
		JsonNode option = option(ChartTypeEnum.HORIZONTAL_BAR);
		assertEquals("category", option.path("yAxis").path("type").asText(),
			"horizontal means the categories run down the side");
		assertEquals("value", option.path("xAxis").path("type").asText());

		assertEquals("category", option(ChartTypeEnum.BAR).path("xAxis").path("type").asText(),
			"and the plain bar must be unaffected");
	}

	@Test
	void areaAliasIsALineWithAFill() throws Exception
	{
		JsonNode series = option(ChartTypeEnum.AREA).path("series").get(0);
		assertEquals("line", series.path("type").asText());
		assertTrue(series.has("areaStyle"), "the fill is what makes it an area chart");
	}

	@Test
	void doughnutAliasIsAPieWithAHole() throws Exception
	{
		JsonNode radius = option(ChartTypeEnum.DOUGHNUT).path("series").get(0).path("radius");
		assertTrue(radius.isArray(),
			"a doughnut needs an inner and an outer radius, got: " + radius);
		assertFalse(radius.get(0).asText().startsWith("0"), "inner radius must be non-zero");

		assertFalse(option(ChartTypeEnum.PIE).path("series").get(0).path("radius").isArray(),
			"a plain pie must stay solid");
	}

	@Test
	void bubbleAliasIsAScatterSizedByTheThirdValue() throws Exception
	{
		JsonNode series = option(ChartTypeEnum.BUBBLE).path("series").get(0);
		assertEquals("scatter", series.path("type").asText());
		assertTrue(series.path("data").get(0).path("symbolSize").asDouble() > 0,
			"the size expression is what separates a bubble from a scatter");
	}

	// ----------------------------------------------------------- variant flags

	@Test
	void polarMovesTheSeriesOntoThePolarCoordinateSystem() throws Exception
	{
		ChartComponent component = new ChartComponent();
		component.setChartType(ChartTypeEnum.BAR);
		component.setPolar(Boolean.TRUE);
		JsonNode option = option(component);

		assertEquals("polar", option.path("series").get(0).path("coordinateSystem").asText());
		assertFalse(option.has("xAxis"),
			"a polar chart must drop the cartesian axes, or both grids are drawn");
	}

	@Test
	void smoothAndStepBothReachTheSeries() throws Exception
	{
		ChartComponent component = new ChartComponent();
		component.setChartType(ChartTypeEnum.LINE);
		component.setSmooth(Boolean.TRUE);
		component.setStep("middle");
		JsonNode series = option(component).path("series").get(0);

		assertTrue(series.path("smooth").asBoolean());
		assertEquals("middle", series.path("step").asText());
	}
}
