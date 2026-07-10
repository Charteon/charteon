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

import java.nio.charset.StandardCharsets;

import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.base.JRBasePrintImage;
import net.sf.jasperreports.engine.type.HorizontalImageAlignEnum;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.engine.type.VerticalImageAlignEnum;

import tech.charteon.component.ChartComponent;
import tech.charteon.export.ssr.EChartsSvgRenderer;

/**
 * Shared logic of the export handlers: renders the option JSON stored on the
 * generic print element to SVG (sized exactly like the report element, so the
 * aspect ratio is preserved by construction) and wraps it in a print image.
 */
public final class ChartElementSvgProvider
{
	/**
	 * Reference render size for the long edge; the vector result is scaled to
	 * the element so ECharts' fixed-pixel decorations stay in proportion.
	 */
	private static final int REFERENCE_SIZE = 640;

	private ChartElementSvgProvider()
	{
	}

	/**
	 * Returns the SVG document for the element, or {@code null} if the element
	 * carries no chart option (e.g. the fill produced no data and no option).
	 */
	public static byte[] getSvg(JRGenericPrintElement element)
	{
		String optionJson = (String) element.getParameterValue(ChartComponent.PARAMETER_OPTION);
		if (optionJson == null || optionJson.isBlank())
		{
			return null;
		}
		String theme = (String) element.getParameterValue(ChartComponent.PARAMETER_THEME);
		String mapName = (String) element.getParameterValue(ChartComponent.PARAMETER_MAP_NAME);

		// Render at a fixed reference resolution (keeping the element's aspect
		// ratio) rather than the element's exact size, then let the vector scale
		// to the frame (viewBox + FILL_FRAME). ECharts sizes its title, legend,
		// axis names and value labels in fixed pixels; rendering those into a
		// small element crowds them into the plot. Rendering at a comfortable
		// reference size and scaling the whole SVG keeps every decoration in
		// proportion — identical behaviour to the designer canvas.
		int w = Math.max(1, element.getWidth());
		int h = Math.max(1, element.getHeight());
		int refW;
		int refH;
		if (w >= h)
		{
			refW = REFERENCE_SIZE;
			refH = Math.max(1, Math.round(REFERENCE_SIZE * (float) h / w));
		}
		else
		{
			refH = REFERENCE_SIZE;
			refW = Math.max(1, Math.round(REFERENCE_SIZE * (float) w / h));
		}
		String svg = EChartsSvgRenderer.renderSvg(optionJson, refW, refH, theme, mapName);
		return svg == null ? null : svg.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Creates the print image skeleton used to embed the rendered chart into
	 * paged export formats.
	 */
	public static JRBasePrintImage createBaseImage(JRGenericPrintElement element)
	{
		JRBasePrintImage printImage = new JRBasePrintImage(element.getDefaultStyleProvider());
		printImage.setUUID(element.getUUID());
		printImage.setX(element.getX());
		printImage.setY(element.getY());
		printImage.setWidth(element.getWidth());
		printImage.setHeight(element.getHeight());
		printImage.setStyle(element.getStyle());
		printImage.setMode(element.getMode());
		printImage.setBackcolor(element.getBackcolor());
		printImage.setForecolor(element.getForecolor());

		// the SVG is rendered at a reference size with a matching aspect ratio,
		// so filling the frame scales every part of the chart uniformly
		printImage.setScaleImage(ScaleImageEnum.FILL_FRAME);
		printImage.setHorizontalImageAlign(HorizontalImageAlignEnum.LEFT);
		printImage.setVerticalImageAlign(VerticalImageAlignEnum.TOP);

		return printImage;
	}
}
