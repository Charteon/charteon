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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The evaluated data of a relation dataset: source&rarr;target edges in
 * encounter order, with optional weights and (for {@code lines} charts)
 * optional start/end coordinates.
 */
public class RelationChartData implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * One edge; the coordinate fields are only set for {@code lines} charts.
	 */
	public record Edge(String source, String target, Number value,
		Number sourceX, Number sourceY, Number targetX, Number targetY) implements Serializable
	{
	}

	private final List<Edge> edges = new ArrayList<>();

	public void addEdge(String source, String target, Number value,
		Number sourceX, Number sourceY, Number targetX, Number targetY)
	{
		edges.add(new Edge(source, target, value, sourceX, sourceY, targetX, targetY));
	}

	public boolean isEmpty()
	{
		return edges.isEmpty();
	}

	public List<Edge> getEdges()
	{
		return edges;
	}

	/**
	 * The distinct node names (sources first, then targets) with the sum of
	 * the weights of their incident edges, in encounter order. Used to derive
	 * the node list for graph charts.
	 */
	public Map<String, Double> getNodeWeights()
	{
		Map<String, Double> weights = new LinkedHashMap<>();
		for (Edge edge : edges)
		{
			double value = edge.value() == null ? 0 : edge.value().doubleValue();
			weights.merge(edge.source(), value, Double::sum);
			weights.merge(edge.target(), value, Double::sum);
		}
		return weights;
	}
}
