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

import java.util.HashMap;

import net.sf.jasperreports.engine.JRPropertiesMap;
import net.sf.jasperreports.engine.component.Component;
import net.sf.jasperreports.engine.component.ComponentManager;
import net.sf.jasperreports.engine.component.ComponentsBundle;
import net.sf.jasperreports.engine.component.DefaultComponentManager;
import net.sf.jasperreports.engine.component.DefaultComponentsBundle;
import net.sf.jasperreports.engine.export.GenericElementHandler;
import net.sf.jasperreports.engine.export.GenericElementHandlerBundle;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.extensions.ExtensionsRegistry;
import net.sf.jasperreports.extensions.ExtensionsRegistryFactory;
import net.sf.jasperreports.extensions.ListExtensionsRegistry;

import tech.charteon.export.ChartElementGraphics2DHandler;
import tech.charteon.export.html.ChartElementHtmlHandler;
import tech.charteon.export.pdf.ChartElementPdfHandler;
import tech.charteon.export.raster.ChartElementDocxHandler;
import tech.charteon.export.raster.ChartElementOdsHandler;
import tech.charteon.export.raster.ChartElementOdtHandler;
import tech.charteon.export.raster.ChartElementPptxHandler;
import tech.charteon.export.raster.ChartElementRtfHandler;
import tech.charteon.export.raster.ChartElementXlsxHandler;
import tech.charteon.export.text.ChartElementCsvHandler;
import tech.charteon.fill.ChartFillFactory;

/**
 * Registers the Charteon chart component and its export handlers with the
 * JasperReports extension framework. Referenced from
 * {@code jasperreports_extension.properties}.
 */
public class CharteonExtensionsRegistryFactory implements ExtensionsRegistryFactory
{
	/**
	 * The exporter key of the PDF exporter, inlined here (it is a compile-time
	 * constant of {@code net.sf.jasperreports.pdf.JRPdfExporter}) so that this
	 * factory class can be loaded without the jasperreports-pdf artifact being
	 * present on the classpath.
	 */
	private static final String PDF_EXPORTER_KEY = "net.sf.jasperreports.pdf";

	private static final GenericElementHandlerBundle HANDLER_BUNDLE =
		new GenericElementHandlerBundle()
		{
			@Override
			public String getNamespace()
			{
				return ChartComponent.ELEMENT_NAMESPACE;
			}

			@Override
			public GenericElementHandler getHandler(String elementName, String exporterKey)
			{
				if (ChartComponent.ELEMENT_NAME.equals(elementName))
				{
					if (HtmlExporter.HTML_EXPORTER_KEY.equals(exporterKey))
					{
						return ChartElementHtmlHandler.getInstance();
					}
					else if (PDF_EXPORTER_KEY.equals(exporterKey))
					{
						return ChartElementPdfHandler.getInstance();
					}
					else if (JRGraphics2DExporter.GRAPHICS2D_EXPORTER_KEY.equals(exporterKey))
					{
						return ChartElementGraphics2DHandler.getInstance();
					}
					else if (JRXlsxExporter.XLSX_EXPORTER_KEY.equals(exporterKey))
					{
						return ChartElementXlsxHandler.getInstance();
					}
					else if (JRDocxExporter.DOCX_EXPORTER_KEY.equals(exporterKey))
					{
						return ChartElementDocxHandler.getInstance();
					}
					else if (JRPptxExporter.PPTX_EXPORTER_KEY.equals(exporterKey))
					{
						return ChartElementPptxHandler.getInstance();
					}
					else if (JROdtExporter.ODT_EXPORTER_KEY.equals(exporterKey))
					{
						return ChartElementOdtHandler.getInstance();
					}
					else if (JROdsExporter.ODS_EXPORTER_KEY.equals(exporterKey))
					{
						return ChartElementOdsHandler.getInstance();
					}
					else if (JRRtfExporter.RTF_EXPORTER_KEY.equals(exporterKey))
					{
						return ChartElementRtfHandler.getInstance();
					}
					else if (JRCsvExporter.CSV_EXPORTER_KEY.equals(exporterKey))
					{
						return ChartElementCsvHandler.getInstance();
					}
				}
				return null;
			}
		};

	/**
	 * Lazy holder: the JR7 registration code is only class-loaded and run when
	 * a JR7 engine was positively detected, never on a JR6 classpath.
	 */
	private static final class RegistryHolder
	{
		static final ExtensionsRegistry REGISTRY;

		static
		{
			DefaultComponentsBundle bundle = new DefaultComponentsBundle();

			HashMap<Class<? extends Component>, ComponentManager> componentManagers = new HashMap<>();

			DefaultComponentManager chartManager = new DefaultComponentManager();
			chartManager.setDesignConverter(ChartDesignConverter.getInstance());
			chartManager.setComponentCompiler(new ChartComponentCompiler());
			chartManager.setComponentFillFactory(new ChartFillFactory());
			componentManagers.put(ChartComponent.class, chartManager);

			bundle.setComponentManagers(componentManagers);

			ListExtensionsRegistry registry = new ListExtensionsRegistry();
			registry.add(ComponentsBundle.class, bundle);
			registry.add(GenericElementHandlerBundle.class, HANDLER_BUNDLE);

			REGISTRY = registry;
		}
	}

	/**
	 * JR6-only class; if it is present, this classpath runs a JasperReports 6.x
	 * engine on which the JR7 component registration must not be activated.
	 * (Insidiously, the JR7 registration code even links under JR6 because the
	 * changed {@code setComponentManagers} map signature erases to the same
	 * raw type — producing a broken, parser-less components bundle. Hence this
	 * explicit probe instead of relying on linkage errors.)
	 */
	private static final String JR6_MARKER_CLASS =
		"net.sf.jasperreports.engine.component.DefaultComponentXmlParser";

	@Override
	public ExtensionsRegistry createRegistry(String registryId, JRPropertiesMap properties)
	{
		if (isJr6Engine())
		{
			org.apache.commons.logging.LogFactory.getLog(CharteonExtensionsRegistryFactory.class)
				.warn("Charteon: a JasperReports 6.x engine was detected; the JR7 component "
					+ "registration is skipped. Use the charteon-jr6-adapter artifact to run "
					+ "Charteon charts on JasperReports 6.x.");
			return EMPTY_REGISTRY;
		}
		return RegistryHolder.REGISTRY;
	}

	private static boolean isJr6Engine()
	{
		try
		{
			Class.forName(JR6_MARKER_CLASS, false,
				CharteonExtensionsRegistryFactory.class.getClassLoader());
			return true;
		}
		catch (ClassNotFoundException e)
		{
			return false;
		}
	}

	private static final ExtensionsRegistry EMPTY_REGISTRY = new ExtensionsRegistry()
	{
		@Override
		public <T> java.util.List<T> getExtensions(Class<T> extensionType)
		{
			return java.util.Collections.emptyList();
		}
	};
}
