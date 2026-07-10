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

import net.sf.jasperreports.engine.JRExpressionCollector;
import net.sf.jasperreports.engine.fill.JRCalculator;
import net.sf.jasperreports.engine.fill.JRExpressionEvalException;
import net.sf.jasperreports.engine.fill.JRFillElementDataset;
import net.sf.jasperreports.engine.fill.JRFillObjectFactory;

import tech.charteon.component.ChartBoxplotDataset;
import tech.charteon.model.BoxplotChartData;

/**
 * Fill-time counterpart of {@link ChartBoxplotDataset}: collects one
 * five-number summary per record.
 */
public class FillBoxplotDataset extends JRFillElementDataset
{
	private final ChartBoxplotDataset dataset;

	private String category;
	private Number min;
	private Number q1;
	private Number median;
	private Number q3;
	private Number max;

	private BoxplotChartData data;

	public FillBoxplotDataset(ChartBoxplotDataset dataset, JRFillObjectFactory factory)
	{
		super(dataset, factory);
		this.dataset = dataset;
	}

	@Override
	protected void customInitialize()
	{
		data = null;
	}

	@Override
	protected void customEvaluate(JRCalculator calculator) throws JRExpressionEvalException
	{
		Object categoryValue = calculator.evaluate(dataset.getCategoryExpression());
		category = categoryValue == null ? "" : categoryValue.toString();
		min = (Number) calculator.evaluate(dataset.getMinExpression());
		q1 = (Number) calculator.evaluate(dataset.getQ1Expression());
		median = (Number) calculator.evaluate(dataset.getMedianExpression());
		q3 = (Number) calculator.evaluate(dataset.getQ3Expression());
		max = (Number) calculator.evaluate(dataset.getMaxExpression());
	}

	@Override
	protected void customIncrement()
	{
		if (data == null)
		{
			data = new BoxplotChartData();
		}
		data.addBox(category, min, q1, median, q3, max);
	}

	/**
	 * One last increment is required to include the values of the last record.
	 */
	public void finishDataset()
	{
		increment();
	}

	public BoxplotChartData getData()
	{
		return data == null ? new BoxplotChartData() : data;
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		// expressions are collected at compile time by ChartComponentCompiler
	}
}
