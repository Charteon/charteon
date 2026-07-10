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
package tech.charteon.export.text;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.jasperreports.engine.JRGenericPrintElement;

import tech.charteon.component.ChartComponent;

/**
 * Builds the textual fallback used by text-based export formats (CSV), which
 * cannot display an image: the chart title followed by the underlying data
 * values, extracted from the generated ECharts option.
 */
public final class ChartTextSummary
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private ChartTextSummary()
	{
	}

	/**
	 * Returns the summary text for the element, or {@code null} when the
	 * element carries no chart option.
	 */
	public static String getSummary(JRGenericPrintElement element)
	{
		String optionJson = (String) element.getParameterValue(ChartComponent.PARAMETER_OPTION);
		if (optionJson == null || optionJson.isBlank())
		{
			return null;
		}

		try
		{
			JsonNode option = MAPPER.readTree(optionJson);
			StringBuilder text = new StringBuilder();

			JsonNode title = option.path("title").path("text");
			if (title.isTextual())
			{
				text.append(title.asText());
			}
			else
			{
				String chartType = (String) element.getParameterValue(ChartComponent.PARAMETER_CHART_TYPE);
				text.append(chartType == null ? "chart" : chartType + " chart");
			}

			JsonNode categories = option.path("xAxis").path("data");

			for (JsonNode series : option.path("series"))
			{
				text.append(" | ");
				JsonNode name = series.path("name");
				if (name.isTextual() && !name.asText().isEmpty())
				{
					text.append(name.asText()).append(": ");
				}
				appendData(text, series.path("data"), categories);
				if (series.has("links"))
				{
					appendLinks(text, series.path("links"));
				}
			}

			return text.toString();
		}
		catch (Exception e)
		{
			// the fallback must never break a text export
			return "[chart]";
		}
	}

	private static void appendData(StringBuilder text, JsonNode data, JsonNode categories)
	{
		int count = 0;
		for (int i = 0; i < data.size(); i++)
		{
			JsonNode item = data.get(i);
			if (count > 0)
			{
				text.append(", ");
			}
			if (item.isObject())
			{
				JsonNode name = item.path("name");
				JsonNode value = item.path("value");
				if (name.isTextual())
				{
					text.append(name.asText()).append('=');
				}
				appendValue(text, value.isMissingNode() ? item : value);
			}
			else
			{
				if (categories.isArray() && i < categories.size())
				{
					text.append(categories.get(i).asText()).append('=');
				}
				appendValue(text, item);
			}
			count++;
		}
	}

	private static void appendLinks(StringBuilder text, JsonNode links)
	{
		for (int i = 0; i < links.size(); i++)
		{
			JsonNode link = links.get(i);
			if (i > 0 || text.charAt(text.length() - 1) != ' ')
			{
				text.append(", ");
			}
			text.append(link.path("source").asText())
				.append(" > ")
				.append(link.path("target").asText());
			JsonNode value = link.path("value");
			if (value.isNumber())
			{
				text.append('=').append(value.asText());
			}
		}
	}

	private static void appendValue(StringBuilder text, JsonNode value)
	{
		if (value.isArray())
		{
			text.append('[');
			for (int i = 0; i < value.size(); i++)
			{
				if (i > 0)
				{
					text.append(' ');
				}
				text.append(value.get(i).asText());
			}
			text.append(']');
		}
		else
		{
			text.append(value.asText());
		}
	}
}
