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
package tech.charteon.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The evaluated data of a candlestick dataset: one OHLC tuple per category,
 * in encounter order.
 */
public class CandlestickChartData implements Serializable
{
	private static final long serialVersionUID = 1L;

	public record Candle(String category, Number open, Number close, Number low,
		Number high) implements Serializable
	{
	}

	private final List<Candle> candles = new ArrayList<>();

	public void addCandle(String category, Number open, Number close, Number low, Number high)
	{
		candles.add(new Candle(category, open, close, low, high));
	}

	public boolean isEmpty()
	{
		return candles.isEmpty();
	}

	public List<Candle> getCandles()
	{
		return candles;
	}
}
