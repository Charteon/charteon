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

import tech.charteon.component.CategorySeries;

/**
 * Evaluates the expressions of one category series per record.
 */
public class FillCategorySeries
{
	private final CategorySeries parent;

	private Comparable<?> series;
	private Object category;
	private Number value;
	private String label;

	public FillCategorySeries(CategorySeries parent)
	{
		this.parent = parent;
	}

	public void evaluate(JRCalculator calculator) throws JRExpressionEvalException
	{
		series = (Comparable<?>) calculator.evaluate(parent.getSeriesExpression());
		category = calculator.evaluate(parent.getCategoryExpression());
		value = (Number) calculator.evaluate(parent.getValueExpression());
		label = parent.getLabelExpression() == null
			? null
			: toString(calculator.evaluate(parent.getLabelExpression()));
	}

	private static String toString(Object value)
	{
		return value == null ? null : value.toString();
	}

	/** The optional per-series type override (combo charts); may be {@code null}. */
	public String getSeriesType()
	{
		return parent.getSeriesType();
	}

	/** Whether this series is plotted against the secondary value axis. */
	public boolean isSecondaryAxis()
	{
		return Boolean.TRUE.equals(parent.getSecondaryAxis());
	}

	/** The optional fixed series color; may be {@code null}. */
	public String getColor()
	{
		return parent.getColor();
	}

	public Comparable<?> getSeries()
	{
		return series;
	}

	public Object getCategory()
	{
		return category;
	}

	public Number getValue()
	{
		return value;
	}

	public String getLabel()
	{
		return label;
	}
}
