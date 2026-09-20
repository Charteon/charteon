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
package tech.charteon.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.jasperreports.engine.JRRuntimeException;

/**
 * The registry of GeoJSON maps available to {@code map} charts.
 *
 * <p>
 * A map is resolved by name, in this order:
 * <ol>
 * <li>maps registered programmatically via {@link #register(String, String)},</li>
 * <li>classpath resources named {@code tech/charteon/maps/<name>.geo.json}.</li>
 * </ol>
 * Charteon bundles a {@code world} map (Natural Earth 1:110m admin-0
 * countries, public domain; region names in the {@code name} property, e.g.
 * "Germany", "United States of America").
 */
public final class CharteonMaps
{
	private static final String RESOURCE_PREFIX = "tech/charteon/maps/";
	private static final String RESOURCE_SUFFIX = ".geo.json";

	private static final Map<String, String> REGISTERED = new ConcurrentHashMap<>();
	private static final Map<String, String> RESOURCE_CACHE = new ConcurrentHashMap<>();

	/**
	 * How often each name has been re-registered.
	 *
	 * <p>
	 * The renderer hands GeoJSON to a JavaScript context, and those contexts are
	 * pooled and long-lived: whatever a context has registered, it keeps. Without
	 * a way to tell one content from another, a {@link #register} that replaces a
	 * map would reach only the contexts built afterwards - so the same chart would
	 * draw the old outline or the new one depending on which context it happened
	 * to get. That is the sort of fault that shows up as "sometimes".
	 *
	 * <p>
	 * The revision travels with the GeoJSON and lets each context notice that what
	 * it holds is stale. Names that come from a classpath resource never change and
	 * stay at revision 0.
	 */
	private static final Map<String, Long> REVISIONS = new ConcurrentHashMap<>();

	private CharteonMaps()
	{
	}

	/**
	 * Registers (or replaces) a GeoJSON map under the given name, making it
	 * available to {@code map} charts through the {@code mapName} attribute.
	 *
	 * @param name the map name
	 * @param geoJson the GeoJSON FeatureCollection as a string
	 */
	public static void register(String name, String geoJson)
	{
		REGISTERED.put(name, geoJson);
		REVISIONS.merge(name, 1L, Long::sum);
	}

	/**
	 * Which revision of the named map is current.
	 *
	 * <p>
	 * 0 means the content cannot change behind the caller's back - either the
	 * name is unknown, or it resolves to a classpath resource. Every
	 * {@link #register} of a name raises its revision by one, which is what tells
	 * a renderer holding an older copy that it has to register the map again.
	 */
	public static long getRevision(String name)
	{
		if (name == null)
		{
			return 0L;
		}
		return REVISIONS.getOrDefault(name, 0L);
	}

	/**
	 * Returns the GeoJSON for the given map name, or {@code null} if the name
	 * is neither registered nor available as a classpath resource.
	 */
	public static String getGeoJson(String name)
	{
		if (name == null)
		{
			return null;
		}
		String registered = REGISTERED.get(name);
		if (registered != null)
		{
			return registered;
		}
		// avoid path traversal through crafted map names
		if (!name.matches("[\\w][\\w.-]*"))
		{
			return null;
		}
		return RESOURCE_CACHE.computeIfAbsent(name, CharteonMaps::loadResource) == NOT_FOUND
			? null
			: RESOURCE_CACHE.get(name);
	}

	private static final String NOT_FOUND = new String("\0charteon-map-not-found");

	private static String loadResource(String name)
	{
		String resource = RESOURCE_PREFIX + name + RESOURCE_SUFFIX;
		ClassLoader classLoader = CharteonMaps.class.getClassLoader();
		try (InputStream in = classLoader.getResourceAsStream(resource))
		{
			if (in == null)
			{
				return NOT_FOUND;
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new JRRuntimeException(e);
		}
	}
}
