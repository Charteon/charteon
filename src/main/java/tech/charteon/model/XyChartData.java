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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The evaluated data of an x/y dataset: points per series in encounter order.
 */
public class XyChartData implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * A single x/y data point with optional size and label.
	 */
	public record Point(Number x, Number y, Number size, String label) implements Serializable
	{
	}

	private final Map<String, List<Point>> seriesPoints = new LinkedHashMap<>();

	public void addPoint(String series, Number x, Number y, Number size, String label)
	{
		seriesPoints.computeIfAbsent(series, k -> new ArrayList<>()).add(new Point(x, y, size, label));
	}

	public boolean isEmpty()
	{
		return seriesPoints.isEmpty();
	}

	public Map<String, List<Point>> getSeriesPoints()
	{
		return Collections.unmodifiableMap(seriesPoints);
	}
}
