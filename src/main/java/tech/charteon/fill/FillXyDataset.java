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

import java.util.ArrayList;
import java.util.List;

import net.sf.jasperreports.engine.JRExpressionCollector;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.fill.JRCalculator;
import net.sf.jasperreports.engine.fill.JRExpressionEvalException;
import net.sf.jasperreports.engine.fill.JRFillElementDataset;
import net.sf.jasperreports.engine.fill.JRFillObjectFactory;

import tech.charteon.component.ChartXyDataset;
import tech.charteon.component.XySeries;
import tech.charteon.model.XyChartData;

/**
 * Fill-time counterpart of {@link ChartXyDataset}.
 */
public class FillXyDataset extends JRFillElementDataset
{
	private final List<FillXySeries> series = new ArrayList<>();

	private XyChartData data;

	public FillXyDataset(ChartXyDataset dataset, JRFillObjectFactory factory)
	{
		super(dataset, factory);

		for (XySeries designSeries : dataset.getSeriesList())
		{
			series.add(new FillXySeries(designSeries));
		}
	}

	@Override
	protected void customInitialize()
	{
		data = null;
	}

	@Override
	protected void customEvaluate(JRCalculator calculator) throws JRExpressionEvalException
	{
		for (FillXySeries fillSeries : series)
		{
			fillSeries.evaluate(calculator);
		}
	}

	@Override
	protected void customIncrement()
	{
		if (data == null)
		{
			data = new XyChartData();
		}

		for (FillXySeries fillSeries : series)
		{
			Comparable<?> seriesName = fillSeries.getSeries();
			if (seriesName == null)
			{
				throw new JRRuntimeException(
					"Charteon: xy series name (seriesExpression) evaluated to null");
			}
			data.addPoint(
				String.valueOf(seriesName),
				fillSeries.getXValue(),
				fillSeries.getYValue(),
				fillSeries.getSize(),
				fillSeries.getLabel());
		}
	}

	/**
	 * One last increment is required to include the values of the last record.
	 */
	public void finishDataset()
	{
		increment();
	}

	public XyChartData getData()
	{
		return data == null ? new XyChartData() : data;
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		// expressions are collected at compile time by ChartComponentCompiler;
		// nothing to collect on the fill-time instance
	}
}
