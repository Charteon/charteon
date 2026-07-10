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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import net.sf.jasperreports.engine.JRCloneable;
import net.sf.jasperreports.engine.JRConstants;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.base.JRBaseObjectFactory;

/**
 * One series of a {@link ChartXyDataset}, used by scatter and bubble charts.
 */
@JsonPropertyOrder({
	"seriesExpression",
	"xValueExpression",
	"yValueExpression",
	"sizeExpression",
	"labelExpression"
	})
public class XySeries implements JRCloneable, Serializable
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	private JRExpression seriesExpression;
	private JRExpression xValueExpression;
	private JRExpression yValueExpression;
	private JRExpression sizeExpression;
	private JRExpression labelExpression;

	public XySeries()
	{
	}

	public XySeries(XySeries series, JRBaseObjectFactory factory)
	{
		this.seriesExpression = factory.getExpression(series.getSeriesExpression());
		this.xValueExpression = factory.getExpression(series.getXValueExpression());
		this.yValueExpression = factory.getExpression(series.getYValueExpression());
		this.sizeExpression = factory.getExpression(series.getSizeExpression());
		this.labelExpression = factory.getExpression(series.getLabelExpression());
	}

	public JRExpression getSeriesExpression()
	{
		return seriesExpression;
	}

	public void setSeriesExpression(JRExpression seriesExpression)
	{
		this.seriesExpression = seriesExpression;
	}

	/**
	 * The expression providing the {@code java.lang.Number} x value.
	 */
	// explicit property name: bean naming would lowercase it to "xvalueExpression"
	@JsonProperty("xValueExpression")
	public JRExpression getXValueExpression()
	{
		return xValueExpression;
	}

	@JsonProperty("xValueExpression")
	public void setXValueExpression(JRExpression xValueExpression)
	{
		this.xValueExpression = xValueExpression;
	}

	/**
	 * The expression providing the {@code java.lang.Number} y value.
	 */
	@JsonProperty("yValueExpression")
	public JRExpression getYValueExpression()
	{
		return yValueExpression;
	}

	@JsonProperty("yValueExpression")
	public void setYValueExpression(JRExpression yValueExpression)
	{
		this.yValueExpression = yValueExpression;
	}

	/**
	 * Optional expression providing a {@code java.lang.Number} used as the
	 * symbol size (bubble charts).
	 */
	public JRExpression getSizeExpression()
	{
		return sizeExpression;
	}

	public void setSizeExpression(JRExpression sizeExpression)
	{
		this.sizeExpression = sizeExpression;
	}

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
			XySeries clone = (XySeries) super.clone();
			clone.seriesExpression = seriesExpression == null ? null : (JRExpression) seriesExpression.clone();
			clone.xValueExpression = xValueExpression == null ? null : (JRExpression) xValueExpression.clone();
			clone.yValueExpression = yValueExpression == null ? null : (JRExpression) yValueExpression.clone();
			clone.sizeExpression = sizeExpression == null ? null : (JRExpression) sizeExpression.clone();
			clone.labelExpression = labelExpression == null ? null : (JRExpression) labelExpression.clone();
			return clone;
		}
		catch (CloneNotSupportedException e)
		{
			throw new JRRuntimeException(e);
		}
	}
}
