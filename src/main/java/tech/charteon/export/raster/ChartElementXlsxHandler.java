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
package tech.charteon.export.raster;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.JRPrintImage;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.export.JRExporterGridCell;
import net.sf.jasperreports.engine.export.ooxml.GenericElementXlsxHandler;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporterContext;

/**
 * XLSX export handler: embeds the chart as a high-resolution PNG.
 */
public class ChartElementXlsxHandler implements GenericElementXlsxHandler
{
	private static final ChartElementXlsxHandler INSTANCE = new ChartElementXlsxHandler();

	public static ChartElementXlsxHandler getInstance()
	{
		return INSTANCE;
	}

	@Override
	public void exportElement(
		JRXlsxExporterContext exporterContext,
		JRGenericPrintElement element,
		JRExporterGridCell gridCell,
		int colIndex,
		int rowIndex)
	{
		try
		{
			JRPrintImage image = getImage(exporterContext, element);
			if (image == null)
			{
				return;
			}
			JRXlsxExporter exporter = (JRXlsxExporter) exporterContext.getExporterRef();
			exporter.exportImage(image, gridCell, colIndex, rowIndex, 0, 0, null);
		}
		catch (Exception e)
		{
			throw new JRRuntimeException(e);
		}
	}

	@Override
	public JRPrintImage getImage(JRXlsxExporterContext exporterContext, JRGenericPrintElement element)
		throws JRException
	{
		return RasterChartImageProvider.getImage(element);
	}

	@Override
	public boolean toExport(JRGenericPrintElement element)
	{
		return true;
	}
}
