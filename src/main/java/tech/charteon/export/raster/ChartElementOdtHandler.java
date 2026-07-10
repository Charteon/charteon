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

import java.awt.Color;
import java.awt.Dimension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.JRPrintImage;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.export.JRExporterGridCell;
import net.sf.jasperreports.engine.export.oasis.DocumentBuilder;
import net.sf.jasperreports.engine.export.oasis.GenericElementOdtHandler;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporterContext;
import net.sf.jasperreports.engine.export.oasis.TableBuilder;
import net.sf.jasperreports.engine.export.oasis.WriterHelper;
import net.sf.jasperreports.renderers.Renderable;
import net.sf.jasperreports.renderers.RenderersCache;

/**
 * ODT export handler: embeds the chart as a high-resolution PNG.
 *
 * <p>
 * The image XML is written directly (a plain {@code draw:frame} +
 * {@code draw:image}) instead of delegating to
 * {@code JROdtExporter.exportImage(...)}: the exporter wraps images in a
 * {@code draw:frame > draw:text-box > draw:frame} structure (for rotation
 * support) that Microsoft Word does not render — the charts would be
 * invisible when the ODT is opened in Word. LibreOffice renders both
 * structures. The image bytes are still registered through the exporter's
 * own {@code DocumentBuilder} (via reflection), so the ODF package stays
 * consistent; if that internal access ever fails, the handler falls back to
 * the exporter's own image writing.
 */
public class ChartElementOdtHandler implements GenericElementOdtHandler
{
	private static final ChartElementOdtHandler INSTANCE = new ChartElementOdtHandler();

	public static ChartElementOdtHandler getInstance()
	{
		return INSTANCE;
	}

	@Override
	public void exportElement(
		JROdtExporterContext exporterContext,
		JRGenericPrintElement element,
		JRExporterGridCell gridCell)
	{
		try
		{
			JRPrintImage image = RasterChartImageProvider.getImage(element);
			if (image == null)
			{
				return;
			}

			TableBuilder tableBuilder = exporterContext.getTableBuilder();
			try
			{
				exportSimpleFrame(tableBuilder, element, image, gridCell);
			}
			catch (ReflectiveOperationException e)
			{
				// internal JR structures changed; use the exporter's own image
				// writing (renders in LibreOffice, but not in Word)
				JROdtExporter exporter = (JROdtExporter) exporterContext.getExporterRef();
				exporter.exportImage(tableBuilder, image, gridCell);
			}
		}
		catch (Exception e)
		{
			throw new JRRuntimeException(e);
		}
	}

	private void exportSimpleFrame(
		TableBuilder tableBuilder,
		JRGenericPrintElement element,
		JRPrintImage image,
		JRExporterGridCell gridCell) throws Exception
	{
		DocumentBuilder documentBuilder =
			(DocumentBuilder) readField(tableBuilder, "documentBuilder");
		WriterHelper bodyWriter = (WriterHelper) readField(tableBuilder, "bodyWriter");
		RenderersCache renderersCache = (RenderersCache) invoke(
			documentBuilder, "getRenderersCache", new Class<?>[0]);

		// registers the PNG bytes with the ODF package and returns its path
		String imagePath = (String) invoke(
			documentBuilder,
			"getImagePath",
			new Class<?>[] {
				Renderable.class, Dimension.class, Color.class,
				JRExporterGridCell.class, RenderersCache.class},
			image.getRenderer(),
			new Dimension(element.getWidth(), element.getHeight()),
			null,
			gridCell,
			renderersCache);

		tableBuilder.buildCellHeader(null, gridCell.getColSpan(), gridCell.getRowSpan());
		bodyWriter.write("<text:p><draw:frame draw:name=\"charteon_" + element.getUUID()
			+ "\" text:anchor-type=\"as-char\""
			+ " svg:width=\"" + inches(element.getWidth()) + "in\""
			+ " svg:height=\"" + inches(element.getHeight()) + "in\">"
			+ "<draw:image xlink:href=\"" + imagePath + "\""
			+ " xlink:type=\"simple\" xlink:show=\"embed\" xlink:actuate=\"onLoad\"/>"
			+ "</draw:frame></text:p>\n");
		tableBuilder.buildCellFooter();
	}

	private static String inches(int sizeInPoints)
	{
		// floor at 4 decimals, like the ODT exporter, so the frame never
		// exceeds the cell
		double inches = Math.floor(sizeInPoints / 72d * 10000d) / 10000d;
		return String.format(Locale.US, "%.4f", inches);
	}

	private static Object readField(Object target, String name) throws ReflectiveOperationException
	{
		Field field = findField(target.getClass(), name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static Field findField(Class<?> type, String name) throws NoSuchFieldException
	{
		for (Class<?> current = type; current != null; current = current.getSuperclass())
		{
			try
			{
				return current.getDeclaredField(name);
			}
			catch (NoSuchFieldException e)
			{
				// continue with the superclass
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static Object invoke(Object target, String name, Class<?>[] parameterTypes,
		Object... arguments) throws ReflectiveOperationException
	{
		for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass())
		{
			try
			{
				Method method = current.getDeclaredMethod(name, parameterTypes);
				method.setAccessible(true);
				return method.invoke(target, arguments);
			}
			catch (NoSuchMethodException e)
			{
				// continue with the superclass
			}
		}
		throw new NoSuchMethodException(name);
	}

	@Override
	public boolean toExport(JRGenericPrintElement element)
	{
		return true;
	}
}
