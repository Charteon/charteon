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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.JRPrintImage;
import net.sf.jasperreports.engine.base.JRBasePrintImage;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.renderers.SimpleDataRenderer;

import tech.charteon.export.ChartElementSvgProvider;

/**
 * Produces a high-resolution PNG print image for the chart element; used by
 * the raster-based export handlers (XLSX/DOCX/PPTX/ODT/ODS/RTF). The rendered
 * PNG is cached in a weak map keyed by the element, so multi-pass exports do
 * not re-render; the cache is deliberately NOT stored as an element parameter,
 * because element parameters are serialized by the print-XML exporter.
 */
public final class RasterChartImageProvider
{
	private static final Map<JRGenericPrintElement, byte[]> PNG_CACHE =
		Collections.synchronizedMap(new WeakHashMap<>());

	private RasterChartImageProvider()
	{
	}

	/**
	 * @return the print image, or {@code null} when the element carries no
	 * chart option
	 */
	public static JRPrintImage getImage(JRGenericPrintElement element)
	{
		byte[] png = PNG_CACHE.get(element);
		if (png == null)
		{
			byte[] svg = ChartElementSvgProvider.getSvg(element);
			if (svg == null)
			{
				return null;
			}
			png = SvgRasterizer.toPng(svg, element.getWidth(), element.getHeight());
			PNG_CACHE.put(element, png);
		}

		JRBasePrintImage printImage = ChartElementSvgProvider.createBaseImage(element);
		// the PNG is supersampled, so it must be scaled down into the element bounds
		printImage.setScaleImage(ScaleImageEnum.RETAIN_SHAPE);
		printImage.setRenderer(SimpleDataRenderer.getInstance(png));
		return printImage;
	}
}
