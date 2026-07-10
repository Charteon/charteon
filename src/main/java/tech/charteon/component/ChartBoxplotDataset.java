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
 * A boxplot dataset: each record contributes the five-number summary
 * (minimum, first quartile, median, third quartile, maximum) of one category.
 */
@JsonPropertyOrder({
	"categoryExpression",
	"minExpression",
	"q1Expression",
	"medianExpression",
	"q3Expression",
	"maxExpression"
	})
public class ChartBoxplotDataset extends JRDesignElementDataset
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private JRExpression categoryExpression;
	private JRExpression minExpression;
	private JRExpression q1Expression;
	private JRExpression medianExpression;
	private JRExpression q3Expression;
	private JRExpression maxExpression;

	public ChartBoxplotDataset()
	{
	}

	public ChartBoxplotDataset(ChartBoxplotDataset dataset, JRBaseObjectFactory factory)
	{
		super(dataset, factory);

		this.categoryExpression = factory.getExpression(dataset.getCategoryExpression());
		this.minExpression = factory.getExpression(dataset.getMinExpression());
		this.q1Expression = factory.getExpression(dataset.getQ1Expression());
		this.medianExpression = factory.getExpression(dataset.getMedianExpression());
		this.q3Expression = factory.getExpression(dataset.getQ3Expression());
		this.maxExpression = factory.getExpression(dataset.getMaxExpression());
	}

	public JRExpression getCategoryExpression()
	{
		return categoryExpression;
	}

	public void setCategoryExpression(JRExpression categoryExpression)
	{
		this.categoryExpression = categoryExpression;
	}

	public JRExpression getMinExpression()
	{
		return minExpression;
	}

	public void setMinExpression(JRExpression minExpression)
	{
		this.minExpression = minExpression;
	}

	public JRExpression getQ1Expression()
	{
		return q1Expression;
	}

	public void setQ1Expression(JRExpression q1Expression)
	{
		this.q1Expression = q1Expression;
	}

	public JRExpression getMedianExpression()
	{
		return medianExpression;
	}

	public void setMedianExpression(JRExpression medianExpression)
	{
		this.medianExpression = medianExpression;
	}

	public JRExpression getQ3Expression()
	{
		return q3Expression;
	}

	public void setQ3Expression(JRExpression q3Expression)
	{
		this.q3Expression = q3Expression;
	}

	public JRExpression getMaxExpression()
	{
		return maxExpression;
	}

	public void setMaxExpression(JRExpression maxExpression)
	{
		this.maxExpression = maxExpression;
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		ChartComponentCompiler.collectExpressions(this, collector);
	}

	@Override
	public Object clone()
	{
		ChartBoxplotDataset clone = (ChartBoxplotDataset) super.clone();
		clone.categoryExpression = JRCloneUtils.nullSafeClone(categoryExpression);
		clone.minExpression = JRCloneUtils.nullSafeClone(minExpression);
		clone.q1Expression = JRCloneUtils.nullSafeClone(q1Expression);
		clone.medianExpression = JRCloneUtils.nullSafeClone(medianExpression);
		clone.q3Expression = JRCloneUtils.nullSafeClone(q3Expression);
		clone.maxExpression = JRCloneUtils.nullSafeClone(maxExpression);
		return clone;
	}
}
