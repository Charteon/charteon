# Charteon Architecture

This document explains how Charteon plugs into the JasperReports Library, how
the rendering pipelines work, and which design decisions were made along the
way.

## Overview

```
JRXML                       fill time                       export time
─────                       ─────────                       ───────────
<component kind="chart">    ChartFillComponent              per-format handlers
  categoryDataset      ──►  evaluates expressions      ──►  PDF : GraalJS SSR → SVG → Batik → vector
  xyDataset                 per record, builds the          HTML: <div> + echarts.init() (interactive)
  hierarchyDataset          ECharts option JSON,            G2D : SVG → Batik → Graphics2D
  relationDataset           stores it on a                  XLSX/DOCX/PPTX/ODT/ODS/RTF: SVG → PNG (3x)
  boxplotDataset            JRGenericPrintElement           CSV : data fallback (title + values)
  candlestickDataset                                        XML : native parameter serialization
  optionExpression
```

The key architectural decision: **the fill phase never renders.** It only
evaluates expressions and produces the ECharts option object (JSON) as a
parameter of a `JRGenericPrintElement`. Rendering happens later, in
exporter-specific generic element handlers. This is the same separation the
JasperReports engine uses for its own generic elements and keeps every export
format free to choose its optimal representation.

## Extension registration

Charteon registers through the standard JasperReports extension point: a
`jasperreports_extension.properties` file at the JAR root points to
`tech.charteon.component.CharteonExtensionsRegistryFactory`, which contributes
two extensions:

1. a `ComponentsBundle` that maps the `ChartComponent` class to its
   `ComponentManager` (compiler, fill factory, design converter), and
2. a `GenericElementHandlerBundle` that maps the generic print element
   (namespace `http://charteon.tech/charts`, name `chart`) to one handler per
   exporter key (PDF, HTML, Graphics2D, XLSX, DOCX, PPTX, ODT, ODS, RTF, CSV).
   The print-XML exporter needs no handler (see the export strategy below),
   and the print-service exporter renders through the Graphics2D pipeline, so
   the Graphics2D handler covers it.

The factory is plain Java (programmatic registration). The Spring-based
registration style that older JasperReports 6.x samples used is gone in the
7.x line; the programmatic `ExtensionsRegistryFactory` is the supported,
dependency-free way.

### Why there is no XML namespace / XSD

JasperReports 7 replaced the Digester-based JRXML parser with a
Jackson-XML-based loader. Component elements are no longer namespaced XML
islands validated by a component-supplied XSD; instead, a component appears as
`<component kind="chart">` and its content is deserialized directly into the
Jackson-annotated component class. The `kind` discriminator is the component's
`@JsonTypeName`, registered automatically from the `ComponentsBundle`.

Consequently, Charteon does not (cannot) hook an XSD into report parsing. The
file `src/main/resources/charteon-component.xsd` documents the component's
structure for humans and IDE tooling, but it is informational only.

One consequence worth knowing: component kind names share one flat namespace
per Jackson base type. Charteon deliberately claims the natural name `chart`
for minimal friction. Should a future JasperReports version introduce a
built-in *component* of the same name (the built-in `chart` *element kind* of
the `jasperreports-charts` artifact is a different Jackson base type and does
not conflict), the two extensions could not be loaded side by side; the name
would then be made configurable.

## Compile phase

`ChartComponentCompiler`

- collects every expression (title, subtitle, raw option, all series
  expressions) so they are compiled exactly like the expressions of native
  report elements — including sub-dataset scoping: series expressions are
  collected through `collector.getCollector(dataset)`, which resolves them in
  the context of the dataset run's dataset;
- verifies the component at compile time: a `chartType` or `optionExpression`
  must be present, typed chart types must have the matching dataset kind with
  at least one series, raw-only types without an `optionExpression` are
  rejected, and evaluation time rules (`Auto` unsupported, `Group` requires an
  existing group) match those of native charts;
- produces the compiled component via a copy constructor that runs all
  expressions through `JRBaseObjectFactory`.

## Fill phase

`ChartFillComponent` (a `BaseFillComponent`) owns up to six fill datasets
(`FillCategoryDataset`, `FillXyDataset`, `FillHierarchyDataset`,
`FillRelationDataset`, `FillBoxplotDataset`, `FillCandlestickDataset`), all
extending JasperReports' `JRFillElementDataset`. The engine drives them per
record
(`customEvaluate`/`customIncrement`), exactly like the datasets of native
charts — dataset runs, reset/increment types and `incrementWhenExpression`
all behave as usual.

When the element is evaluated (including delayed evaluation via
`evaluationTime="Report|Page|Group"`), the accumulated data is handed to
`EChartsOptionBuilder`, which generates the ECharts option object. If an
`optionExpression` is present, its JSON is deep-merged over the generated
option (raw values win, arrays are replaced). The resulting JSON string is
stored on the `JRGenericPrintElement`.

## PDF pipeline (vector)

`ChartElementPdfHandler` runs the option JSON through the server-side
renderer and hands the resulting SVG to the PDF exporter:

1. **GraalJS SSR.** `EChartsSvgRenderer` keeps a pool of GraalVM polyglot
   contexts (they are single-threaded and expensive to build). Each context
   loads a small bootstrap plus the bundled `echarts.min.js` once and is then
   reused. Rendering calls
   `echarts.init(null, theme, {renderer:'svg', ssr:true, width, height})`
   followed by `renderToSVGString()` — ECharts' official server-side rendering
   API, which needs neither DOM nor canvas.
2. **SVG → PDF vector.** The SVG bytes are wrapped in a JasperReports
   `SimpleDataRenderer` on a print image and passed to
   `JRPdfExporter.exportImage(...)`. JasperReports detects the SVG payload,
   bridges it through Apache Batik to `Graphics2D`, and the PDF exporter's
   `PdfGraphics2D` writes native vector operations into the PDF content
   stream. No rasterization: text, axes and shapes stay sharp at every zoom
   level.

Because the SVG is rendered at exactly the element's width × height, no
scaling or aspect-ratio distortion can occur downstream.

### Why GraalJS and not headless Chromium

- no external process or binary to install and babysit — plain JVM classpath;
- deterministic, low latency per chart (contexts are pooled; ECharts is parsed
  once per context);
- the community edition is UPL-licensed and compatible with LGPL distribution;
- ECharts' `ssr: true` mode is explicitly designed for DOM-less environments,
  so a full browser buys nothing for static output.

A Chromium-based fallback would only be worth considering for exotic cases
that hard-require canvas rasterization inside the chart (see limitations).

### SSR environment notes

- `setTimeout`/`requestAnimationFrame` are stubbed as no-ops in the bootstrap.
  The SSR pass is fully synchronous; scheduled callbacks must never run
  (running them inline would recurse through zrender's frame loop).
- Text measurement without a canvas uses zrender's built-in width estimation.
  It is good enough that labels and legends do not overlap in practice, but it
  is an approximation; extremely tight layouts with unusual fonts may measure
  a few pixels off. The HTML export is unaffected (real browser measurement).
- ECharts emits a `<style>` element containing `:hover`/`cursor` rules into
  its SVG. Batik cannot parse style sheets in documents that have no base URI
  (JasperReports creates the SVG document from bytes with a `null` URI), so
  the bootstrap strips these style elements — they only affect interactive
  use and are meaningless in print output.

### SSR special cases per series type

- **`graph` layout.** The ECharts force layout settles over animation frames,
  which the synchronous SSR pass never runs — a force graph would render with
  its initial random positions. Charteon therefore defaults `graph` to the
  deterministic `circular` layout, which renders identically in PDF and HTML.
  `graphLayout="force"` is available (Charteon sets
  `force.layoutAnimation=false`, so ECharts computes the layout in one
  synchronous step); it works, but expect the HTML export to fine-tune node
  positions interactively while the PDF shows the one-shot result.
- **`themeRiver` axis.** The themeRiver layout does not support a category
  single axis (it produces NaN transforms, which break SVG consumers).
  Charteon maps the categories to numeric axis positions and installs a
  revived label formatter that displays the original category names.
- **`effectScatter` ripples** are an animation; the PDF/raster output shows
  the static symbols, the HTML export shows the live effect.
- **`custom` series** require a `renderItem` callback, which JSON cannot
  carry — see function revival below.

## Function revival ("js:" strings)

The option travels as JSON, which cannot represent functions — but some
ECharts features (the `custom` series' `renderItem`, callback formatters) are
functions by nature. Charteon revives them: any string value in the option
that starts with `js:` is evaluated into a function before `setOption`, both
in the GraalJS SSR context (PDF/raster) and in the browser (HTML export).

```json
{ "series": { "renderItem": "js:function(params, api) { ... }" } }
```

The revived code originates from the report template and runs with exactly
the same trust level as any other report expression — it is report-author
code, not end-user input. Note the deep-merge convenience rule: a raw-option
*object* merged over the generated series *array* is merged into every
series element, so `renderItem` (or any per-series option) can be injected
without replacing the generated data.

## Geo maps (GeoJSON)

`map` charts need a registered GeoJSON map. Resolution order
(`tech.charteon.util.CharteonMaps`):

1. maps registered programmatically: `CharteonMaps.register("mymap", geoJson)`;
2. classpath resources `tech/charteon/maps/<name>.geo.json`.

Charteon bundles `world` (Natural Earth 1:110m admin-0 countries, public
domain, ~250 KB; region names in the `name` property, e.g. "Germany",
"United States of America"). The SSR bootstrap registers a map once per
GraalJS context; the HTML handler embeds `echarts.registerMap(...)` once per
exported report and map. Border representations in the bundled map follow the
Natural Earth source; ship your own GeoJSON under a different `mapName` if
you need different boundaries or higher resolution.

## HTML pipeline (interactive)

`ChartElementHtmlHandler` embeds the bundled `echarts.min.js` once per
exported report (tracked per exporter context), then emits per chart a sized
`<div>` plus an init script that calls `echarts.init(...)` and
`setOption(...)` with the stored option JSON. Charts remain fully interactive:
tooltips, legend toggling, highlighting. The option JSON is emitted
script-safely (`</` escaped).

## Export format strategy

One principle drives the per-format handlers: **the best representation each
format can carry.**

| Representation | Formats | Why |
|---|---|---|
| Interactive vector | HTML | ECharts runs live in the browser — tooltips, legend toggling, zoom. |
| Static vector | PDF, Graphics2D (Swing viewer, printing, print service) | Sharp at any zoom; SVG bridges natively to `PdfGraphics2D`/`Graphics2D` via Batik. |
| High-resolution raster | XLSX, DOCX, PPTX, ODT, ODS, RTF | These exporters embed bitmap images only; JasperReports' OOXML/ODF/RTF writers have no reliable vector (SVG/EMF) embedding path, so Charteon renders the SVG to PNG at **3× supersampling** (`SvgRasterizer`, Batik `PNGTranscoder`) and scales it back to the element size — crisp instead of pixelated, aspect ratio preserved by construction. |
| Data fallback | CSV | A text grid cannot show an image; dropping the chart would silently lose information. Charteon exports the chart *data* instead: the title followed by `name=value` pairs (including sankey/graph links). |
| Native serialization | JR print XML | `JRXmlExporter` serializes generic elements with their parameters natively; the chart payload is a JSON string, so it roundtrips losslessly and renders again after `JRPrintXmlLoader.load(...)`. |
| Not representable | Plain text | `JRTextExporter` has no generic-element extension point in JasperReports 7, so chart elements are skipped; the surrounding report texts export normally. |

ODT/Word interoperability note: `JROdtExporter.exportImage(...)` wraps
images in a `draw:frame > draw:text-box > draw:frame` structure (rotation
support) that Microsoft Word does not render — images embedded that way are
invisible when the ODT is opened in Word (LibreOffice shows them). The
Charteon ODT handler therefore writes a plain `draw:frame > draw:image`
itself and only registers the image bytes through the exporter's document
builder, so the charts show up in both Word and LibreOffice. The ODS
exporter does not use the text-box wrapper, so ODS keeps the standard
delegation.

The rendered PNG is cached in a weak map keyed by the print element (multi-
pass exports do not re-render). It is deliberately *not* stored as an element
parameter: the print-XML exporter serializes all element parameters, and a
cached byte array would bloat the XML and fail the loader's value
deserialization whitelist.

JasperReports 7 note: the POI-based XLS exporter of the 6.x line no longer
exists; XLSX is the spreadsheet format of the 7.x line (plus ODS).

## Known limitations

- **Gradient fills rasterize in PDF.** Java2D/`PdfGraphics2D` represents some
  ECharts gradient fills (e.g. the `visualMap` gradient bar of a heatmap) as
  small embedded images. Chart geometry, text and axes are always vector.
- **Plain-text export cannot show charts** (no generic-element hook in
  `JRTextExporter`, see above); use CSV for a data-carrying text format.
- **One chart per component.** Combination/multi-axis setups are expressed
  through the raw option escape hatch rather than dedicated markup.
- **JasperReports 6.x is not supported** (different extension XML model, see
  above).

## Clean-room statement

Charteon is an independent implementation built exclusively on top of the
public JasperReports extension APIs (`ComponentsBundle`, `ComponentCompiler`,
`FillComponent`, `GenericElementHandlerBundle`) and the public Apache ECharts
option API. It does not copy, replicate, or derive from any commercial
charting product or from the proprietary chart extensions shipped with
commercial JasperReports editions — no code, no schemas, no XML namespaces,
no option/property vocabularies. ECharts' own capabilities define Charteon's
feature set; all tag, attribute and property names were derived independently
from ECharts and JasperReports terminology.
