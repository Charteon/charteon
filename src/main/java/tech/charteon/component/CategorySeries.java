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

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import net.sf.jasperreports.engine.JRCloneable;
import net.sf.jasperreports.engine.JRConstants;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.base.JRBaseObjectFactory;

/**
 * One series of a {@link ChartCategoryDataset}, mirroring the familiar
 * series/category/value/label expression structure of JasperReports chart
 * datasets.
 *
 * <p>For combined (combo) charts, a series may override the chart's base type
 * via {@code seriesType} (e.g. draw a {@code line} on top of {@code bar}s) and
 * may be plotted against a second value axis via {@code secondaryAxis}, which
 * is the standard "bars on the left axis, trend line on the right axis" case.
 */
@JsonPropertyOrder({
	"seriesType",
	"secondaryAxis",
	"color",
	"seriesExpression",
	"categoryExpression",
	"valueExpression",
	"labelExpression"
	})
public class CategorySeries implements JRCloneable, Serializable
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private String seriesType;
	private Boolean secondaryAxis;
	private String color;
	private JRExpression seriesExpression;
	private JRExpression categoryExpression;
	private JRExpression valueExpression;
	private JRExpression labelExpression;

	public CategorySeries()
	{
	}

	public CategorySeries(CategorySeries series, JRBaseObjectFactory factory)
	{
		this.seriesType = series.getSeriesType();
		this.secondaryAxis = series.getSecondaryAxis();
		this.color = series.getColor();
		this.seriesExpression = factory.getExpression(series.getSeriesExpression());
		this.categoryExpression = factory.getExpression(series.getCategoryExpression());
		this.valueExpression = factory.getExpression(series.getValueExpression());
		this.labelExpression = factory.getExpression(series.getLabelExpression());
	}

	/**
	 * Optional per-series chart type, overriding the chart's base type for this
	 * series only (e.g. {@code "line"} to draw a trend line over {@code bar}
	 * series). Only meaningful for the axis-based types (bar/line). {@code null}
	 * uses the chart's base type.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getSeriesType()
	{
		return seriesType;
	}

	public void setSeriesType(String seriesType)
	{
		this.seriesType = seriesType;
	}

	/**
	 * Whether this series is plotted against a second value axis (drawn on the
	 * opposite side). Used for combo charts whose series have very different
	 * magnitudes or units. {@code null}/{@code false} uses the primary axis.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public Boolean getSecondaryAxis()
	{
		return secondaryAxis;
	}

	public void setSecondaryAxis(Boolean secondaryAxis)
	{
		this.secondaryAxis = secondaryAxis;
	}

	/**
	 * Optional fixed color for this series (any CSS/ECharts color, e.g.
	 * {@code "#2e7d32"}). {@code null} uses the chart palette. Combo charts use
	 * this to give each series its own color (e.g. blue bars, red line).
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getColor()
	{
		return color;
	}

	public void setColor(String color)
	{
		this.color = color;
	}

	/**
	 * The expression that provides the series name; usually a constant
	 * {@code String}, but any {@code java.lang.Comparable} works.
	 */
	public JRExpression getSeriesExpression()
	{
		return seriesExpression;
	}

	public void setSeriesExpression(JRExpression seriesExpression)
	{
		this.seriesExpression = seriesExpression;
	}

	/**
	 * The expression that provides the category for each record.
	 */
	public JRExpression getCategoryExpression()
	{
		return categoryExpression;
	}

	public void setCategoryExpression(JRExpression categoryExpression)
	{
		this.categoryExpression = categoryExpression;
	}

	/**
	 * The expression that provides the {@code java.lang.Number} value for each
	 * category in the series.
	 */
	public JRExpression getValueExpression()
	{
		return valueExpression;
	}

	public void setValueExpression(JRExpression valueExpression)
	{
		this.valueExpression = valueExpression;
	}

	/**
	 * Optional expression that customizes the item label for each value.
	 */
	public JRExpression getLabelExpression()
	{
		return labelExpression;
	}

	public void setLabelExpression(JRExpression labelExpression)
	{
		this.labelExpression = labelExpression;
	}

	@Override
	public Object clone()
	{
		try
		{
			CategorySeries clone = (CategorySeries) super.clone();
			clone.seriesExpression = seriesExpression == null ? null : (JRExpression) seriesExpression.clone();
			clone.categoryExpression = categoryExpression == null ? null : (JRExpression) categoryExpression.clone();
			clone.valueExpression = valueExpression == null ? null : (JRExpression) valueExpression.clone();
			clone.labelExpression = labelExpression == null ? null : (JRExpression) labelExpression.clone();
			return clone;
		}
		catch (CloneNotSupportedException e)
		{
			throw new JRRuntimeException(e);
		}
	}
}
