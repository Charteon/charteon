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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import net.sf.jasperreports.engine.JRConstants;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRExpressionCollector;
import net.sf.jasperreports.engine.base.JRBaseObjectFactory;
import net.sf.jasperreports.engine.design.JRDesignElementDataset;
import net.sf.jasperreports.engine.util.JRCloneUtils;

/**
 * A relation dataset for sankey, graph and lines charts: each record
 * contributes one source&rarr;target edge with an optional weight. For the
 * {@code lines} type, the optional coordinate expressions provide the start
 * and end points of each connection.
 */
@JsonPropertyOrder({
	"sourceExpression",
	"targetExpression",
	"valueExpression",
	"sourceXExpression",
	"sourceYExpression",
	"targetXExpression",
	"targetYExpression"
	})
public class ChartRelationDataset extends JRDesignElementDataset
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private JRExpression sourceExpression;
	private JRExpression targetExpression;
	private JRExpression valueExpression;
	private JRExpression sourceXExpression;
	private JRExpression sourceYExpression;
	private JRExpression targetXExpression;
	private JRExpression targetYExpression;

	public ChartRelationDataset()
	{
	}

	public ChartRelationDataset(ChartRelationDataset dataset, JRBaseObjectFactory factory)
	{
		super(dataset, factory);

		this.sourceExpression = factory.getExpression(dataset.getSourceExpression());
		this.targetExpression = factory.getExpression(dataset.getTargetExpression());
		this.valueExpression = factory.getExpression(dataset.getValueExpression());
		this.sourceXExpression = factory.getExpression(dataset.getSourceXExpression());
		this.sourceYExpression = factory.getExpression(dataset.getSourceYExpression());
		this.targetXExpression = factory.getExpression(dataset.getTargetXExpression());
		this.targetYExpression = factory.getExpression(dataset.getTargetYExpression());
	}

	/**
	 * The expression providing the edge source node name.
	 */
	public JRExpression getSourceExpression()
	{
		return sourceExpression;
	}

	public void setSourceExpression(JRExpression sourceExpression)
	{
		this.sourceExpression = sourceExpression;
	}

	/**
	 * The expression providing the edge target node name.
	 */
	public JRExpression getTargetExpression()
	{
		return targetExpression;
	}

	public void setTargetExpression(JRExpression targetExpression)
	{
		this.targetExpression = targetExpression;
	}

	/**
	 * The expression providing the {@code java.lang.Number} weight of the edge.
	 */
	public JRExpression getValueExpression()
	{
		return valueExpression;
	}

	public void setValueExpression(JRExpression valueExpression)
	{
		this.valueExpression = valueExpression;
	}

	/**
	 * For {@code lines} charts: the x coordinate of the connection start.
	 */
	@JsonProperty("sourceXExpression")
	public JRExpression getSourceXExpression()
	{
		return sourceXExpression;
	}

	@JsonProperty("sourceXExpression")
	public void setSourceXExpression(JRExpression sourceXExpression)
	{
		this.sourceXExpression = sourceXExpression;
	}

	/**
	 * For {@code lines} charts: the y coordinate of the connection start.
	 */
	@JsonProperty("sourceYExpression")
	public JRExpression getSourceYExpression()
	{
		return sourceYExpression;
	}

	@JsonProperty("sourceYExpression")
	public void setSourceYExpression(JRExpression sourceYExpression)
	{
		this.sourceYExpression = sourceYExpression;
	}

	/**
	 * For {@code lines} charts: the x coordinate of the connection end.
	 */
	@JsonProperty("targetXExpression")
	public JRExpression getTargetXExpression()
	{
		return targetXExpression;
	}

	@JsonProperty("targetXExpression")
	public void setTargetXExpression(JRExpression targetXExpression)
	{
		this.targetXExpression = targetXExpression;
	}

	/**
	 * For {@code lines} charts: the y coordinate of the connection end.
	 */
	@JsonProperty("targetYExpression")
	public JRExpression getTargetYExpression()
	{
		return targetYExpression;
	}

	@JsonProperty("targetYExpression")
	public void setTargetYExpression(JRExpression targetYExpression)
	{
		this.targetYExpression = targetYExpression;
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		ChartComponentCompiler.collectExpressions(this, collector);
	}

	@Override
	public Object clone()
	{
		ChartRelationDataset clone = (ChartRelationDataset) super.clone();
		clone.sourceExpression = JRCloneUtils.nullSafeClone(sourceExpression);
		clone.targetExpression = JRCloneUtils.nullSafeClone(targetExpression);
		clone.valueExpression = JRCloneUtils.nullSafeClone(valueExpression);
		clone.sourceXExpression = JRCloneUtils.nullSafeClone(sourceXExpression);
		clone.sourceYExpression = JRCloneUtils.nullSafeClone(sourceYExpression);
		clone.targetXExpression = JRCloneUtils.nullSafeClone(targetXExpression);
		clone.targetYExpression = JRCloneUtils.nullSafeClone(targetYExpression);
		return clone;
	}
}
