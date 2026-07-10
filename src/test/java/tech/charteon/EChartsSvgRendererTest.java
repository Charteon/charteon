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
package tech.charteon;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import tech.charteon.export.ssr.EChartsSvgRenderer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test of the GraalJS server-side rendering path in isolation.
 */
public class EChartsSvgRendererTest
{
	@Test
	void rendersBarChartSvg() throws Exception
	{
		String option = """
			{
			  "xAxis": {"type": "category", "data": ["A", "B", "C"]},
			  "yAxis": {"type": "value"},
			  "series": [{"type": "bar", "data": [1, 2, 3]}]
			}
			""";

		String svg = EChartsSvgRenderer.renderSvg(option, 400, 300, null);
		assertNotNull(svg);

		Path outputDir = Path.of("target", "test-output");
		Files.createDirectories(outputDir);
		Files.writeString(outputDir.resolve("renderer-bar.svg"), svg);

		assertTrue(svg.startsWith("<svg"), "must be an SVG document");
		assertTrue(svg.contains("width=\"400\""), "must honor requested width");
		assertFalse(svg.contains("<style"),
			"must not contain <style> elements (Batik cannot parse them without a document URI)");
	}
}
