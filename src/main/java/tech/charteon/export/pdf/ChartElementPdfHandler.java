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
package tech.charteon.export.pdf;

import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.base.JRBasePrintImage;
import net.sf.jasperreports.pdf.GenericElementPdfHandler;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.pdf.JRPdfExporterContext;
import net.sf.jasperreports.renderers.SimpleDataRenderer;

import tech.charteon.export.ChartElementSvgProvider;

/**
 * PDF export handler. The chart is rendered server-side to SVG and handed to
 * the PDF exporter as an SVG data renderable; JasperReports draws it through
 * its Batik bridge as native vector graphics into the PDF content stream —
 * the chart stays sharp at every zoom level, no rasterization involved.
 */
public class ChartElementPdfHandler implements GenericElementPdfHandler
{
	private static final ChartElementPdfHandler INSTANCE = new ChartElementPdfHandler();

	public static ChartElementPdfHandler getInstance()
	{
		return INSTANCE;
	}

	@Override
	public void exportElement(JRPdfExporterContext exporterContext, JRGenericPrintElement element)
	{
		byte[] svg = ChartElementSvgProvider.getSvg(element);
		if (svg == null)
		{
			return;
		}

		try
		{
			JRBasePrintImage printImage = ChartElementSvgProvider.createBaseImage(element);
			printImage.setRenderer(SimpleDataRenderer.getInstance(svg));

			JRPdfExporter exporter = (JRPdfExporter) exporterContext.getExporterRef();
			exporter.exportImage(printImage);
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
