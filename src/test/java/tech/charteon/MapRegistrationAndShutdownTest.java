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

import org.junit.jupiter.api.Test;

import tech.charteon.export.ssr.EChartsSvgRenderer;
import tech.charteon.util.CharteonMaps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two things a pooled, long-lived JavaScript runtime gets wrong if nobody
 * looks: content that is registered once and never refreshed, and a runtime
 * that is never released.
 *
 * <p>
 * Both are invisible in ordinary use. A map is usually registered once at
 * start-up, and a JVM that exits takes the runtime with it - so neither fault
 * shows up until somebody replaces a map at run time, or stops and starts a
 * plug-in inside a host that keeps running.
 */
public class MapRegistrationAndShutdownTest
{
	/** One square. */
	private static final String SQUARE = """
		{"type":"FeatureCollection","features":[{"type":"Feature",
		 "properties":{"name":"Alpha"},
		 "geometry":{"type":"Polygon",
		  "coordinates":[[[0,0],[10,0],[10,10],[0,10],[0,0]]]}}]}
		""";

	/** A triangle, under the same name - deliberately a different shape. */
	private static final String TRIANGLE = """
		{"type":"FeatureCollection","features":[{"type":"Feature",
		 "properties":{"name":"Alpha"},
		 "geometry":{"type":"Polygon",
		  "coordinates":[[[0,0],[20,0],[10,20],[0,0]]]}}]}
		""";

	private static String mapOption(String mapName)
	{
		return "{\"series\":[{\"type\":\"map\",\"map\":\"" + mapName
			+ "\",\"data\":[{\"name\":\"Alpha\",\"value\":1}]}]}";
	}

	/**
	 * The drawn outlines, with the noise removed.
	 *
	 * <p>
	 * ECharts numbers the CSS classes of every chart instance it creates
	 * ({@code zr0-cls-1}, {@code zr1-cls-2}, ...), so two renders of the very
	 * same option are never byte-identical - comparing whole documents would
	 * pass no matter what was drawn. What is compared here is the path geometry
	 * alone, which is what the map actually decides.
	 */
	private static String outlines(String svg)
	{
		StringBuilder paths = new StringBuilder();
		java.util.regex.Matcher m =
			java.util.regex.Pattern.compile("\\sd=\"([^\"]+)\"").matcher(svg);
		while (m.find())
		{
			paths.append(m.group(1)).append('\n');
		}
		return paths.toString();
	}

	/**
	 * Replacing a map has to reach the renderer.
	 *
	 * <p>
	 * {@code CharteonMaps.register} says it registers "or replaces". The
	 * JavaScript contexts are pooled and outlive any one chart, so a context that
	 * already knows the name used to skip the new content silently - and which
	 * context a chart got decided which outline it drew. This is that case: the
	 * same option, the same map name, different geometry, and the second render
	 * must not repeat the first.
	 */
	@Test
	void replacingAMapChangesWhatIsDrawn()
	{
		String name = "charteon-test-shape";

		CharteonMaps.register(name, SQUARE);
		long first = CharteonMaps.getRevision(name);
		String square = EChartsSvgRenderer.renderSvg(mapOption(name), 400, 300, null, name);

		CharteonMaps.register(name, TRIANGLE);
		long second = CharteonMaps.getRevision(name);
		String triangle = EChartsSvgRenderer.renderSvg(mapOption(name), 400, 300, null, name);

		assertTrue(second > first, "a replacement raises the revision");
		assertTrue(square.startsWith("<svg") && triangle.startsWith("<svg"));
		assertFalse(outlines(square).isBlank(), "the map has to draw something at all");
		assertNotEquals(outlines(square), outlines(triangle),
			"the replaced map must be drawn; the same outlines mean the context kept the old one");
	}

	/** A map that never changes stays at revision 0 and is registered once. */
	@Test
	void aMapFromTheClasspathHasNoRevision()
	{
		assertEquals(0L, CharteonMaps.getRevision("world"));
		assertEquals(0L, CharteonMaps.getRevision("no-such-map-anywhere"));
		assertEquals(0L, CharteonMaps.getRevision(null));
	}

	/**
	 * The runtime can be released and is rebuilt on the next render.
	 *
	 * <p>
	 * That second half is what makes it usable from a bundle activator: stopping
	 * the plug-in must not leave the library permanently broken for a host that
	 * starts it again.
	 */
	@Test
	void theRuntimeCanBeReleasedAndComesBack()
	{
		String option = "{\"series\":[{\"type\":\"bar\",\"data\":[1,2,3]}],"
			+ "\"xAxis\":{\"type\":\"category\",\"data\":[\"a\",\"b\",\"c\"]},"
			+ "\"yAxis\":{\"type\":\"value\"}}";

		assertTrue(EChartsSvgRenderer.renderSvg(option, 200, 150, null).startsWith("<svg"));

		EChartsSvgRenderer.shutdown();

		assertTrue(EChartsSvgRenderer.renderSvg(option, 200, 150, null).startsWith("<svg"),
			"a render after shutdown builds the runtime again");
	}

	/** Releasing twice, or releasing what was never built, is not an error. */
	@Test
	void shutdownIsSafeToRepeat()
	{
		EChartsSvgRenderer.shutdown();
		EChartsSvgRenderer.shutdown();
	}
}
