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
 * A candlestick (OHLC) dataset: each record contributes the open, close, low
 * and high values of one category (typically a trading day).
 */
@JsonPropertyOrder({
	"categoryExpression",
	"openExpression",
	"closeExpression",
	"lowExpression",
	"highExpression"
	})
public class ChartCandlestickDataset extends JRDesignElementDataset
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private JRExpression categoryExpression;
	private JRExpression openExpression;
	private JRExpression closeExpression;
	private JRExpression lowExpression;
	private JRExpression highExpression;

	public ChartCandlestickDataset()
	{
	}

	public ChartCandlestickDataset(ChartCandlestickDataset dataset, JRBaseObjectFactory factory)
	{
		super(dataset, factory);

		this.categoryExpression = factory.getExpression(dataset.getCategoryExpression());
		this.openExpression = factory.getExpression(dataset.getOpenExpression());
		this.closeExpression = factory.getExpression(dataset.getCloseExpression());
		this.lowExpression = factory.getExpression(dataset.getLowExpression());
		this.highExpression = factory.getExpression(dataset.getHighExpression());
	}

	public JRExpression getCategoryExpression()
	{
		return categoryExpression;
	}

	public void setCategoryExpression(JRExpression categoryExpression)
	{
		this.categoryExpression = categoryExpression;
	}

	public JRExpression getOpenExpression()
	{
		return openExpression;
	}

	public void setOpenExpression(JRExpression openExpression)
	{
		this.openExpression = openExpression;
	}

	public JRExpression getCloseExpression()
	{
		return closeExpression;
	}

	public void setCloseExpression(JRExpression closeExpression)
	{
		this.closeExpression = closeExpression;
	}

	public JRExpression getLowExpression()
	{
		return lowExpression;
	}

	public void setLowExpression(JRExpression lowExpression)
	{
		this.lowExpression = lowExpression;
	}

	public JRExpression getHighExpression()
	{
		return highExpression;
	}

	public void setHighExpression(JRExpression highExpression)
	{
		this.highExpression = highExpression;
	}

	@Override
	public void collectExpressions(JRExpressionCollector collector)
	{
		ChartComponentCompiler.collectExpressions(this, collector);
	}

	@Override
	public Object clone()
	{
		ChartCandlestickDataset clone = (ChartCandlestickDataset) super.clone();
		clone.categoryExpression = JRCloneUtils.nullSafeClone(categoryExpression);
		clone.openExpression = JRCloneUtils.nullSafeClone(openExpression);
		clone.closeExpression = JRCloneUtils.nullSafeClone(closeExpression);
		clone.lowExpression = JRCloneUtils.nullSafeClone(lowExpression);
		clone.highExpression = JRCloneUtils.nullSafeClone(highExpression);
		return clone;
	}
}
