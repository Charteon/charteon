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

import tech.charteon.component.CategorySeries;
import tech.charteon.component.ChartCategoryDataset;
import tech.charteon.model.CategoryChartData;

/**
 * Fill-time counterpart of {@link ChartCategoryDataset}: evaluates all series
 * expressions per record and accumulates the results into a
 * {@link CategoryChartData}.
 */
public class FillCategoryDataset extends JRFillElementDataset
{
	public static final String EXCEPTION_MESSAGE_KEY_SERIES_NULL_NAME =
		"charteon.category.dataset.series.null.name";

	private final List<FillCategorySeries> series = new ArrayList<>();

	private CategoryChartData data;

	public FillCategoryDataset(ChartCategoryDataset dataset, JRFillObjectFactory factory)
	{
		super(dataset, factory);

		for (CategorySeries designSeries : dataset.getSeriesList())
		{
			series.add(new FillCategorySeries(designSeries));
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
		for (FillCategorySeries fillSeries : series)
		{
			fillSeries.evaluate(calculator);
		}
	}

	@Override
	protected void customIncrement()
	{
		if (data == null)
		{
			data = new CategoryChartData();
		}

		for (FillCategorySeries fillSeries : series)
		{
			Comparable<?> seriesName = fillSeries.getSeries();
			if (seriesName == null)
			{
				throw new JRRuntimeException(
					"Charteon: category series name (seriesExpression) evaluated to null");
			}
			Object category = fillSeries.getCategory();
			String name = String.valueOf(seriesName);
			data.addValue(
				name,
				category == null ? "" : String.valueOf(category),
				fillSeries.getValue(),
				fillSeries.getLabel());
			data.setStyle(name, fillSeries.getSeriesType(), fillSeries.isSecondaryAxis(),
				fillSeries.getColor());
		}
	}

	/**
	 * One last increment is required to include the values of the last record.
	 */
	public void finishDataset()
	{
		increment();
	}

	public CategoryChartData getData()
	{
		return data == null ? new CategoryChartData() : data;
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		// expressions are collected at compile time by ChartComponentCompiler;
		// nothing to collect on the fill-time instance
	}
}
