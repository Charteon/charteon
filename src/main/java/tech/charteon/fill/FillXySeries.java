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
package tech.charteon.fill;

import net.sf.jasperreports.engine.fill.JRCalculator;
import net.sf.jasperreports.engine.fill.JRExpressionEvalException;

import tech.charteon.component.XySeries;

/**
 * Evaluates the expressions of one x/y series per record.
 */
public class FillXySeries
{
	private final XySeries parent;

	private Comparable<?> series;
	private Number xValue;
	private Number yValue;
	private Number size;
	private String label;

	public FillXySeries(XySeries parent)
	{
		this.parent = parent;
	}

	public void evaluate(JRCalculator calculator) throws JRExpressionEvalException
	{
		series = (Comparable<?>) calculator.evaluate(parent.getSeriesExpression());
		xValue = (Number) calculator.evaluate(parent.getXValueExpression());
		yValue = (Number) calculator.evaluate(parent.getYValueExpression());
		size = parent.getSizeExpression() == null
			? null
			: (Number) calculator.evaluate(parent.getSizeExpression());
		label = parent.getLabelExpression() == null
			? null
			: toString(calculator.evaluate(parent.getLabelExpression()));
	}

	private static String toString(Object value)
	{
		return value == null ? null : value.toString();
	}

	public Comparable<?> getSeries()
	{
		return series;
	}

	public Number getXValue()
	{
		return xValue;
	}

	public Number getYValue()
	{
		return yValue;
	}

	public Number getSize()
	{
		return size;
	}

	public String getLabel()
	{
		return label;
	}
}
