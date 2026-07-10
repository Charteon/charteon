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
 * The evaluated data of a hierarchy dataset: a flat list of
 * (name, parent, value) nodes in encounter order, plus the logic to assemble
 * them into the tree structure ECharts expects.
 */
public class HierarchyChartData implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * One node of the hierarchy; {@code parent} is {@code null} (or a name
	 * that never occurs as a node) for root nodes.
	 */
	public record Node(String name, String parent, Number value) implements Serializable
	{
	}

	private final List<Node> nodes = new ArrayList<>();

	public void addNode(String name, String parent, Number value)
	{
		nodes.add(new Node(name, parent, value));
	}

	public boolean isEmpty()
	{
		return nodes.isEmpty();
	}

	public List<Node> getNodes()
	{
		return nodes;
	}

	/**
	 * Assembles the flat node list into trees: returns the root nodes (nodes
	 * without a parent or whose parent never occurs as a node name) with
	 * their children linked, in encounter order.
	 */
	public List<TreeNode> buildForest()
	{
		Map<String, TreeNode> byName = new LinkedHashMap<>();
		for (Node node : nodes)
		{
			byName.put(node.name(), new TreeNode(node.name(), node.value()));
		}

		List<TreeNode> roots = new ArrayList<>();
		for (Node node : nodes)
		{
			TreeNode treeNode = byName.get(node.name());
			TreeNode parent = node.parent() == null ? null : byName.get(node.parent());
			if (parent == null || parent == treeNode)
			{
				roots.add(treeNode);
			}
			else
			{
				parent.children().add(treeNode);
			}
		}
		return roots;
	}

	/**
	 * A linked node of the assembled hierarchy.
	 */
	public record TreeNode(String name, Number value, List<TreeNode> children) implements Serializable
	{
		public TreeNode(String name, Number value)
		{
			this(name, value, new ArrayList<>());
		}
	}
}
