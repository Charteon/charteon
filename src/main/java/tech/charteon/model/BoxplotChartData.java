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
import java.util.List;

/**
 * The evaluated data of a boxplot dataset: one five-number summary per
 * category, in encounter order.
 */
public class BoxplotChartData implements Serializable
{
	private static final long serialVersionUID = 1L;

	public record Box(String category, Number min, Number q1, Number median,
		Number q3, Number max) implements Serializable
	{
	}

	private final List<Box> boxes = new ArrayList<>();

	public void addBox(String category, Number min, Number q1, Number median, Number q3, Number max)
	{
		boxes.add(new Box(category, min, q1, median, q3, max));
	}

	public boolean isEmpty()
	{
		return boxes.isEmpty();
	}

	public List<Box> getBoxes()
	{
		return boxes;
	}
}
