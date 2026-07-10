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

import net.sf.jasperreports.engine.component.Component;
import net.sf.jasperreports.engine.component.ComponentFillFactory;
import net.sf.jasperreports.engine.component.FillComponent;
import net.sf.jasperreports.engine.fill.JRFillCloneFactory;
import net.sf.jasperreports.engine.fill.JRFillObjectFactory;

import tech.charteon.component.ChartComponent;

/**
 * Creates the fill-time component instances for the Charteon chart component.
 */
public class ChartFillFactory implements ComponentFillFactory
{
	@Override
	public FillComponent toFillComponent(Component component, JRFillObjectFactory factory)
	{
		return new ChartFillComponent((ChartComponent) component, factory);
	}

	@Override
	public FillComponent cloneFillComponent(FillComponent component, JRFillCloneFactory factory)
	{
		throw new UnsupportedOperationException(
			"Charteon chart components do not support fill cloning");
	}
}
