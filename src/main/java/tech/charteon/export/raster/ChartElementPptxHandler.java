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

import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.JRPrintImage;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.export.ooxml.GenericElementPptxHandler;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporterContext;

/**
 * PPTX export handler: embeds the chart as a high-resolution PNG.
 */
public class ChartElementPptxHandler implements GenericElementPptxHandler
{
	private static final ChartElementPptxHandler INSTANCE = new ChartElementPptxHandler();

	public static ChartElementPptxHandler getInstance()
	{
		return INSTANCE;
	}

	@Override
	public void exportElement(
		JRPptxExporterContext exporterContext,
		JRGenericPrintElement element)
	{
		try
		{
			JRPrintImage image = RasterChartImageProvider.getImage(element);
			if (image == null)
			{
				return;
			}
			JRPptxExporter exporter = (JRPptxExporter) exporterContext.getExporterRef();
			exporter.exportImage(image);
		}
		catch (Exception e)
		{
			throw new JRRuntimeException(e);
		}
	}

	@Override
	public boolean toExport(JRGenericPrintElement element)
	{
		return true;
	}
}
