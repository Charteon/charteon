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

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Helpers for validating ZIP-container based export formats (OOXML, ODF).
 */
public final class ZipTestUtil
{
	private ZipTestUtil()
	{
	}

	public static List<String> entryNames(File file) throws Exception
	{
		List<String> names = new ArrayList<>();
		try (ZipFile zip = new ZipFile(file))
		{
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements())
			{
				names.add(entries.nextElement().getName());
			}
		}
		return names;
	}

	public static long countEntries(File file, String prefix) throws Exception
	{
		return entryNames(file).stream().filter(name -> name.startsWith(prefix)).count();
	}
}
