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

import net.sf.jasperreports.engine.JRComponentElement;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.base.JRBasePrintImage;
import net.sf.jasperreports.engine.base.JRBasePrintText;
import net.sf.jasperreports.engine.type.HorizontalImageAlignEnum;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.engine.type.VerticalImageAlignEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import net.sf.jasperreports.engine.component.ComponentDesignConverter;
import net.sf.jasperreports.engine.convert.ReportConverter;
import net.sf.jasperreports.renderers.SimpleDataRenderer;

import tech.charteon.model.ChartSamplePreview;

/**
 * Design-time preview converter. Renders a representative sample-data preview
 * of the chart (via the ECharts SSR pipeline) so the component shows a real
 * chart in a designer canvas (e.g. Jaspersoft Studio). If the preview cannot
 * be rendered — most notably when the host JVM is older than 21, which GraalJS
 * requires — it falls back to a centered text placeholder, so a broken preview
 * never breaks the designer.
 */
public class ChartDesignConverter implements ComponentDesignConverter
{
	private static final ChartDesignConverter INSTANCE = new ChartDesignConverter();

	public static ChartDesignConverter getInstance()
	{
		return INSTANCE;
	}

	@Override
	public JRPrintElement convert(ReportConverter reportConverter, JRComponentElement element)
	{
		ChartComponent chart = element.getComponent() instanceof ChartComponent
			? (ChartComponent) element.getComponent() : null;

		if (chart != null)
		{
			// Render at a fixed reference resolution (keeping the element's aspect
			// ratio) rather than at the element's live pixel size, then let the
			// vector image scale to fill the element. ECharts lays out its title,
			// legend, axis names and value labels with fixed pixel sizes; rendering
			// those into a small element makes them crowd into the plot ("sticky"
			// title/legend). Rendering big and scaling the whole SVG uniformly keeps
			// every decoration in proportion as the element is resized.
			int w = Math.max(1, element.getWidth());
			int h = Math.max(1, element.getHeight());
			int refW;
			int refH;
			if (w >= h)
			{
				refW = REFERENCE_SIZE;
				refH = quantize(Math.round(REFERENCE_SIZE * (float) h / w));
			}
			else
			{
				refH = REFERENCE_SIZE;
				refW = quantize(Math.round(REFERENCE_SIZE * (float) w / h));
			}
			byte[] svg = ChartSamplePreview.renderPreviewSvg(chart, refW, refH);
			if (svg != null)
			{
				return svgImage(reportConverter, element, svg);
			}
		}

		return placeholder(reportConverter, element, chart);
	}

	/**
	 * Reference render size for the long edge. Big enough that ECharts' default
	 * pixel-sized decorations look balanced; the vector result is then scaled to
	 * the element.
	 */
	private static final int REFERENCE_SIZE = 640;

	/** Rounds to a multiple of 16 so proportional resizes reuse a cached render. */
	private static int quantize(int value)
	{
		return Math.max(16, Math.round(value / 16f) * 16);
	}

	private JRPrintElement svgImage(
		ReportConverter reportConverter, JRComponentElement element, byte[] svg)
	{
		JRBasePrintImage image = new JRBasePrintImage(reportConverter.getDefaultStyleProvider());
		reportConverter.copyBaseAttributes(element, image);
		// the SVG was rendered at the element's aspect ratio, so stretching it to
		// fill the frame scales every part of the chart uniformly (no distortion)
		image.setScaleImage(ScaleImageEnum.FILL_FRAME);
		image.setHorizontalImageAlign(HorizontalImageAlignEnum.LEFT);
		image.setVerticalImageAlign(VerticalImageAlignEnum.TOP);
		image.setRenderer(SimpleDataRenderer.getInstance(svg));
		return image;
	}

	private JRPrintElement placeholder(
		ReportConverter reportConverter, JRComponentElement element, ChartComponent chart)
	{
		JRBasePrintText text = new JRBasePrintText(reportConverter.getDefaultStyleProvider());
		reportConverter.copyBaseAttributes(element, text);
		text.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
		text.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);

		String typeName = chart != null && chart.getChartType() != null
			? chart.getChartType().getName() : "raw option";
		text.setText("[Charteon chart: " + typeName + "]");

		return text;
	}
}
