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
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.fill.JRCalculator;
import net.sf.jasperreports.engine.fill.JRExpressionEvalException;
import net.sf.jasperreports.engine.fill.JRFillElementDataset;
import net.sf.jasperreports.engine.fill.JRFillObjectFactory;

import tech.charteon.component.ChartRelationDataset;
import tech.charteon.model.RelationChartData;

/**
 * Fill-time counterpart of {@link ChartRelationDataset}: collects one
 * source&rarr;target edge per record.
 */
public class FillRelationDataset extends JRFillElementDataset
{
	private final ChartRelationDataset dataset;

	private String source;
	private String target;
	private Number value;
	private Number sourceX;
	private Number sourceY;
	private Number targetX;
	private Number targetY;

	private RelationChartData data;

	public FillRelationDataset(ChartRelationDataset dataset, JRFillObjectFactory factory)
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
		Object sourceValue = calculator.evaluate(dataset.getSourceExpression());
		Object targetValue = calculator.evaluate(dataset.getTargetExpression());
		source = sourceValue == null ? null : sourceValue.toString();
		target = targetValue == null ? null : targetValue.toString();
		value = (Number) calculator.evaluate(dataset.getValueExpression());
		sourceX = (Number) calculator.evaluate(dataset.getSourceXExpression());
		sourceY = (Number) calculator.evaluate(dataset.getSourceYExpression());
		targetX = (Number) calculator.evaluate(dataset.getTargetXExpression());
		targetY = (Number) calculator.evaluate(dataset.getTargetYExpression());
	}

	@Override
	protected void customIncrement()
	{
		if (data == null)
		{
			data = new RelationChartData();
		}
		if (source == null || target == null)
		{
			throw new JRRuntimeException(
				"Charteon: relation source/target expression evaluated to null");
		}
		data.addEdge(source, target, value, sourceX, sourceY, targetX, targetY);
	}

	/**
	 * One last increment is required to include the values of the last record.
	 */
	public void finishDataset()
	{
		increment();
	}

	public RelationChartData getData()
	{
		return data == null ? new RelationChartData() : data;
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		// expressions are collected at compile time by ChartComponentCompiler
	}
}
