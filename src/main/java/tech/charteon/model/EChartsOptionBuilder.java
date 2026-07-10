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
package tech.charteon.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.sf.jasperreports.engine.JRRuntimeException;

import tech.charteon.component.ChartComponent;
import tech.charteon.component.ChartTypeEnum;

/**
 * Builds the Apache ECharts option object (as a JSON string) from the
 * evaluated dataset data. The generated option follows the public ECharts
 * option API exclusively.
 *
 * <p>
 * Presentation variants (stacked, horizontal, area fill, smooth/step,
 * doughnut/rose, polar) are read from the component properties; the pre-v2
 * variant type aliases are normalized to base type + implied property first.
 *
 * <p>
 * If a raw option (from {@code optionExpression}) is present, it is
 * deep-merged over the generated option; values from the raw option win,
 * arrays are replaced as a whole.
 */
public final class EChartsOptionBuilder
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * The effective, alias-normalized chart settings.
	 */
	private record Settings(
		ChartTypeEnum type,
		boolean showValues,
		boolean stacked,
		boolean horizontal,
		boolean filled,
		boolean smooth,
		String step,
		String innerRadius,
		String roseType,
		boolean polar,
		String symbol,
		String mapName,
		String graphLayout,
		String valueFormat,
		String groupingSeparator,
		String decimalSeparator,
		String xAxisTitle,
		String yAxisTitle,
		String secondaryAxisTitle,
		String colors,
		boolean colorByCategory)
	{
	}

	private EChartsOptionBuilder()
	{
	}

	public static String buildOption(
		ChartComponent component,
		String title,
		String subtitle,
		ChartData data,
		String rawOptionJson)
	{
		try
		{
			ObjectNode option = MAPPER.createObjectNode();
			Settings settings = normalize(component);
			if (data == null)
			{
				data = ChartData.EMPTY;
			}

			if (settings.type() != null)
			{
				switch (settings.type().getDatasetKind())
				{
					case CATEGORY:
						buildCategoryOption(option, settings,
							data.category() == null ? new CategoryChartData() : data.category());
						break;
					case XY:
						buildXyOption(option, settings,
							data.xy() == null ? new XyChartData() : data.xy());
						break;
					case HIERARCHY:
						buildHierarchyOption(option, settings,
							data.hierarchy() == null ? new HierarchyChartData() : data.hierarchy());
						break;
					case RELATION:
						buildRelationOption(option, settings,
							data.relation() == null ? new RelationChartData() : data.relation());
						break;
					case BOXPLOT:
						buildBoxplotOption(option, settings,
							data.boxplot() == null ? new BoxplotChartData() : data.boxplot());
						break;
					case CANDLESTICK:
						buildCandlestickOption(option, settings,
							data.candlestick() == null ? new CandlestickChartData() : data.candlestick());
						break;
				}
			}

			addTitle(option, title, subtitle);
			addLegend(option, settings.type(), component.getShowLegend());
			addColorPalette(option, settings);

			if (rawOptionJson != null && !rawOptionJson.isBlank())
			{
				JsonNode rawOption = MAPPER.readTree(rawOptionJson);
				if (!(rawOption instanceof ObjectNode))
				{
					throw new JRRuntimeException(
						"Charteon: optionExpression must produce a JSON object, but got: "
							+ rawOption.getNodeType());
				}
				deepMerge(option, (ObjectNode) rawOption);
			}

			return MAPPER.writeValueAsString(option);
		}
		catch (JsonProcessingException e)
		{
			throw new JRRuntimeException(e);
		}
	}

	/**
	 * Resolves the pre-v2 variant type aliases into base type + implied
	 * property and applies the component's variant properties.
	 */
	private static Settings normalize(ChartComponent component)
	{
		ChartTypeEnum declared = component.getChartType();
		ChartTypeEnum base = declared == null ? null : declared.getBaseType();

		boolean stacked = Boolean.TRUE.equals(component.getStacked())
			|| declared == ChartTypeEnum.STACKED_BAR;
		boolean horizontal = Boolean.TRUE.equals(component.getHorizontal())
			|| declared == ChartTypeEnum.HORIZONTAL_BAR;
		boolean filled = Boolean.TRUE.equals(component.getFilled())
			|| declared == ChartTypeEnum.AREA;
		String innerRadius = component.getInnerRadius() != null
			? component.getInnerRadius()
			: (declared == ChartTypeEnum.DOUGHNUT ? "40%" : null);

		return new Settings(
			base,
			Boolean.TRUE.equals(component.getShowValues()),
			stacked,
			horizontal,
			filled,
			Boolean.TRUE.equals(component.getSmooth()),
			component.getStep(),
			innerRadius,
			component.getRoseType(),
			Boolean.TRUE.equals(component.getPolar()),
			component.getSymbol(),
			component.getMapName() == null ? "world" : component.getMapName(),
			component.getGraphLayout() == null ? "circular" : component.getGraphLayout(),
			blankToNull(component.getValueFormat()),
			component.getGroupingSeparator() == null ? "," : component.getGroupingSeparator(),
			component.getDecimalSeparator() == null ? "." : component.getDecimalSeparator(),
			blankToNull(component.getXAxisTitle()),
			blankToNull(component.getYAxisTitle()),
			blankToNull(component.getSecondaryAxisTitle()),
			blankToNull(component.getColors()),
			Boolean.TRUE.equals(component.getColorByCategory()));
	}

	private static String blankToNull(String value)
	{
		return value == null || value.isBlank() ? null : value;
	}

	private static void addTitle(ObjectNode option, String title, String subtitle)
	{
		if (title != null || subtitle != null)
		{
			ObjectNode titleNode = option.putObject("title");
			if (title != null)
			{
				titleNode.put("text", title);
			}
			if (subtitle != null)
			{
				titleNode.put("subtext", subtitle);
			}
			titleNode.put("left", "center");
		}
	}

	private static void addLegend(ObjectNode option, ChartTypeEnum chartType, Boolean showLegend)
	{
		int seriesCount = option.has("series") ? option.get("series").size() : 0;
		boolean show = showLegend != null
			? showLegend
			: (seriesCount > 1
				|| chartType == ChartTypeEnum.PIE
				|| chartType == ChartTypeEnum.FUNNEL);
		ObjectNode legend = option.putObject("legend");
		legend.put("show", show);
		legend.put("bottom", 0);
	}

	/**
	 * Applies the component's {@code colors} palette (comma-separated) to the
	 * option root, so it cycles across series (or categories when
	 * {@code colorByCategory} is set). A raw {@code optionExpression} colour
	 * still wins, as it is merged afterwards.
	 */
	private static void addColorPalette(ObjectNode option, Settings settings)
	{
		if (settings.colors() == null)
		{
			return;
		}
		ArrayNode palette = option.putArray("color");
		for (String color : settings.colors().split(","))
		{
			String trimmed = color.trim();
			if (!trimmed.isEmpty())
			{
				palette.add(trimmed);
			}
		}
	}

	private static void buildCategoryOption(
		ObjectNode option, Settings settings, CategoryChartData data)
	{
		switch (settings.type())
		{
			case BAR:
			case LINE:
				if (settings.polar())
				{
					buildPolarAxisOption(option, settings, data);
				}
				else
				{
					buildAxisOption(option, settings, data);
				}
				break;
			case PIE:
				buildPieOption(option, settings, data);
				break;
			case RADAR:
				buildRadarOption(option, settings, data);
				break;
			case GAUGE:
				buildGaugeOption(option, data);
				break;
			case FUNNEL:
				buildFunnelOption(option, settings, data);
				break;
			case HEATMAP:
				buildHeatmapOption(option, data);
				break;
			case MAP:
				buildMapOption(option, settings, data);
				break;
			case PARALLEL:
				buildParallelOption(option, data);
				break;
			case THEME_RIVER:
				buildThemeRiverOption(option, data);
				break;
			case PICTORIAL_BAR:
				buildPictorialBarOption(option, settings, data);
				break;
			default:
				throw new JRRuntimeException(
					"Charteon: unsupported typed category chart type " + settings.type());
		}
	}

	private static void buildAxisOption(
		ObjectNode option, Settings settings, CategoryChartData data)
	{
		ObjectNode tooltip = option.putObject("tooltip");
		tooltip.put("trigger", "axis");
		ObjectNode grid = option.putObject("grid");
		grid.put("containLabel", true);

		ArrayNode categoriesNode = MAPPER.createArrayNode();
		data.getCategories().forEach(categoriesNode::add);

		boolean horizontal = settings.horizontal();
		String categoryAxisKey = horizontal ? "yAxis" : "xAxis";
		String valueAxisKey = horizontal ? "xAxis" : "yAxis";
		String valueAxisTitle = valueAxisKey.equals("yAxis") ? settings.yAxisTitle() : settings.xAxisTitle();
		String categoryAxisTitle = categoryAxisKey.equals("xAxis") ? settings.xAxisTitle() : settings.yAxisTitle();

		ObjectNode categoryAxis = option.putObject(categoryAxisKey);
		categoryAxis.put("type", "category");
		categoryAxis.set("data", categoriesNode);
		applyAxisTitle(categoryAxis, categoryAxisTitle, false);

		String formatter = numberFormatterJs(settings);
		if (data.hasSecondaryAxis())
		{
			// combo chart with a second value axis: bars on the primary, e.g. a
			// trend line on the secondary (opposite) side
			ArrayNode valueAxes = option.putArray(valueAxisKey);
			ObjectNode primary = valueAxes.addObject();
			primary.put("type", "value");
			applyAxisTitle(primary, valueAxisTitle, true);
			applyAxisFormatter(primary, formatter);
			ObjectNode secondary = valueAxes.addObject();
			secondary.put("type", "value");
			applyAxisTitle(secondary, settings.secondaryAxisTitle(), true);
			applyAxisFormatter(secondary, formatter);
		}
		else
		{
			ObjectNode valueAxis = option.putObject(valueAxisKey);
			valueAxis.put("type", "value");
			applyAxisTitle(valueAxis, valueAxisTitle, true);
			applyAxisFormatter(valueAxis, formatter);
		}
		if (formatter != null)
		{
			tooltip.put("valueFormatter", formatter);
		}

		String axisIndexKey = horizontal ? "xAxisIndex" : "yAxisIndex";

		ArrayNode seriesArray = option.putArray("series");
		for (Map.Entry<String, Map<String, Number>> series : data.getSeriesValues().entrySet())
		{
			CategoryChartData.SeriesStyle style = data.getStyle(series.getKey());
			ObjectNode seriesNode = seriesArray.addObject();
			seriesNode.put("name", series.getKey());
			// combo: a per-series type overrides the chart's base type
			seriesNode.put("type", style != null && style.type() != null
				? style.type()
				: settings.type().getEchartsSeriesType());
			if (style != null && style.secondaryAxis())
			{
				seriesNode.put(axisIndexKey, 1);
			}
			if (style != null && style.color() != null)
			{
				seriesNode.putObject("itemStyle").put("color", style.color());
			}
			if (settings.colorByCategory())
			{
				seriesNode.put("colorBy", "data");
			}
			applyLineBarVariants(seriesNode, settings);

			ArrayNode dataArray = seriesNode.putArray("data");
			for (String category : data.getCategories())
			{
				Number value = series.getValue().get(category);
				String label = data.getLabel(series.getKey(), category);
				addDataItem(dataArray, value, label);
			}
		}
	}

	private static void applyAxisTitle(ObjectNode axis, String title, boolean valueAxis)
	{
		if (title == null)
		{
			return;
		}
		axis.put("name", title);
		if (!valueAxis)
		{
			// center the category-axis title under the axis; the value-axis title
			// keeps its default end position so it does not overlap the labels
			axis.put("nameLocation", "middle");
			axis.put("nameGap", 28);
		}
	}

	private static void applyAxisFormatter(ObjectNode axis, String formatter)
	{
		if (formatter != null)
		{
			axis.putObject("axisLabel").put("formatter", formatter);
		}
	}

	private static void buildPolarAxisOption(
		ObjectNode option, Settings settings, CategoryChartData data)
	{
		option.putObject("tooltip").put("trigger", "item");
		option.putObject("polar");

		ArrayNode categoriesNode = MAPPER.createArrayNode();
		data.getCategories().forEach(categoriesNode::add);
		ObjectNode angleAxis = option.putObject("angleAxis");
		angleAxis.put("type", "category");
		angleAxis.set("data", categoriesNode);
		option.putObject("radiusAxis");

		ArrayNode seriesArray = option.putArray("series");
		for (Map.Entry<String, Map<String, Number>> series : data.getSeriesValues().entrySet())
		{
			ObjectNode seriesNode = seriesArray.addObject();
			seriesNode.put("name", series.getKey());
			seriesNode.put("type", settings.type().getEchartsSeriesType());
			seriesNode.put("coordinateSystem", "polar");
			applyLineBarVariants(seriesNode, settings);

			ArrayNode dataArray = seriesNode.putArray("data");
			for (String category : data.getCategories())
			{
				addNumber(dataArray, series.getValue().get(category));
			}
		}
	}

	private static void applyLineBarVariants(ObjectNode seriesNode, Settings settings)
	{
		if (settings.showValues())
		{
			addValueLabel(seriesNode, settings);
		}
		if (settings.stacked())
		{
			seriesNode.put("stack", "total");
		}
		if (settings.filled())
		{
			seriesNode.putObject("areaStyle");
		}
		if (settings.smooth())
		{
			seriesNode.put("smooth", true);
		}
		if (settings.step() != null)
		{
			seriesNode.put("step", settings.step());
		}
	}

	private static void buildPieOption(
		ObjectNode option, Settings settings, CategoryChartData data)
	{
		option.putObject("tooltip").put("trigger", "item");

		ArrayNode seriesArray = option.putArray("series");
		// a pie chart uses the first series of the dataset; the category
		// becomes the slice name
		Map.Entry<String, Map<String, Number>> firstSeries = firstSeries(data);
		ObjectNode seriesNode = seriesArray.addObject();
		if (firstSeries != null)
		{
			seriesNode.put("name", firstSeries.getKey());
		}
		seriesNode.put("type", "pie");
		// A slightly reduced outer radius leaves room for the outside labels and
		// their leader lines, so headings are not clipped at the canvas edge
		// (especially once the chart is scaled to a non-wide element).
		if (settings.innerRadius() != null)
		{
			ArrayNode radius = seriesNode.putArray("radius");
			radius.add(settings.innerRadius());
			radius.add("62%");
		}
		else
		{
			seriesNode.put("radius", "62%");
		}
		if (settings.roseType() != null)
		{
			seriesNode.put("roseType", settings.roseType());
		}
		// Always label the slices with their name (the category), and value too
		// when "show values" is on. Making this explicit — rather than relying on
		// the ECharts default — guarantees the heading appears at the leader line.
		ObjectNode label = seriesNode.putObject("label");
		label.put("show", true);
		String pieExpr = numberFormatterExpr(settings);
		if (settings.showValues() && pieExpr != null)
		{
			// format the slice value with valueFormat, keeping "name: value"
			label.put("formatter", "js:function(p){return p.name+': '+(" + pieExpr + ")(p);}");
		}
		else
		{
			label.put("formatter", settings.showValues() ? "{b}: {c}" : "{b}");
		}
		label.put("color", "#333");
		seriesNode.putObject("labelLine").put("show", true);
		ArrayNode dataArray = seriesNode.putArray("data");
		if (firstSeries != null)
		{
			for (Map.Entry<String, Number> item : firstSeries.getValue().entrySet())
			{
				ObjectNode itemNode = dataArray.addObject();
				itemNode.put("name", item.getKey());
				putNumber(itemNode, "value", item.getValue());
			}
		}
	}

	private static void buildRadarOption(ObjectNode option, Settings settings, CategoryChartData data)
	{
		option.putObject("tooltip").put("trigger", "item");

		double max = 0;
		for (Map<String, Number> values : data.getSeriesValues().values())
		{
			for (Number value : values.values())
			{
				if (value != null && value.doubleValue() > max)
				{
					max = value.doubleValue();
				}
			}
		}
		double indicatorMax = max > 0 ? max * 1.1 : 1;

		ObjectNode radar = option.putObject("radar");
		ArrayNode indicators = radar.putArray("indicator");
		for (String category : data.getCategories())
		{
			ObjectNode indicator = indicators.addObject();
			indicator.put("name", category);
			indicator.put("max", indicatorMax);
		}

		ArrayNode seriesArray = option.putArray("series");
		ObjectNode seriesNode = seriesArray.addObject();
		seriesNode.put("type", "radar");
		if (settings.showValues())
		{
			ObjectNode label = seriesNode.putObject("label");
			label.put("show", true);
		}
		ArrayNode dataArray = seriesNode.putArray("data");
		for (Map.Entry<String, Map<String, Number>> series : data.getSeriesValues().entrySet())
		{
			ObjectNode itemNode = dataArray.addObject();
			itemNode.put("name", series.getKey());
			ArrayNode values = itemNode.putArray("value");
			for (String category : data.getCategories())
			{
				Number value = series.getValue().get(category);
				if (value == null)
				{
					values.addNull();
				}
				else
				{
					values.add(value.doubleValue());
				}
			}
		}
	}

	private static void buildGaugeOption(ObjectNode option, CategoryChartData data)
	{
		option.putObject("tooltip").put("trigger", "item");

		ArrayNode seriesArray = option.putArray("series");
		ObjectNode seriesNode = seriesArray.addObject();
		seriesNode.put("type", "gauge");
		ObjectNode detail = seriesNode.putObject("detail");
		detail.put("valueAnimation", false);

		ArrayNode dataArray = seriesNode.putArray("data");
		Map.Entry<String, Map<String, Number>> firstSeries = firstSeries(data);
		if (firstSeries != null)
		{
			for (Map.Entry<String, Number> item : firstSeries.getValue().entrySet())
			{
				ObjectNode itemNode = dataArray.addObject();
				itemNode.put("name", item.getKey());
				putNumber(itemNode, "value", item.getValue());
			}
		}
	}

	private static void buildFunnelOption(ObjectNode option, Settings settings, CategoryChartData data)
	{
		option.putObject("tooltip").put("trigger", "item");

		ArrayNode seriesArray = option.putArray("series");
		ObjectNode seriesNode = seriesArray.addObject();
		seriesNode.put("type", "funnel");
		if (settings.showValues())
		{
			ObjectNode label = seriesNode.putObject("label");
			label.put("show", true);
			label.put("formatter", "{b}: {c}");
		}
		Map.Entry<String, Map<String, Number>> firstSeries = firstSeries(data);
		if (firstSeries != null)
		{
			seriesNode.put("name", firstSeries.getKey());
		}
		ArrayNode dataArray = seriesNode.putArray("data");
		if (firstSeries != null)
		{
			for (Map.Entry<String, Number> item : firstSeries.getValue().entrySet())
			{
				ObjectNode itemNode = dataArray.addObject();
				itemNode.put("name", item.getKey());
				putNumber(itemNode, "value", item.getValue());
			}
		}
	}

	/**
	 * Cartesian heatmap: the categories form the x axis, the series names the
	 * y axis, each value one colored cell.
	 */
	private static void buildHeatmapOption(ObjectNode option, CategoryChartData data)
	{
		option.putObject("tooltip").put("trigger", "item");
		ObjectNode grid = option.putObject("grid");
		grid.put("containLabel", true);

		List<String> categories = List.copyOf(data.getCategories());
		List<String> seriesNames = List.copyOf(data.getSeriesValues().keySet());

		ArrayNode categoriesNode = MAPPER.createArrayNode();
		categories.forEach(categoriesNode::add);
		ObjectNode xAxis = option.putObject("xAxis");
		xAxis.put("type", "category");
		xAxis.set("data", categoriesNode);

		ArrayNode seriesNamesNode = MAPPER.createArrayNode();
		seriesNames.forEach(seriesNamesNode::add);
		ObjectNode yAxis = option.putObject("yAxis");
		yAxis.put("type", "category");
		yAxis.set("data", seriesNamesNode);

		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;

		ArrayNode seriesArray = option.putArray("series");
		ObjectNode seriesNode = seriesArray.addObject();
		seriesNode.put("type", "heatmap");
		ObjectNode label = seriesNode.putObject("label");
		label.put("show", true);
		ArrayNode dataArray = seriesNode.putArray("data");
		for (int y = 0; y < seriesNames.size(); y++)
		{
			Map<String, Number> values = data.getSeriesValues().get(seriesNames.get(y));
			for (int x = 0; x < categories.size(); x++)
			{
				Number value = values.get(categories.get(x));
				if (value != null)
				{
					ArrayNode cell = dataArray.addArray();
					cell.add(x);
					cell.add(y);
					cell.add(value.doubleValue());
					min = Math.min(min, value.doubleValue());
					max = Math.max(max, value.doubleValue());
				}
			}
		}

		addVisualMap(option, min, max);
	}

	/**
	 * Choropleth map: the categories are region names of the registered geo
	 * map, the values drive the color scale.
	 */
	private static void buildMapOption(ObjectNode option, Settings settings, CategoryChartData data)
	{
		option.putObject("tooltip").put("trigger", "item");

		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;

		ArrayNode seriesArray = option.putArray("series");
		ObjectNode seriesNode = seriesArray.addObject();
		seriesNode.put("type", "map");
		seriesNode.put("map", settings.mapName());
		if (settings.showValues())
		{
			ObjectNode label = seriesNode.putObject("label");
			label.put("show", true);
			label.put("formatter", "{c}");
		}
		Map.Entry<String, Map<String, Number>> firstSeries = firstSeries(data);
		if (firstSeries != null)
		{
			seriesNode.put("name", firstSeries.getKey());
		}
		ArrayNode dataArray = seriesNode.putArray("data");
		if (firstSeries != null)
		{
			for (Map.Entry<String, Number> item : firstSeries.getValue().entrySet())
			{
				ObjectNode itemNode = dataArray.addObject();
				itemNode.put("name", item.getKey());
				putNumber(itemNode, "value", item.getValue());
				if (item.getValue() != null)
				{
					min = Math.min(min, item.getValue().doubleValue());
					max = Math.max(max, item.getValue().doubleValue());
				}
			}
		}

		addVisualMap(option, min, max);
	}

	/**
	 * Parallel coordinates: the categories become the parallel axes, each
	 * series one line across them.
	 */
	private static void buildParallelOption(ObjectNode option, CategoryChartData data)
	{
		option.putObject("tooltip").put("trigger", "item");

		List<String> categories = List.copyOf(data.getCategories());
		ArrayNode axes = option.putArray("parallelAxis");
		for (int dim = 0; dim < categories.size(); dim++)
		{
			ObjectNode axis = axes.addObject();
			axis.put("dim", dim);
			axis.put("name", categories.get(dim));
		}

		ArrayNode seriesArray = option.putArray("series");
		for (Map.Entry<String, Map<String, Number>> series : data.getSeriesValues().entrySet())
		{
			ObjectNode seriesNode = seriesArray.addObject();
			seriesNode.put("name", series.getKey());
			seriesNode.put("type", "parallel");
			ObjectNode lineStyle = seriesNode.putObject("lineStyle");
			lineStyle.put("width", 2);
			ArrayNode dataArray = seriesNode.putArray("data");
			ArrayNode lineValues = dataArray.addArray();
			for (String category : categories)
			{
				addNumber(lineValues, series.getValue().get(category));
			}
		}
	}

	/**
	 * Theme river: the categories are the x positions, each series one
	 * stream; the data triples are (x, value, series name). The themeRiver
	 * layout does not support a category axis, so the categories are mapped
	 * to numeric indices and shown through a revived label formatter.
	 */
	private static void buildThemeRiverOption(ObjectNode option, CategoryChartData data)
	{
		ObjectNode tooltip = option.putObject("tooltip");
		tooltip.put("trigger", "axis");

		List<String> categories = List.copyOf(data.getCategories());
		ArrayNode categoriesJson = MAPPER.createArrayNode();
		categories.forEach(categoriesJson::add);

		ObjectNode singleAxis = option.putObject("singleAxis");
		singleAxis.put("type", "value");
		singleAxis.put("min", 0);
		singleAxis.put("max", Math.max(categories.size() - 1, 1));
		singleAxis.put("interval", 1);
		singleAxis.put("bottom", 40);
		ObjectNode axisLabel = singleAxis.putObject("axisLabel");
		axisLabel.put("formatter", "js:function(v){var c=" + categoriesJson.toString()
			+ ";return c[Math.round(v)]||'';}");

		ArrayNode seriesArray = option.putArray("series");
		ObjectNode seriesNode = seriesArray.addObject();
		seriesNode.put("type", "themeRiver");
		ArrayNode dataArray = seriesNode.putArray("data");
		for (Map.Entry<String, Map<String, Number>> series : data.getSeriesValues().entrySet())
		{
			for (int x = 0; x < categories.size(); x++)
			{
				Number value = series.getValue().get(categories.get(x));
				ArrayNode triple = dataArray.addArray();
				triple.add(x);
				triple.add(value == null ? 0 : value.doubleValue());
				triple.add(series.getKey());
			}
		}
	}

	private static void buildPictorialBarOption(
		ObjectNode option, Settings settings, CategoryChartData data)
	{
		option.putObject("tooltip").put("trigger", "axis");
		ObjectNode grid = option.putObject("grid");
		grid.put("containLabel", true);

		ArrayNode categoriesNode = MAPPER.createArrayNode();
		data.getCategories().forEach(categoriesNode::add);
		boolean horizontal = settings.horizontal();
		ObjectNode categoryAxis = option.putObject(horizontal ? "yAxis" : "xAxis");
		categoryAxis.put("type", "category");
		categoryAxis.set("data", categoriesNode);
		option.putObject(horizontal ? "xAxis" : "yAxis").put("type", "value");

		ArrayNode seriesArray = option.putArray("series");
		for (Map.Entry<String, Map<String, Number>> series : data.getSeriesValues().entrySet())
		{
			ObjectNode seriesNode = seriesArray.addObject();
			seriesNode.put("name", series.getKey());
			seriesNode.put("type", "pictorialBar");
			if (settings.showValues())
			{
				addValueLabel(seriesNode, settings);
			}
			seriesNode.put("symbol", settings.symbol() == null ? "circle" : settings.symbol());
			seriesNode.put("symbolRepeat", true);
			seriesNode.put("symbolClip", true);
			seriesNode.put("symbolMargin", 2);
			ArrayNode symbolSize = seriesNode.putArray("symbolSize");
			if (horizontal)
			{
				symbolSize.add(10);
				symbolSize.add("70%");
			}
			else
			{
				symbolSize.add("70%");
				symbolSize.add(10);
			}
			ArrayNode dataArray = seriesNode.putArray("data");
			for (String category : data.getCategories())
			{
				addNumber(dataArray, series.getValue().get(category));
			}
		}
	}

	private static void buildXyOption(
		ObjectNode option, Settings settings, XyChartData data)
	{
		option.putObject("tooltip").put("trigger", "item");
		ObjectNode grid = option.putObject("grid");
		grid.put("containLabel", true);
		option.putObject("xAxis").put("type", "value");
		option.putObject("yAxis").put("type", "value");

		// normalize bubble sizes into a readable pixel range
		double minSize = Double.MAX_VALUE;
		double maxSize = -Double.MAX_VALUE;
		boolean hasSizes = false;
		for (List<XyChartData.Point> points : data.getSeriesPoints().values())
		{
			for (XyChartData.Point point : points)
			{
				if (point.size() != null)
				{
					hasSizes = true;
					minSize = Math.min(minSize, point.size().doubleValue());
					maxSize = Math.max(maxSize, point.size().doubleValue());
				}
			}
		}

		ArrayNode seriesArray = option.putArray("series");
		for (Map.Entry<String, List<XyChartData.Point>> series : data.getSeriesPoints().entrySet())
		{
			ObjectNode seriesNode = seriesArray.addObject();
			seriesNode.put("name", series.getKey());
			seriesNode.put("type", settings.type().getEchartsSeriesType());
			if (settings.symbol() != null)
			{
				seriesNode.put("symbol", settings.symbol());
			}
			if (settings.showValues())
			{
				ObjectNode label = seriesNode.putObject("label");
				label.put("show", true);
				label.put("position", "top");
				// the y value; {@[1]} addresses the second value dimension
				label.put("formatter", "{@[1]}");
				label.put("color", "#333");
			}

			ArrayNode dataArray = seriesNode.putArray("data");
			for (XyChartData.Point point : series.getValue())
			{
				ObjectNode itemNode = dataArray.addObject();
				ArrayNode value = itemNode.putArray("value");
				addNumber(value, point.x());
				addNumber(value, point.y());
				if (hasSizes && point.size() != null)
				{
					itemNode.put("symbolSize", scaleSymbolSize(
						point.size().doubleValue(), minSize, maxSize));
				}
				if (point.label() != null)
				{
					ObjectNode labelNode = itemNode.putObject("label");
					labelNode.put("show", true);
					labelNode.put("formatter", point.label());
				}
			}
		}
	}

	private static void buildHierarchyOption(
		ObjectNode option, Settings settings, HierarchyChartData data)
	{
		option.putObject("tooltip").put("trigger", "item");

		List<HierarchyChartData.TreeNode> forest = data.buildForest();

		ArrayNode seriesArray = option.putArray("series");
		ObjectNode seriesNode = seriesArray.addObject();
		seriesNode.put("type", settings.type().getEchartsSeriesType());

		switch (settings.type())
		{
			case TREE:
			{
				// a tree series needs a single root; wrap forests
				ArrayNode dataArray = seriesNode.putArray("data");
				if (forest.size() == 1)
				{
					dataArray.add(toTreeNode(forest.get(0)));
				}
				else if (!forest.isEmpty())
				{
					ObjectNode root = dataArray.addObject();
					root.put("name", "");
					ArrayNode children = root.putArray("children");
					forest.forEach(node -> children.add(toTreeNode(node)));
				}
				seriesNode.put("symbolSize", 8);
				ObjectNode label = seriesNode.putObject("label");
				label.put("position", "left");
				label.put("verticalAlign", "middle");
				ObjectNode leaves = seriesNode.putObject("leaves");
				ObjectNode leavesLabel = leaves.putObject("label");
				leavesLabel.put("position", "right");
				leavesLabel.put("verticalAlign", "middle");
				break;
			}
			case TREEMAP:
			{
				ArrayNode dataArray = seriesNode.putArray("data");
				forest.forEach(node -> dataArray.add(toTreeNode(node)));
				// keep the treemap static and fully visible in paged output
				seriesNode.put("roam", false);
				seriesNode.put("nodeClick", false);
				ObjectNode breadcrumb = seriesNode.putObject("breadcrumb");
				breadcrumb.put("show", false);
				if (settings.showValues())
				{
					ObjectNode label = seriesNode.putObject("label");
					label.put("formatter", "{b}: {c}");
				}
				break;
			}
			case SUNBURST:
			{
				ArrayNode dataArray = seriesNode.putArray("data");
				forest.forEach(node -> dataArray.add(toTreeNode(node)));
				seriesNode.put("radius", "90%");
				if (settings.showValues())
				{
					ObjectNode label = seriesNode.putObject("label");
					label.put("formatter", "{b}: {c}");
				}
				break;
			}
			default:
				throw new JRRuntimeException(
					"Charteon: unsupported hierarchy chart type " + settings.type());
		}
	}

	private static ObjectNode toTreeNode(HierarchyChartData.TreeNode node)
	{
		ObjectNode nodeJson = MAPPER.createObjectNode();
		nodeJson.put("name", node.name());
		if (node.value() != null)
		{
			putNumber(nodeJson, "value", node.value());
		}
		if (!node.children().isEmpty())
		{
			ArrayNode children = nodeJson.putArray("children");
			node.children().forEach(child -> children.add(toTreeNode(child)));
		}
		return nodeJson;
	}

	private static void buildRelationOption(
		ObjectNode option, Settings settings, RelationChartData data)
	{
		option.putObject("tooltip").put("trigger", "item");

		ArrayNode seriesArray = option.putArray("series");
		ObjectNode seriesNode = seriesArray.addObject();
		seriesNode.put("type", settings.type().getEchartsSeriesType());

		switch (settings.type())
		{
			case SANKEY:
			{
				ArrayNode nodes = seriesNode.putArray("data");
				for (String name : data.getNodeWeights().keySet())
				{
					nodes.addObject().put("name", name);
				}
				ArrayNode links = seriesNode.putArray("links");
				for (RelationChartData.Edge edge : data.getEdges())
				{
					ObjectNode link = links.addObject();
					link.put("source", edge.source());
					link.put("target", edge.target());
					putNumber(link, "value", edge.value() == null ? 0 : edge.value());
				}
				ObjectNode lineStyle = seriesNode.putObject("lineStyle");
				lineStyle.put("color", "gradient");
				lineStyle.put("curveness", 0.5);
				ObjectNode emphasis = seriesNode.putObject("emphasis");
				emphasis.put("focus", "adjacency");
				if (settings.showValues())
				{
					ObjectNode label = seriesNode.putObject("label");
					label.put("show", true);
					label.put("formatter", "{b}: {c}");
				}
				break;
			}
			case GRAPH:
			{
				// circular is the default layout: deterministic, so PDF (SSR)
				// and HTML render identically; force is available but needs
				// animation frames to settle, which server-side rendering
				// does not run (documented in architecture.md)
				seriesNode.put("layout", settings.graphLayout());
				Map<String, Double> nodeWeights = data.getNodeWeights();
				double minWeight = Double.MAX_VALUE;
				double maxWeight = -Double.MAX_VALUE;
				for (Double weight : nodeWeights.values())
				{
					minWeight = Math.min(minWeight, weight);
					maxWeight = Math.max(maxWeight, weight);
				}
				ArrayNode nodes = seriesNode.putArray("data");
				for (Map.Entry<String, Double> node : nodeWeights.entrySet())
				{
					ObjectNode nodeJson = nodes.addObject();
					nodeJson.put("name", node.getKey());
					nodeJson.put("value", node.getValue());
					nodeJson.put("symbolSize",
						scaleSymbolSize(node.getValue(), minWeight, maxWeight));
				}
				ArrayNode links = seriesNode.putArray("links");
				for (RelationChartData.Edge edge : data.getEdges())
				{
					ObjectNode link = links.addObject();
					link.put("source", edge.source());
					link.put("target", edge.target());
					if (edge.value() != null)
					{
						putNumber(link, "value", edge.value());
					}
				}
				ObjectNode label = seriesNode.putObject("label");
				label.put("show", true);
				if (settings.showValues())
				{
					label.put("formatter", "{b}: {c}");
				}
				if ("force".equals(settings.graphLayout()))
				{
					ObjectNode force = seriesNode.putObject("force");
					force.put("layoutAnimation", false);
					force.put("repulsion", 100);
				}
				if ("circular".equals(settings.graphLayout()))
				{
					ObjectNode circular = seriesNode.putObject("circular");
					circular.put("rotateLabel", true);
				}
				break;
			}
			case LINES:
			{
				seriesNode.put("coordinateSystem", "cartesian2d");
				seriesNode.put("polyline", false);
				ObjectNode lineStyle = seriesNode.putObject("lineStyle");
				lineStyle.put("width", 2);
				lineStyle.put("curveness", 0.2);
				if (settings.showValues())
				{
					ObjectNode label = seriesNode.putObject("label");
					label.put("show", true);
					label.put("position", "end");
					label.put("formatter", "{c}");
				}
				double minX = Double.MAX_VALUE;
				double maxX = -Double.MAX_VALUE;
				double minY = Double.MAX_VALUE;
				double maxY = -Double.MAX_VALUE;
				ArrayNode dataArray = seriesNode.putArray("data");
				for (RelationChartData.Edge edge : data.getEdges())
				{
					ObjectNode item = dataArray.addObject();
					item.put("name", edge.source() + " > " + edge.target());
					ArrayNode coords = item.putArray("coords");
					ArrayNode from = coords.addArray();
					addNumber(from, edge.sourceX());
					addNumber(from, edge.sourceY());
					ArrayNode to = coords.addArray();
					addNumber(to, edge.targetX());
					addNumber(to, edge.targetY());
					if (edge.value() != null)
					{
						putNumber(item, "value", edge.value());
					}
					for (Number x : new Number[] {edge.sourceX(), edge.targetX()})
					{
						if (x != null)
						{
							minX = Math.min(minX, x.doubleValue());
							maxX = Math.max(maxX, x.doubleValue());
						}
					}
					for (Number y : new Number[] {edge.sourceY(), edge.targetY()})
					{
						if (y != null)
						{
							minY = Math.min(minY, y.doubleValue());
							maxY = Math.max(maxY, y.doubleValue());
						}
					}
				}
				// a lines series does not size the axes by itself
				ObjectNode xAxis = option.putObject("xAxis");
				xAxis.put("type", "value");
				ObjectNode yAxis = option.putObject("yAxis");
				yAxis.put("type", "value");
				if (minX <= maxX)
				{
					double padX = (maxX - minX) * 0.1 + 1;
					double padY = (maxY - minY) * 0.1 + 1;
					xAxis.put("min", Math.floor(minX - padX));
					xAxis.put("max", Math.ceil(maxX + padX));
					yAxis.put("min", Math.floor(minY - padY));
					yAxis.put("max", Math.ceil(maxY + padY));
				}
				break;
			}
			default:
				throw new JRRuntimeException(
					"Charteon: unsupported relation chart type " + settings.type());
		}
	}

	private static void buildBoxplotOption(
		ObjectNode option, Settings settings, BoxplotChartData data)
	{
		option.putObject("tooltip").put("trigger", "item");
		ObjectNode grid = option.putObject("grid");
		grid.put("containLabel", true);

		ArrayNode categoriesNode = MAPPER.createArrayNode();
		data.getBoxes().forEach(box -> categoriesNode.add(box.category()));
		boolean horizontal = settings.horizontal();
		ObjectNode categoryAxis = option.putObject(horizontal ? "yAxis" : "xAxis");
		categoryAxis.put("type", "category");
		categoryAxis.set("data", categoriesNode);
		ObjectNode valueAxis = option.putObject(horizontal ? "xAxis" : "yAxis");
		valueAxis.put("type", "value");
		valueAxis.put("scale", true);

		ArrayNode seriesArray = option.putArray("series");
		ObjectNode seriesNode = seriesArray.addObject();
		seriesNode.put("type", "boxplot");
		ArrayNode dataArray = seriesNode.putArray("data");
		for (BoxplotChartData.Box box : data.getBoxes())
		{
			ArrayNode values = dataArray.addArray();
			addNumber(values, box.min());
			addNumber(values, box.q1());
			addNumber(values, box.median());
			addNumber(values, box.q3());
			addNumber(values, box.max());
		}
	}

	private static void buildCandlestickOption(
		ObjectNode option, Settings settings, CandlestickChartData data)
	{
		option.putObject("tooltip").put("trigger", "axis");
		ObjectNode grid = option.putObject("grid");
		grid.put("containLabel", true);

		ArrayNode categoriesNode = MAPPER.createArrayNode();
		data.getCandles().forEach(candle -> categoriesNode.add(candle.category()));
		ObjectNode categoryAxis = option.putObject("xAxis");
		categoryAxis.put("type", "category");
		categoryAxis.set("data", categoriesNode);
		ObjectNode valueAxis = option.putObject("yAxis");
		valueAxis.put("type", "value");
		valueAxis.put("scale", true);

		ArrayNode seriesArray = option.putArray("series");
		ObjectNode seriesNode = seriesArray.addObject();
		seriesNode.put("type", "candlestick");
		ArrayNode dataArray = seriesNode.putArray("data");
		for (CandlestickChartData.Candle candle : data.getCandles())
		{
			// ECharts candlestick order: [open, close, lowest, highest]
			ArrayNode values = dataArray.addArray();
			addNumber(values, candle.open());
			addNumber(values, candle.close());
			addNumber(values, candle.low());
			addNumber(values, candle.high());
		}
	}

	/**
	 * Labels every data point with its value; stacked segments are labeled
	 * inside, plain bars/lines above (or to the right when horizontal).
	 */
	private static void addValueLabel(ObjectNode seriesNode, Settings settings)
	{
		ObjectNode label = seriesNode.putObject("label");
		label.put("show", true);
		label.put("position",
			settings.stacked() ? "inside" : (settings.horizontal() ? "right" : "top"));
		// outside labels inherit no usable color in SSR output; fix it
		label.put("color", settings.stacked() ? "#fff" : "#333");
		String formatter = numberFormatterJs(settings);
		if (formatter != null)
		{
			label.put("formatter", formatter);
		}
		// With many categories the value labels would collide into an unreadable
		// mess; hideOverlap drops colliding ones (all show when there is room,
		// thinned out automatically when tight). Not for stacked (inside) labels.
		if (!settings.stacked())
		{
			seriesNode.putObject("labelLayout").put("hideOverlap", true);
		}
	}

	/**
	 * The {@code valueFormat} pattern compiled into a revived ("js:") ECharts
	 * formatter callback, or {@code null} when no format is configured. The
	 * callback accepts either a raw value (axis label) or a params object with a
	 * {@code .value} (series label / tooltip), so it works in all three roles.
	 */
	private static String numberFormatterJs(Settings settings)
	{
		String expr = numberFormatterExpr(settings);
		return expr == null ? null : "js:" + expr;
	}

	/**
	 * The bare {@code function(x){…}} expression for {@link #numberFormatterJs},
	 * reusable where it must be invoked from another callback (e.g. pie labels).
	 */
	private static String numberFormatterExpr(Settings settings)
	{
		String pattern = settings.valueFormat();
		if (pattern == null)
		{
			return null;
		}
		int first = -1;
		int last = -1;
		for (int i = 0; i < pattern.length(); i++)
		{
			char c = pattern.charAt(i);
			if (c == '#' || c == '0')
			{
				if (first < 0)
				{
					first = i;
				}
				last = i;
			}
		}
		String prefix;
		String suffix;
		boolean grouping;
		int decimals = 0;
		if (first < 0)
		{
			prefix = "";
			suffix = pattern;
			grouping = false;
		}
		else
		{
			prefix = pattern.substring(0, first);
			String mask = pattern.substring(first, last + 1);
			suffix = pattern.substring(last + 1);
			grouping = mask.indexOf(',') >= 0;
			int dot = mask.indexOf('.');
			if (dot >= 0)
			{
				for (int i = dot + 1; i < mask.length(); i++)
				{
					char c = mask.charAt(i);
					if (c == '#' || c == '0')
					{
						decimals++;
					}
				}
			}
		}

		String gsep = jsEscape(settings.groupingSeparator());
		String dsep = jsEscape(settings.decimalSeparator());
		StringBuilder js = new StringBuilder();
		js.append("function(x){var n=(x&&typeof x==='object')?x.value:x;")
			.append("n=Number(n);if(!isFinite(n)){return x;}")
			.append("var g=n<0;n=Math.abs(n);var s=n.toFixed(").append(decimals).append(");")
			.append("var i=s.indexOf('.');var a=i<0?s:s.substring(0,i);var b=i<0?'':s.substring(i+1);");
		if (grouping)
		{
			js.append("a=a.replace(/\\B(?=(\\d{3})+(?!\\d))/g,'").append(gsep).append("');");
		}
		js.append("var o=a+(b?'").append(dsep).append("'+b:'');")
			.append("return '").append(jsEscape(prefix)).append("'+(g?'-':'')+o+'")
			.append(jsEscape(suffix)).append("';}");
		return js.toString();
	}

	/** Escapes a literal for embedding inside a single-quoted JS string. */
	private static String jsEscape(String s)
	{
		return s == null ? "" : s.replace("\\", "\\\\").replace("'", "\\'");
	}

	private static void addVisualMap(ObjectNode option, double min, double max)
	{
		ObjectNode visualMap = option.putObject("visualMap");
		if (min <= max)
		{
			visualMap.put("min", Math.floor(min));
			visualMap.put("max", Math.ceil(max));
		}
		else
		{
			visualMap.put("min", 0);
			visualMap.put("max", 1);
		}
		visualMap.put("calculable", false);
		visualMap.put("orient", "horizontal");
		visualMap.put("left", "center");
		visualMap.put("bottom", 0);
	}

	private static double scaleSymbolSize(double size, double min, double max)
	{
		final double minPixels = 8;
		final double maxPixels = 40;
		if (max <= min)
		{
			return (minPixels + maxPixels) / 2;
		}
		return minPixels + (size - min) / (max - min) * (maxPixels - minPixels);
	}

	private static Map.Entry<String, Map<String, Number>> firstSeries(CategoryChartData data)
	{
		var iterator = data.getSeriesValues().entrySet().iterator();
		return iterator.hasNext() ? iterator.next() : null;
	}

	private static void addDataItem(ArrayNode dataArray, Number value, String label)
	{
		if (label == null)
		{
			addNumber(dataArray, value);
		}
		else
		{
			ObjectNode itemNode = dataArray.addObject();
			putNumber(itemNode, "value", value);
			ObjectNode labelNode = itemNode.putObject("label");
			labelNode.put("show", true);
			labelNode.put("formatter", label);
		}
	}

	private static void addNumber(ArrayNode array, Number value)
	{
		if (value == null)
		{
			array.addNull();
		}
		else if (value instanceof Integer || value instanceof Long
			|| value instanceof Short || value instanceof Byte)
		{
			array.add(value.longValue());
		}
		else
		{
			array.add(value.doubleValue());
		}
	}

	private static void putNumber(ObjectNode node, String field, Number value)
	{
		if (value == null)
		{
			node.putNull(field);
		}
		else if (value instanceof Integer || value instanceof Long
			|| value instanceof Short || value instanceof Byte)
		{
			node.put(field, value.longValue());
		}
		else
		{
			node.put(field, value.doubleValue());
		}
	}

	/**
	 * Deep-merges {@code source} into {@code target}; object nodes are merged
	 * recursively, everything else (including arrays) is replaced. As a
	 * convenience, an object merged over an existing array of objects (e.g. a
	 * raw option {@code "series": {...}} over the generated series array) is
	 * merged into every element, so per-series options like {@code renderItem}
	 * can be set without replacing the generated data.
	 */
	static void deepMerge(ObjectNode target, ObjectNode source)
	{
		var fields = source.fields();
		while (fields.hasNext())
		{
			var entry = fields.next();
			JsonNode existing = target.get(entry.getKey());
			if (existing instanceof ObjectNode existingObject
				&& entry.getValue() instanceof ObjectNode sourceObject)
			{
				deepMerge(existingObject, sourceObject);
			}
			else if (existing instanceof ArrayNode existingArray
				&& entry.getValue() instanceof ObjectNode sourceObject)
			{
				for (JsonNode element : existingArray)
				{
					if (element instanceof ObjectNode elementObject)
					{
						deepMerge(elementObject, sourceObject);
					}
				}
			}
			else
			{
				target.set(entry.getKey(), entry.getValue());
			}
		}
	}
}
