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
package tech.charteon.export.text;

import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.export.GenericElementCsvHandler;
import net.sf.jasperreports.engine.export.JRCsvExporterContext;

/**
 * CSV export handler. CSV cannot display an image, so the chart is exported
 * as its data: the chart title followed by the series values (name=value
 * pairs). This keeps the underlying numbers available in data-centric
 * exports instead of dropping the element entirely.
 */
public class ChartElementCsvHandler implements GenericElementCsvHandler
{
	private static final ChartElementCsvHandler INSTANCE = new ChartElementCsvHandler();

	public static ChartElementCsvHandler getInstance()
	{
		return INSTANCE;
	}

	@Override
	public String getTextValue(JRCsvExporterContext exporterContext, JRGenericPrintElement element)
	{
		return ChartTextSummary.getSummary(element);
	}

	@Override
	public boolean toExport(JRGenericPrintElement element)
	{
		return true;
	}
}
