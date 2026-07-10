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
package tech.charteon.component;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The chart types supported by the Charteon chart component: every core
 * Apache ECharts series type (ECharts-GL/3D excluded), each driven by the
 * dataset kind that matches its data structure.
 *
 * <p>
 * Presentation variants that ECharts itself models as options of a base
 * series (stacked, horizontal, area fill, smooth/step lines, doughnut/rose
 * pies, polar coordinates, bubble sizing) are component properties
 * ({@code stacked}, {@code horizontal}, {@code filled}, ...) rather than
 * separate types. The pre-v2 variant type names ({@code stackedBar},
 * {@code horizontalBar}, {@code area}, {@code doughnut}, {@code bubble})
 * remain supported as aliases of the base type plus the implied property.
 */
public enum ChartTypeEnum
{
	LINE("line", "line", DatasetKind.CATEGORY),
	BAR("bar", "bar", DatasetKind.CATEGORY),
	PIE("pie", "pie", DatasetKind.CATEGORY),
	SCATTER("scatter", "scatter", DatasetKind.XY),
	EFFECT_SCATTER("effectScatter", "effectScatter", DatasetKind.XY),
	RADAR("radar", "radar", DatasetKind.CATEGORY),
	TREE("tree", "tree", DatasetKind.HIERARCHY),
	TREEMAP("treemap", "treemap", DatasetKind.HIERARCHY),
	SUNBURST("sunburst", "sunburst", DatasetKind.HIERARCHY),
	BOXPLOT("boxplot", "boxplot", DatasetKind.BOXPLOT),
	CANDLESTICK("candlestick", "candlestick", DatasetKind.CANDLESTICK),
	HEATMAP("heatmap", "heatmap", DatasetKind.CATEGORY),
	MAP("map", "map", DatasetKind.CATEGORY),
	PARALLEL("parallel", "parallel", DatasetKind.CATEGORY),
	LINES("lines", "lines", DatasetKind.RELATION),
	GRAPH("graph", "graph", DatasetKind.RELATION),
	SANKEY("sankey", "sankey", DatasetKind.RELATION),
	FUNNEL("funnel", "funnel", DatasetKind.CATEGORY),
	GAUGE("gauge", "gauge", DatasetKind.CATEGORY),
	PICTORIAL_BAR("pictorialBar", "pictorialBar", DatasetKind.CATEGORY),
	THEME_RIVER("themeRiver", "themeRiver", DatasetKind.CATEGORY),
	/**
	 * The ECharts custom series; the data comes from an {@code xyDataset},
	 * the mandatory {@code renderItem} callback is supplied through the
	 * {@code optionExpression} escape hatch as a {@code "js:function..."}
	 * string (see the function revival notes in the documentation).
	 */
	CUSTOM("custom", "custom", DatasetKind.XY),

	// pre-v2 variant aliases, kept for backward compatibility
	STACKED_BAR("stackedBar", "bar", DatasetKind.CATEGORY),
	HORIZONTAL_BAR("horizontalBar", "bar", DatasetKind.CATEGORY),
	AREA("area", "line", DatasetKind.CATEGORY),
	DOUGHNUT("doughnut", "pie", DatasetKind.CATEGORY),
	BUBBLE("bubble", "scatter", DatasetKind.XY);

	/**
	 * The kind of dataset a chart type is driven by.
	 */
	public enum DatasetKind
	{
		/** driven by {@code categoryDataset} (series/category/value expressions) */
		CATEGORY,
		/** driven by {@code xyDataset} (series/x/y/size expressions) */
		XY,
		/** driven by {@code hierarchyDataset} (name/parent/value expressions) */
		HIERARCHY,
		/** driven by {@code relationDataset} (source/target/value expressions) */
		RELATION,
		/** driven by {@code boxplotDataset} (category + five-number summary) */
		BOXPLOT,
		/** driven by {@code candlestickDataset} (category + OHLC expressions) */
		CANDLESTICK
	}

	private final String name;
	private final String echartsSeriesType;
	private final DatasetKind datasetKind;

	ChartTypeEnum(String name, String echartsSeriesType, DatasetKind datasetKind)
	{
		this.name = name;
		this.echartsSeriesType = echartsSeriesType;
		this.datasetKind = datasetKind;
	}

	@JsonValue
	public String getName()
	{
		return name;
	}

	/**
	 * The value used for {@code series[].type} in the generated ECharts option.
	 */
	public String getEchartsSeriesType()
	{
		return echartsSeriesType;
	}

	public DatasetKind getDatasetKind()
	{
		return datasetKind;
	}

	/**
	 * Resolves the pre-v2 variant aliases to their base type; core types
	 * resolve to themselves.
	 */
	public ChartTypeEnum getBaseType()
	{
		switch (this)
		{
			case STACKED_BAR:
			case HORIZONTAL_BAR:
				return BAR;
			case AREA:
				return LINE;
			case DOUGHNUT:
				return PIE;
			case BUBBLE:
				return SCATTER;
			default:
				return this;
		}
	}

	@JsonCreator
	public static ChartTypeEnum getByName(String name)
	{
		if (name != null)
		{
			for (ChartTypeEnum value : values())
			{
				if (value.name.equals(name))
				{
					return value;
				}
			}
		}
		return null;
	}
}
