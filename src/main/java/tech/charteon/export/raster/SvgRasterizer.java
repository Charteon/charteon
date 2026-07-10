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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import net.sf.jasperreports.engine.JRRuntimeException;

/**
 * Rasterizes SVG documents to PNG via the Apache Batik transcoder. Used as
 * fallback for export formats that cannot embed vector graphics
 * (XLSX/DOCX/PPTX). The image is rendered at a scale factor above 1 so it
 * stays crisp on high-density displays.
 */
public final class SvgRasterizer
{
	/**
	 * Supersampling factor for the raster fallback (3x for retina-class
	 * quality).
	 */
	public static final float SCALE_FACTOR = 3f;

	private SvgRasterizer()
	{
	}

	public static byte[] toPng(byte[] svg, int width, int height)
	{
		try
		{
			PNGTranscoder transcoder = new PNGTranscoder();
			transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, width * SCALE_FACTOR);
			transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, height * SCALE_FACTOR);

			TranscoderInput input = new TranscoderInput(new ByteArrayInputStream(svg));
			ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 16);
			transcoder.transcode(input, new TranscoderOutput(out));
			return out.toByteArray();
		}
		catch (TranscoderException e)
		{
			throw new JRRuntimeException("Charteon: SVG to PNG transcoding failed", e);
		}
	}
}
