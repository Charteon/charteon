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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import net.sf.jasperreports.engine.JRConstants;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRExpressionCollector;
import net.sf.jasperreports.engine.base.JRBaseObjectFactory;
import net.sf.jasperreports.engine.design.JRDesignElementDataset;
import net.sf.jasperreports.engine.util.JRCloneUtils;

/**
 * A hierarchy dataset for tree, treemap and sunburst charts: each record
 * contributes one node identified by its name, attached to its parent by the
 * parent name ({@code null} or unknown parent = root node).
 */
@JsonPropertyOrder({
	"nameExpression",
	"parentExpression",
	"valueExpression"
	})
public class ChartHierarchyDataset extends JRDesignElementDataset
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private JRExpression nameExpression;
	private JRExpression parentExpression;
	private JRExpression valueExpression;

	public ChartHierarchyDataset()
	{
	}

	public ChartHierarchyDataset(ChartHierarchyDataset dataset, JRBaseObjectFactory factory)
	{
		super(dataset, factory);

		this.nameExpression = factory.getExpression(dataset.getNameExpression());
		this.parentExpression = factory.getExpression(dataset.getParentExpression());
		this.valueExpression = factory.getExpression(dataset.getValueExpression());
	}

	/**
	 * The expression providing the node name (unique within the hierarchy).
	 */
	public JRExpression getNameExpression()
	{
		return nameExpression;
	}

	public void setNameExpression(JRExpression nameExpression)
	{
		this.nameExpression = nameExpression;
	}

	/**
	 * The expression providing the parent node name; records evaluating to
	 * {@code null} (or to a name that never occurs as a node) become root
	 * nodes.
	 */
	public JRExpression getParentExpression()
	{
		return parentExpression;
	}

	public void setParentExpression(JRExpression parentExpression)
	{
		this.parentExpression = parentExpression;
	}

	/**
	 * The expression providing the {@code java.lang.Number} value of the node;
	 * may be {@code null} for pure grouping nodes whose value is the sum of
	 * their children.
	 */
	public JRExpression getValueExpression()
	{
		return valueExpression;
	}

	public void setValueExpression(JRExpression valueExpression)
	{
		this.valueExpression = valueExpression;
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		ChartComponentCompiler.collectExpressions(this, collector);
	}

	@Override
	public Object clone()
	{
		ChartHierarchyDataset clone = (ChartHierarchyDataset) super.clone();
		clone.nameExpression = JRCloneUtils.nullSafeClone(nameExpression);
		clone.parentExpression = JRCloneUtils.nullSafeClone(parentExpression);
		clone.valueExpression = JRCloneUtils.nullSafeClone(valueExpression);
		return clone;
	}
}
