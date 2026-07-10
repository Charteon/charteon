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
package tech.charteon.export.html;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.export.GenericElementHtmlHandler;
import net.sf.jasperreports.engine.export.JRHtmlExporterContext;

import tech.charteon.component.ChartComponent;
import tech.charteon.util.CharteonMaps;

/**
 * HTML export handler. Embeds the bundled Apache ECharts library once per
 * exported report and initializes each chart in an interactive
 * {@code <div>} — tooltips, legend toggling and zooming keep working in the
 * exported HTML, which raster chart images cannot offer.
 */
public class ChartElementHtmlHandler implements GenericElementHtmlHandler
{
	private static final String ECHARTS_RESOURCE = "tech/charteon/echarts.min.js";

	private static final ChartElementHtmlHandler INSTANCE = new ChartElementHtmlHandler();

	public static ChartElementHtmlHandler getInstance()
	{
		return INSTANCE;
	}

	private static volatile String echartsLibrary;

	private static final AtomicLong ELEMENT_COUNTER = new AtomicLong();

	/**
	 * Tracks the exporter contexts into which the ECharts library has already
	 * been written, so it is embedded only once per exported report.
	 */
	private static final Set<JRHtmlExporterContext> LIBRARY_EMITTED =
		Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	/**
	 * Tracks which GeoJSON maps have already been registered per exporter
	 * context, so each map is embedded only once per exported report.
	 */
	private static final Map<JRHtmlExporterContext, Set<String>> MAPS_EMITTED =
		Collections.synchronizedMap(new WeakHashMap<>());

	/**
	 * Revives {@code "js:function..."} strings of the option into functions
	 * (custom series renderItem, formatter callbacks). Emitted once together
	 * with the ECharts library.
	 */
	private static final String REVIVE_SCRIPT =
		"function __charteonRevive(n){"
		+ "if(typeof n==='string'){return n.indexOf('js:')===0?(0,eval)('('+n.substring(3)+')'):n;}"
		+ "if(Array.isArray(n)){for(var i=0;i<n.length;i++){n[i]=__charteonRevive(n[i]);}return n;}"
		+ "if(n!==null&&typeof n==='object'){for(var k in n){if(Object.prototype.hasOwnProperty.call(n,k)){n[k]=__charteonRevive(n[k]);}}return n;}"
		+ "return n;}";

	@Override
	public String getHtmlFragment(JRHtmlExporterContext exporterContext, JRGenericPrintElement element)
	{
		String optionJson = (String) element.getParameterValue(ChartComponent.PARAMETER_OPTION);
		if (optionJson == null || optionJson.isBlank())
		{
			return "";
		}
		String theme = (String) element.getParameterValue(ChartComponent.PARAMETER_THEME);

		StringBuilder html = new StringBuilder(1024);

		if (LIBRARY_EMITTED.add(exporterContext))
		{
			html.append("<!-- charteon:echarts-library -->\n")
				.append("<script>\n")
				.append(getEchartsLibrary())
				.append("\n").append(REVIVE_SCRIPT).append("\n")
				.append("</script>\n");
		}

		String mapName = (String) element.getParameterValue(ChartComponent.PARAMETER_MAP_NAME);
		if (mapName != null)
		{
			Set<String> emittedMaps = MAPS_EMITTED.computeIfAbsent(exporterContext,
				k -> Collections.synchronizedSet(new java.util.HashSet<>()));
			if (emittedMaps.add(mapName))
			{
				String geoJson = CharteonMaps.getGeoJson(mapName);
				if (geoJson == null)
				{
					throw new JRRuntimeException("Charteon: no GeoJSON found for map \"" + mapName
						+ "\"; register it via CharteonMaps.register(...) or provide a classpath"
						+ " resource tech/charteon/maps/" + mapName + ".geo.json");
				}
				html.append("<!-- charteon:geo-map ").append(mapName).append(" -->\n")
					.append("<script>\n")
					.append("echarts.registerMap(").append(quoteJsString(mapName)).append(", ")
					.append(scriptSafe(geoJson)).append(");\n")
					.append("</script>\n");
			}
		}

		String divId = "charteon_" + element.getUUID() + "_" + ELEMENT_COUNTER.incrementAndGet();
		int width = element.getWidth();
		int height = element.getHeight();

		html.append("<div id=\"").append(divId).append("\" style=\"width:")
			.append(width).append("px;height:").append(height).append("px;\"></div>\n");
		html.append("<script>\n")
			.append("(function(){\n")
			.append("var chart = echarts.init(document.getElementById('").append(divId).append("')");
		if (theme != null && !theme.isBlank())
		{
			html.append(", ").append(quoteJsString(theme));
		}
		html.append(");\n")
			.append("chart.setOption(__charteonRevive(").append(scriptSafe(optionJson)).append("));\n")
			.append("})();\n")
			.append("</script>\n");

		return html.toString();
	}

	@Override
	public boolean toExport(JRGenericPrintElement element)
	{
		return true;
	}

	private static String getEchartsLibrary()
	{
		String library = echartsLibrary;
		if (library == null)
		{
			synchronized (ChartElementHtmlHandler.class)
			{
				library = echartsLibrary;
				if (library == null)
				{
					try (InputStream in = ChartElementHtmlHandler.class.getClassLoader()
						.getResourceAsStream(ECHARTS_RESOURCE))
					{
						if (in == null)
						{
							throw new JRRuntimeException(
								"Charteon: resource not found: " + ECHARTS_RESOURCE);
						}
						library = new String(in.readAllBytes(), StandardCharsets.UTF_8);
					}
					catch (IOException e)
					{
						throw new JRRuntimeException(e);
					}
					echartsLibrary = library;
				}
			}
		}
		return library;
	}

	/**
	 * The option JSON is emitted inside a script block; a literal
	 * {@code </script>} inside string values would terminate it early.
	 */
	private static String scriptSafe(String json)
	{
		return json.replace("</", "<\\/");
	}

	private static String quoteJsString(String value)
	{
		return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
	}
}
