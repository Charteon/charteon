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

import tech.charteon.component.ChartHierarchyDataset;
import tech.charteon.model.HierarchyChartData;

/**
 * Fill-time counterpart of {@link ChartHierarchyDataset}: collects one
 * (name, parent, value) node per record.
 */
public class FillHierarchyDataset extends JRFillElementDataset
{
	private final ChartHierarchyDataset dataset;

	private String name;
	private String parent;
	private Number value;

	private HierarchyChartData data;

	public FillHierarchyDataset(ChartHierarchyDataset dataset, JRFillObjectFactory factory)
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
		Object nameValue = calculator.evaluate(dataset.getNameExpression());
		Object parentValue = calculator.evaluate(dataset.getParentExpression());
		name = nameValue == null ? null : nameValue.toString();
		parent = parentValue == null ? null : parentValue.toString();
		value = (Number) calculator.evaluate(dataset.getValueExpression());
	}

	@Override
	protected void customIncrement()
	{
		if (data == null)
		{
			data = new HierarchyChartData();
		}
		if (name == null)
		{
			throw new JRRuntimeException(
				"Charteon: hierarchy node name (nameExpression) evaluated to null");
		}
		data.addNode(name, parent, value);
	}

	/**
	 * One last increment is required to include the values of the last record.
	 */
	public void finishDataset()
	{
		increment();
	}

	public HierarchyChartData getData()
	{
		return data == null ? new HierarchyChartData() : data;
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		// expressions are collected at compile time by ChartComponentCompiler
	}
}
