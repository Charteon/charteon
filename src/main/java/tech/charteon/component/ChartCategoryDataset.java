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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import net.sf.jasperreports.engine.JRConstants;
import net.sf.jasperreports.engine.JRExpressionCollector;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.base.JRBaseObjectFactory;
import net.sf.jasperreports.engine.design.JRDesignElementDataset;
import net.sf.jasperreports.engine.util.JRCloneUtils;

/**
 * A category dataset for the Charteon chart component, structured like the
 * category datasets of native JasperReports charts: one or more series, each
 * with series/category/value/label expressions evaluated per record.
 */
public class ChartCategoryDataset extends JRDesignElementDataset
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private List<CategorySeries> seriesList = new ArrayList<>();

	public ChartCategoryDataset()
	{
	}

	public ChartCategoryDataset(ChartCategoryDataset dataset, JRBaseObjectFactory factory)
	{
		super(dataset, factory);

		for (CategorySeries series : dataset.getSeriesList())
		{
			seriesList.add(new CategorySeries(series, factory));
		}
	}

	@JsonIgnore
	public List<CategorySeries> getSeriesList()
	{
		return seriesList;
	}

	@JacksonXmlElementWrapper(useWrapping = false)
	public CategorySeries[] getSeries()
	{
		return seriesList.toArray(new CategorySeries[0]);
	}

	@JsonSetter
	private void setSeries(List<CategorySeries> series)
	{
		if (series != null)
		{
			seriesList.addAll(series);
		}
	}

	public void addSeries(CategorySeries series)
	{
		seriesList.add(series);
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		ChartComponentCompiler.collectExpressions(this, collector);
	}

	@Override
	public Object clone()
	{
		ChartCategoryDataset clone = (ChartCategoryDataset) super.clone();
		if (seriesList != null)
		{
			clone.seriesList = new ArrayList<>(seriesList.size());
			for (CategorySeries series : seriesList)
			{
				clone.seriesList.add(JRCloneUtils.nullSafeClone(series));
			}
		}
		return clone;
	}
}
