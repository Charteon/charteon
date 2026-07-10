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

import tech.charteon.component.ChartCandlestickDataset;
import tech.charteon.model.CandlestickChartData;

/**
 * Fill-time counterpart of {@link ChartCandlestickDataset}: collects one
 * OHLC tuple per record.
 */
public class FillCandlestickDataset extends JRFillElementDataset
{
	private final ChartCandlestickDataset dataset;

	private String category;
	private Number open;
	private Number close;
	private Number low;
	private Number high;

	private CandlestickChartData data;

	public FillCandlestickDataset(ChartCandlestickDataset dataset, JRFillObjectFactory factory)
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
		open = (Number) calculator.evaluate(dataset.getOpenExpression());
		close = (Number) calculator.evaluate(dataset.getCloseExpression());
		low = (Number) calculator.evaluate(dataset.getLowExpression());
		high = (Number) calculator.evaluate(dataset.getHighExpression());
	}

	@Override
	protected void customIncrement()
	{
		if (data == null)
		{
			data = new CandlestickChartData();
		}
		data.addCandle(category, open, close, low, high);
	}

	/**
	 * One last increment is required to include the values of the last record.
	 */
	public void finishDataset()
	{
		increment();
	}

	public CandlestickChartData getData()
	{
		return data == null ? new CandlestickChartData() : data;
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		// expressions are collected at compile time by ChartComponentCompiler
	}
}
