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
package tech.charteon.export;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.export.GenericElementGraphics2DHandler;
import net.sf.jasperreports.engine.export.JRGraphics2DExporterContext;
import net.sf.jasperreports.engine.export.draw.Offset;
import net.sf.jasperreports.renderers.SimpleDataRenderer;
import net.sf.jasperreports.renderers.WrappingSvgDataToGraphics2DRenderer;

/**
 * Graphics2D export handler (used by the Swing viewer and by Java printing):
 * draws the SVG through JasperReports' Batik bridge directly onto the
 * graphics context — vector output here as well.
 */
public class ChartElementGraphics2DHandler implements GenericElementGraphics2DHandler
{
	private static final ChartElementGraphics2DHandler INSTANCE = new ChartElementGraphics2DHandler();

	public static ChartElementGraphics2DHandler getInstance()
	{
		return INSTANCE;
	}

	@Override
	public void exportElement(
		JRGraphics2DExporterContext exporterContext,
		JRGenericPrintElement element,
		Graphics2D grx,
		Offset offset)
	{
		byte[] svg = ChartElementSvgProvider.getSvg(element);
		if (svg == null)
		{
			return;
		}

		try
		{
			WrappingSvgDataToGraphics2DRenderer renderer =
				new WrappingSvgDataToGraphics2DRenderer(SimpleDataRenderer.getInstance(svg));
			renderer.render(
				exporterContext.getJasperReportsContext(),
				grx,
				new Rectangle2D.Double(
					element.getX() + offset.getX(),
					element.getY() + offset.getY(),
					element.getWidth(),
					element.getHeight()));
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
