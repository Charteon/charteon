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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.sf.jasperreports.engine.util.JRExpressionUtil;

import tech.charteon.component.ChartComponent;
import tech.charteon.component.ChartTypeEnum;
import tech.charteon.export.ssr.EChartsSvgRenderer;

/**
 * Design-time chart preview: builds an ECharts option for a component using
 * representative sample data (design time has no report data) and renders it
 * to an SVG through the GraalJS pipeline. Used by
 * {@link tech.charteon.component.ChartDesignConverter} so report designers
 * (e.g. Jaspersoft Studio) see a real chart in the canvas rather than a
 * placeholder box.
 *
 * <p>Rendering is cached per component state and size, so canvas redraws do
 * not re-render. Any failure (most prominently: GraalJS needs a JVM &ge; 21,
 * while some design tools bundle an older JRE) returns {@code null} and the
 * caller falls back to a text placeholder — a broken preview must never break
 * the designer.</p>
 */
public final class ChartSamplePreview
{
	private static final Log log = LogFactory.getLog(ChartSamplePreview.class);

	private static final int CACHE_MAX_ENTRIES = 200;

	/** LRU cache: preview key (component state + size) → SVG bytes. */
	private static final Map<String, byte[]> CACHE = Collections.synchronizedMap(
		new LinkedHashMap<>(64, 0.75f, true)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest)
			{
				return size() > CACHE_MAX_ENTRIES;
			}
		});

	/** Sticky flag: once SSR proved unavailable (old JVM), stop retrying. */
	private static volatile boolean ssrUnavailable;

	/** Ensures the first (and only the first) rendering failure is logged loudly. */
	private static volatile boolean failureLogged;

	private ChartSamplePreview()
	{
	}

	/**
	 * Returns the preview SVG for the component at the given size, or
	 * {@code null} if it cannot be rendered on this JVM.
	 */
	public static byte[] renderPreviewSvg(ChartComponent component, int width, int height)
	{
		if (ssrUnavailable || component == null || width <= 0 || height <= 0)
		{
			return null;
		}
		try
		{
			String option = buildSampleOption(component);
			String key = width + "x" + height + "|" + component.getTheme() + "|" + option;
			byte[] cached = CACHE.get(key);
			if (cached != null)
			{
				return cached;
			}
			String svg = EChartsSvgRenderer.renderSvg(
				option, width, height, component.getTheme(), component.getMapName());
			if (svg == null)
			{
				return null;
			}
			byte[] bytes = svg.getBytes(StandardCharsets.UTF_8);
			CACHE.put(key, bytes);
			return bytes;
		}
		catch (UnsupportedClassVersionError | NoClassDefFoundError e)
		{
			// GraalJS requires a newer JVM than the one the host runs on;
			// remember and stop trying
			ssrUnavailable = true;
			log.warn("Charteon: design preview disabled — the chart renderer needs a "
				+ "JVM >= 21, but this process runs on an older one (" + e + ")");
			return null;
		}
		catch (Throwable t)
		{
			// Log the real cause once at WARN so it reaches the host's error log
			// (e.g. Jaspersoft Studio's Error Log view) instead of only debug —
			// an empty canvas box otherwise hides why the chart did not render.
			if (!failureLogged)
			{
				failureLogged = true;
				log.warn("Charteon: design preview rendering failed", t);
			}
			else
			{
				log.debug("Charteon: design preview rendering failed", t);
			}
			return null;
		}
	}

	/**
	 * Builds the option JSON with sample data matching the chart type; the
	 * design-time title falls back to the title expression text if it is a
	 * simple string literal.
	 */
	public static String buildSampleOption(ChartComponent component)
	{
		ChartTypeEnum type = component.getChartType();
		ChartData data = sampleData(type);
		// reflect combo settings (per-series type / secondary axis) onto the
		// sample so the design canvas previews a combo chart rather than generic
		// grouped bars
		if (data.category() != null && component.getCategoryDataset() != null)
		{
			applyComboStyles(data.category(), component.getCategoryDataset());
		}
		String title = literalText(JRExpressionUtil.getExpressionText(component.getTitleExpression()));
		String subtitle = literalText(JRExpressionUtil.getExpressionText(component.getSubtitleExpression()));
		// The preview is rendered small (wizard thumbnails ~300x200); ECharts'
		// default fonts overlap and some default label colors are white (thus
		// invisible on the light canvas). A cosmetic styling layer is merged over
		// the generated option — for the design preview only, never the report
		// output.
		String base = EChartsOptionBuilder.buildOption(component, title, subtitle, data, null);
		return applyPreviewStyling(base);
	}

	/**
	 * Deep-merges the design-preview cosmetic overlay over the generated option:
	 * smaller fonts, a thinner/lighter gauge, and dark labels with a white halo
	 * so they stay legible on any background (fixes the white
	 * funnel/themeRiver/tree/sankey/graph labels). Axis styling is only applied
	 * when the option actually has axes, so non-cartesian types (gauge, pie,
	 * funnel, ...) do not gain spurious axis lines. Styling is purely cosmetic:
	 * any failure returns the unstyled option rather than breaking the preview.
	 */
	private static String applyPreviewStyling(String baseOption)
	{
		try
		{
			ObjectNode option = (ObjectNode) MAPPER.readTree(baseOption);
			EChartsOptionBuilder.deepMerge(option, (ObjectNode) MAPPER.readTree(PREVIEW_COMMON));
			if (option.has("xAxis"))
			{
				EChartsOptionBuilder.deepMerge(option, axisWrap("xAxis"));
			}
			if (option.has("yAxis"))
			{
				EChartsOptionBuilder.deepMerge(option, axisWrap("yAxis"));
			}
			return MAPPER.writeValueAsString(option);
		}
		catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e)
		{
			return baseOption;
		}
	}

	private static ObjectNode axisWrap(String axisKey) throws com.fasterxml.jackson.core.JsonProcessingException
	{
		ObjectNode wrap = MAPPER.createObjectNode();
		wrap.set(axisKey, MAPPER.readTree(PREVIEW_AXIS));
		return wrap;
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** Type-agnostic styling: fonts, legend, and per-series labels/gauge. */
	private static final String PREVIEW_COMMON =
		"{"
		+ "\"textStyle\":{\"fontSize\":11},"
		+ "\"title\":{\"textStyle\":{\"fontSize\":13}},"
		+ "\"legend\":{\"textStyle\":{\"fontSize\":10},\"itemHeight\":10,\"itemWidth\":16},"
		+ "\"series\":{"
		// dark labels (no halo: a text-border stroke is painted over the fill by
		// Batik in server-side rendering, which would swallow the small glyphs)
		+ "\"label\":{\"fontSize\":9,\"color\":\"#333\"},"
		+ "\"detail\":{\"fontSize\":15,\"fontWeight\":\"normal\"},"
		+ "\"title\":{\"fontSize\":10},"
		+ "\"axisLabel\":{\"fontSize\":8,\"distance\":12,\"color\":\"#555\"},"
		+ "\"axisLine\":{\"lineStyle\":{\"width\":8}},"
		+ "\"splitLine\":{\"length\":8,\"distance\":6},"
		+ "\"pointer\":{\"width\":4}"
		+ "}"
		+ "}";

	/** Cartesian-axis styling, applied only when the option has that axis. */
	private static final String PREVIEW_AXIS =
		"{\"axisLabel\":{\"fontSize\":9},\"splitNumber\":3,\"nameTextStyle\":{\"fontSize\":9}}";

	private static String literalText(String expressionText)
	{
		if (expressionText == null)
		{
			return null;
		}
		String trimmed = expressionText.trim();
		if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")
			&& trimmed.indexOf('"', 1) == trimmed.length() - 1)
		{
			return trimmed.substring(1, trimmed.length() - 1);
		}
		return null;
	}

	/**
	 * Maps the component's per-series combo settings (seriesType / secondaryAxis)
	 * onto the generic sample series by index, so a bar+line / dual-axis design is
	 * visible in the design canvas preview.
	 */
	private static void applyComboStyles(CategoryChartData sample,
		tech.charteon.component.ChartCategoryDataset dataset)
	{
		java.util.List<String> names = java.util.List.copyOf(sample.getSeriesValues().keySet());
		java.util.List<tech.charteon.component.CategorySeries> defs = dataset.getSeriesList();
		for (int i = 0; i < names.size() && i < defs.size(); i++)
		{
			tech.charteon.component.CategorySeries def = defs.get(i);
			boolean secondary = Boolean.TRUE.equals(def.getSecondaryAxis());
			if (def.getSeriesType() != null || secondary || def.getColor() != null)
			{
				sample.setStyle(names.get(i), def.getSeriesType(), secondary, def.getColor());
			}
		}
	}

	private static ChartData sampleData(ChartTypeEnum type)
	{
		if (type == null)
		{
			return sampleCategory();
		}
		// a gauge draws one needle + one big center number per value, so the
		// generic 2x3 category sample would stack six overlapping needles/numbers
		if (type == ChartTypeEnum.GAUGE)
		{
			return sampleGauge();
		}
		switch (type.getDatasetKind())
		{
			case XY:
				return sampleXy();
			case HIERARCHY:
				return sampleHierarchy();
			case RELATION:
				return sampleRelation();
			case BOXPLOT:
				return sampleBoxplot();
			case CANDLESTICK:
				return sampleCandlestick();
			case CATEGORY:
			default:
				return sampleCategory();
		}
	}

	private static ChartData sampleCategory()
	{
		CategoryChartData data = new CategoryChartData();
		data.addValue("Series A", "Jan", 120, null);
		data.addValue("Series A", "Feb", 145, null);
		data.addValue("Series A", "Mar", 132, null);
		data.addValue("Series B", "Jan", 85, null);
		data.addValue("Series B", "Feb", 74, null);
		data.addValue("Series B", "Mar", 110, null);
		return new ChartData(data, null, null, null, null, null);
	}

	private static ChartData sampleGauge()
	{
		CategoryChartData data = new CategoryChartData();
		data.addValue("Score", "Utilization", 72, null);
		return new ChartData(data, null, null, null, null, null);
	}

	private static ChartData sampleXy()
	{
		XyChartData data = new XyChartData();
		data.addPoint("Cluster 1", 2.1, 3.4, 8, null);
		data.addPoint("Cluster 1", 3.0, 2.8, 14, null);
		data.addPoint("Cluster 1", 3.8, 4.1, 10, null);
		data.addPoint("Cluster 2", 6.2, 6.0, 18, null);
		data.addPoint("Cluster 2", 7.1, 5.4, 9, null);
		data.addPoint("Cluster 2", 6.8, 7.2, 12, null);
		return new ChartData(null, data, null, null, null, null);
	}

	private static ChartData sampleHierarchy()
	{
		HierarchyChartData data = new HierarchyChartData();
		data.addNode("Products", null, null);
		data.addNode("Coffee", "Products", 62);
		data.addNode("Tea", "Products", 38);
		data.addNode("Espresso", "Coffee", 34);
		data.addNode("Latte", "Coffee", 28);
		return new ChartData(null, null, data, null, null, null);
	}

	private static ChartData sampleRelation()
	{
		RelationChartData data = new RelationChartData();
		data.addEdge("Visit", "Cart", 120, 0.0, 0.0, 4.0, 2.0);
		data.addEdge("Cart", "Checkout", 72, 4.0, 2.0, 8.0, 1.0);
		data.addEdge("Checkout", "Purchase", 51, 8.0, 1.0, 12.0, 3.0);
		return new ChartData(null, null, null, data, null, null);
	}

	private static ChartData sampleBoxplot()
	{
		BoxplotChartData data = new BoxplotChartData();
		data.addBox("Mon", 12, 25, 34, 46, 60);
		data.addBox("Tue", 15, 28, 39, 51, 68);
		data.addBox("Wed", 10, 22, 31, 42, 55);
		return new ChartData(null, null, null, null, data, null);
	}

	private static ChartData sampleCandlestick()
	{
		CandlestickChartData data = new CandlestickChartData();
		data.addCandle("D1", 20, 34, 18, 38);
		data.addCandle("D2", 34, 30, 26, 40);
		data.addCandle("D3", 30, 42, 28, 46);
		return new ChartData(null, null, null, null, null, data);
	}
}
