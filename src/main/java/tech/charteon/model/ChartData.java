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

/**
 * The evaluated data of all datasets a chart component may declare (at most
 * one of them is non-null in a valid component).
 */
public record ChartData(
	CategoryChartData category,
	XyChartData xy,
	HierarchyChartData hierarchy,
	RelationChartData relation,
	BoxplotChartData boxplot,
	CandlestickChartData candlestick)
{
	public static final ChartData EMPTY = new ChartData(null, null, null, null, null, null);
}
