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
package tech.charteon.export.ssr;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import net.sf.jasperreports.engine.JRRuntimeException;

import tech.charteon.util.CharteonMaps;

/**
 * Renders Apache ECharts options to SVG strings, server-side, using GraalVM
 * JavaScript — no browser, no DOM, no canvas.
 *
 * <p>
 * Contexts are expensive to create (ECharts is ~1 MB of JavaScript), so a
 * fixed-size pool of contexts is kept; each context loads ECharts once and is
 * reused for all subsequent charts. GraalVM contexts are single-threaded,
 * which is why access is pooled rather than shared.
 */
public final class EChartsSvgRenderer
{
	private static final String ECHARTS_RESOURCE = "tech/charteon/echarts.min.js";
	private static final String BOOTSTRAP_RESOURCE = "tech/charteon/charteon-ssr.js";

	private static final int POOL_SIZE =
		Math.max(1, Math.min(4, java.lang.Runtime.getRuntime().availableProcessors() / 2));

	/** How long {@link #shutdown()} waits for renders in flight before closing anyway. */
	private static final int SHUTDOWN_WAIT_SECONDS = 10;

	/**
	 * Bootstrapped GraalVM state: the shared engine (so parsed/compiled code is
	 * shared between contexts), the two JS sources and the context pool.
	 */
	private static final class GraalRuntime
	{
		final Engine engine;
		final Source echartsSource;
		final Source bootstrapSource;
		final ConcurrentLinkedQueue<Context> pool = new ConcurrentLinkedQueue<>();
		final Semaphore permits = new Semaphore(POOL_SIZE);

		GraalRuntime()
		{
			this.engine = Engine.newBuilder()
				.option("engine.WarnInterpreterOnly", "false")
				.build();
			this.echartsSource = loadSource(ECHARTS_RESOURCE);
			this.bootstrapSource = loadSource(BOOTSTRAP_RESOURCE);
		}
	}

	/** Lazily built once on first render. */
	private static volatile GraalRuntime runtime;

	/**
	 * The very first bootstrap failure, cached forever. GraalVM's own
	 * {@code Engine$ImplHolder} static initializer is poisoned process-wide after
	 * its first failure — every later touch throws only the secondary, causeless
	 * {@code NoClassDefFoundError: Could not initialize class ...Engine$ImplHolder}.
	 * We keep the ORIGINAL throwable (an {@code ExceptionInInitializerError} whose
	 * cause is the true root) so it can be reported on every subsequent call.
	 */
	private static volatile Throwable initFailure;

	private EChartsSvgRenderer()
	{
	}

	/**
	 * Returns the shared runtime, building it on first use. Unlike a static
	 * holder, a build failure is captured and rethrown (with its real cause
	 * chained) on every call rather than being masked forever by the JVM as an
	 * opaque {@code "Could not initialize class ...$Holder"}.
	 *
	 * <p>GraalVM Polyglot discovers its implementation, the Truffle runtime and
	 * the {@code "js"} language through the {@link java.util.ServiceLoader},
	 * which consults the <em>thread context</em> class loader. Inside an OSGi
	 * host such as Jaspersoft Studio the context class loader at fill time is the
	 * platform (Equinox) loader, which cannot see the polyglot/Truffle classes
	 * embedded in this bundle. The context class loader is therefore pinned to
	 * this class's loader (the bundle class loader) for the whole bootstrap.</p>
	 */
	private static GraalRuntime runtime()
	{
		GraalRuntime local = runtime;
		if (local != null)
		{
			return local;
		}
		synchronized (EChartsSvgRenderer.class)
		{
			if (runtime != null)
			{
				return runtime;
			}
			if (initFailure != null)
			{
				// Report the ORIGINAL root cause, not the later poisoned-holder echo.
				throw bootstrapFailure(initFailure);
			}
			Thread thread = Thread.currentThread();
			ClassLoader previous = thread.getContextClassLoader();
			try
			{
				thread.setContextClassLoader(EChartsSvgRenderer.class.getClassLoader());
				runtime = new GraalRuntime();
				return runtime;
			}
			catch (Throwable t)
			{
				initFailure = t;
				throw bootstrapFailure(t);
			}
			finally
			{
				thread.setContextClassLoader(previous);
			}
		}
	}

	private static JRRuntimeException bootstrapFailure(Throwable t)
	{
		Throwable root = t;
		while (root.getCause() != null && root.getCause() != root)
		{
			root = root.getCause();
		}
		return new JRRuntimeException("Charteon: could not initialise the GraalVM JavaScript"
			+ " runtime for server-side chart rendering. Root cause: " + root, t);
	}

	/**
	 * Renders the given ECharts option JSON to an SVG document string.
	 *
	 * @param optionJson the ECharts option object as JSON string
	 * @param width the target width in pixels/points
	 * @param height the target height in pixels/points
	 * @param theme the ECharts theme name, or {@code null} for the default
	 */
	public static String renderSvg(String optionJson, int width, int height, String theme)
	{
		return renderSvg(optionJson, width, height, theme, null);
	}

	/**
	 * Renders the given ECharts option JSON to an SVG document string,
	 * registering the named GeoJSON map (resolved through
	 * {@link CharteonMaps}) first — required for {@code map} charts.
	 */
	public static String renderSvg(String optionJson, int width, int height, String theme,
		String mapName)
	{
		String mapGeoJson = null;
		long mapRevision = 0L;
		if (mapName != null)
		{
			mapGeoJson = CharteonMaps.getGeoJson(mapName);
			mapRevision = CharteonMaps.getRevision(mapName);
			if (mapGeoJson == null)
			{
				throw new JRRuntimeException("Charteon: no GeoJSON found for map \"" + mapName
					+ "\"; register it via CharteonMaps.register(...) or provide a classpath"
					+ " resource tech/charteon/maps/" + mapName + ".geo.json");
			}
		}

		GraalRuntime rt = runtime();
		rt.permits.acquireUninterruptibly();
		try
		{
			Context context = rt.pool.poll();
			if (context == null)
			{
				context = createContext(rt);
			}
			try
			{
				if (mapGeoJson != null)
				{
					Value registerMap = context.getBindings("js").getMember("__charteonRegisterMap");
					// The revision goes along so that a context still holding an
					// earlier version of this map registers the new one.
					registerMap.execute(mapName, mapGeoJson, mapRevision);
				}
				Value render = context.getBindings("js").getMember("__charteonRender");
				Value svg = render.execute(optionJson, width, height, theme);
				return svg.asString();
			}
			finally
			{
				rt.pool.offer(context);
			}
		}
		catch (RuntimeException e)
		{
			throw new JRRuntimeException("Charteon: server-side chart rendering failed", e);
		}
		finally
		{
			rt.permits.release();
		}
	}

	/**
	 * Releases the JavaScript runtime: every pooled context and the engine
	 * behind them.
	 *
	 * <p>
	 * Nothing here is reclaimed on its own. The engine and its contexts are
	 * reachable from a static field, each context holds roughly a megabyte of
	 * parsed ECharts, and all of it is anchored to the class loader of this
	 * class. In a plain application that lives until the JVM exits and costs
	 * nothing. In an OSGi host such as Jaspersoft Studio the bundle can be
	 * stopped and started again while the JVM keeps running - and then the old
	 * bundle's class loader cannot be collected, because Truffle still points at
	 * it. That is the usual way a plug-in leaks a whole class loader per restart.
	 *
	 * <p>
	 * So a bundle activator calls this from its {@code stop}. It is safe to call
	 * more than once, safe to call when nothing was ever rendered, and a
	 * {@link #renderSvg} afterwards simply builds the runtime again.
	 *
	 * <p>
	 * A render in flight is waited for, briefly: whoever is rendering holds a
	 * permit, and this waits for all of them. If a render does not finish within
	 * that time the engine is closed anyway and the running script is cancelled -
	 * a shutdown that can be blocked indefinitely by one stuck chart would be no
	 * shutdown at all.
	 */
	public static void shutdown()
	{
		GraalRuntime local;
		synchronized (EChartsSvgRenderer.class)
		{
			local = runtime;
			runtime = null;
			// A failure belonged to the runtime that has just been dropped; the
			// next attempt deserves to be judged on its own.
			initFailure = null;
		}
		if (local == null)
		{
			return;
		}
		try
		{
			// Whoever renders holds a permit, so holding all of them means nobody
			// is. The result is deliberately not acted on: on a timeout we close
			// anyway, and closing cancels what is still running.
			local.permits.tryAcquire(POOL_SIZE, SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		for (Context context = local.pool.poll(); context != null; context = local.pool.poll())
		{
			closeQuietly(context);
		}
		try
		{
			// true: cancel anything still executing. Closing the engine also
			// closes contexts that a slow render has not handed back.
			local.engine.close(true);
		}
		catch (RuntimeException alreadyGone)
		{
			// Closing twice, or closing what never opened, is not a failure here.
		}
	}

	private static void closeQuietly(Context context)
	{
		try
		{
			context.close(true);
		}
		catch (RuntimeException alreadyGone)
		{
			// As above: shutdown reports nothing, it only releases.
		}
	}

	private static Context createContext(GraalRuntime rt)
	{
		// Same OSGi reason as in runtime(): creating a "js" context resolves the
		// language through the context class loader, so pin it to our loader.
		Thread thread = Thread.currentThread();
		ClassLoader previous = thread.getContextClassLoader();
		try
		{
			thread.setContextClassLoader(EChartsSvgRenderer.class.getClassLoader());
			Context context = Context.newBuilder("js")
				.engine(rt.engine)
				.allowExperimentalOptions(false)
				.build();
			// the bootstrap installs environment shims and must run before ECharts
			context.eval(rt.bootstrapSource);
			context.eval(rt.echartsSource);
			return context;
		}
		finally
		{
			thread.setContextClassLoader(previous);
		}
	}

	private static Source loadSource(String resource)
	{
		ClassLoader classLoader = EChartsSvgRenderer.class.getClassLoader();
		try (InputStream in = classLoader.getResourceAsStream(resource))
		{
			if (in == null)
			{
				throw new JRRuntimeException("Charteon: resource not found: " + resource);
			}
			String code = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			return Source.newBuilder("js", code, resource).buildLiteral();
		}
		catch (IOException e)
		{
			throw new JRRuntimeException(e);
		}
	}
}
