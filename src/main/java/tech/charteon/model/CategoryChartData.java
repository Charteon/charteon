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

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The evaluated data of a category dataset: values keyed by series name and
 * category, both kept in encounter order.
 */
public class CategoryChartData implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final Set<String> categories = new LinkedHashSet<>();
	private final Map<String, Map<String, Number>> seriesValues = new LinkedHashMap<>();
	private final Map<String, Map<String, String>> seriesLabels = new LinkedHashMap<>();
	private final Map<String, SeriesStyle> seriesStyles = new LinkedHashMap<>();

	/**
	 * Optional per-series presentation for combo charts: an overriding chart
	 * type ({@code null} = the chart's base type), whether the series is plotted
	 * against the secondary value axis, and a fixed series color ({@code null} =
	 * palette).
	 */
	public record SeriesStyle(String type, boolean secondaryAxis, String color)
	{
	}

	public void addValue(String series, String category, Number value, String label)
	{
		categories.add(category);
		seriesValues.computeIfAbsent(series, k -> new LinkedHashMap<>()).put(category, value);
		if (label != null)
		{
			seriesLabels.computeIfAbsent(series, k -> new LinkedHashMap<>()).put(category, label);
		}
	}

	/** Registers the combo-chart presentation of a series (by name). */
	public void setStyle(String series, String type, boolean secondaryAxis, String color)
	{
		if (type != null || secondaryAxis || color != null)
		{
			seriesStyles.put(series, new SeriesStyle(type, secondaryAxis, color));
		}
	}

	/** The registered style of a series, or {@code null} for the defaults. */
	public SeriesStyle getStyle(String series)
	{
		return seriesStyles.get(series);
	}

	/** Whether any series requested the secondary value axis. */
	public boolean hasSecondaryAxis()
	{
		return seriesStyles.values().stream().anyMatch(SeriesStyle::secondaryAxis);
	}

	public boolean isEmpty()
	{
		return seriesValues.isEmpty();
	}

	/**
	 * All categories in encounter order.
	 */
	public Set<String> getCategories()
	{
		return Collections.unmodifiableSet(categories);
	}

	/**
	 * Values per series (in encounter order), keyed by category.
	 */
	public Map<String, Map<String, Number>> getSeriesValues()
	{
		return Collections.unmodifiableMap(seriesValues);
	}

	public String getLabel(String series, String category)
	{
		Map<String, String> labels = seriesLabels.get(series);
		return labels == null ? null : labels.get(category);
	}
}
