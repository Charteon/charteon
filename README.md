# Charteon

**Modern, interactive charts for JasperReports, powered by Apache ECharts.**

![Build](https://img.shields.io/badge/build-passing-brightgreen) ![License: LGPL v3](https://img.shields.io/badge/License-LGPL_v3-blue.svg) ![Maven Central](https://img.shields.io/badge/maven--central-coming_soon-lightgrey)

Charteon adds a chart component to the JasperReports Library that renders with
[Apache ECharts](https://echarts.apache.org/) instead of JFreeChart — with the
same JRXML mindset you already know from native chart elements: datasets,
series, category/value expressions.

> **Trademark disclaimer.** Charteon is an independent open-source project. It
> is **not** affiliated with, endorsed by, or sponsored by Cloud Software
> Group, Inc. (Jaspersoft®/JasperReports®) or the Apache Software Foundation
> (Apache ECharts™). Those names are used descriptively only, to state what
> Charteon integrates with. See the [NOTICE](NOTICE) file.

> **🤖 AI-developed project.** Charteon is a heavily AI-developed product: the
> vast majority of its code, tests and documentation were written by a large
> language model (Anthropic's Claude) under human direction and reviewed by its
> maintainers. We are transparent about this. Please review the code and its
> license for your own use case, and report anything that looks off — issues and
> pull requests are welcome.

## Why Charteon?

- **Crisp vector PDFs.** Charts are rendered server-side to SVG (GraalVM
  JavaScript, no browser required) and embedded into the PDF content stream as
  true vector graphics via Apache Batik. No pixelation at any zoom level.
- **Interactive HTML.** In HTML exports the charts stay live ECharts instances:
  tooltips, legend toggling, hover highlighting — something a static raster
  chart image cannot offer.
- **Familiar authoring model.** The component mirrors the series/category/value
  expression structure of native JasperReports chart datasets. If you have
  written a `<chart>` element before, you already know how to use Charteon.
- **Full ECharts surface.** Every core ECharts series type — 22 in total,
  from bar and line to treemap, sankey, candlestick and GeoJSON maps — is
  available declaratively, with dataset structures that match the data
  (hierarchies, relations, OHLC, five-number summaries). The
  `optionExpression` escape hatch remains for fine-tuning.
- **Open license.** LGPLv3, matching the JasperReports Library itself. No
  license servers, no paid tiers.

## Quickstart

### 1. Add the dependency

```xml
<dependency>
    <groupId>tech.charteon</groupId>
    <artifactId>charteon-core</artifactId>
    <version>0.2.0</version>
</dependency>
```

Charteon ships in two flavors:

| Artifact | Contents | Use when |
|---|---|---|
| `charteon-core` (thin, default) | Charteon classes + bundled `echarts.min.js`; GraalJS and Batik resolved transitively by Maven | You use Maven/Gradle dependency management (recommended) |
| `charteon-core` with classifier `all` | Additionally bundles GraalJS/Truffle and Batik | You drop a single JAR into a classpath, or want isolation from other GraalVM/Batik versions |

JasperReports itself is **never** bundled — Charteon links against the
JasperReports version already on your classpath (7.x line, see the
compatibility matrix below).

### 2. Use the component in your JRXML

```xml
<element kind="component" x="0" y="0" width="500" height="300">
  <component kind="chart" chartType="bar">
    <titleExpression><![CDATA["Monthly Sales by Product"]]></titleExpression>
    <categoryDataset>
      <series>
        <seriesExpression><![CDATA[$F{product}]]></seriesExpression>
        <categoryExpression><![CDATA[$F{month}]]></categoryExpression>
        <valueExpression><![CDATA[$F{amount}]]></valueExpression>
      </series>
    </categoryDataset>
  </component>
</element>
```

No extra registration needed — Charteon registers itself through the standard
JasperReports extension mechanism (`jasperreports_extension.properties`) as
soon as the JAR is on the classpath.

### 3. Fill and export as usual

```java
JasperReport report = JasperCompileManager.compileReport("report.jrxml");
JasperPrint print = JasperFillManager.fillReport(report, params, dataSource);

// vector charts:
JasperExportManager.exportReportToPdfFile(print, "report.pdf");

// interactive charts:
HtmlExporter html = new HtmlExporter();
html.setExporterInput(new SimpleExporterInput(print));
html.setExporterOutput(new SimpleHtmlExporterOutput("report.html"));
html.exportReport();
```

## Supported chart types

Every core Apache ECharts series type is supported declaratively (the
ECharts-GL/3D add-on package is out of scope):

| `chartType` | Dataset | Expected data structure |
|---|---|---|
| `bar` | `categoryDataset` | series / category / value per record |
| `line` | `categoryDataset` | series / category / value per record |
| `pie` | `categoryDataset` | first series; categories become slices |
| `radar` | `categoryDataset` | categories become indicators, one polygon per series |
| `gauge` | `categoryDataset` | first series; one needle per value |
| `funnel` | `categoryDataset` | first series; categories become funnel steps |
| `heatmap` | `categoryDataset` | categories → x axis, series names → y axis, value → cell color |
| `map` | `categoryDataset` | categories are region names of the GeoJSON map (`mapName`, default `world`) |
| `parallel` | `categoryDataset` | categories become the parallel axes, one line per series |
| `themeRiver` | `categoryDataset` | categories → x positions, one stream per series |
| `pictorialBar` | `categoryDataset` | like `bar`, drawn from repeated symbols (`symbol` attribute) |
| `scatter` | `xyDataset` | x / y per point; optional `sizeExpression` → bubble |
| `effectScatter` | `xyDataset` | like `scatter`, with ripple highlight in HTML |
| `custom` | `xyDataset` | data points + a `renderItem` callback (`"js:function..."`) via `optionExpression` |
| `tree` | `hierarchyDataset` | name / parent / value per record (flat list → tree) |
| `treemap` | `hierarchyDataset` | name / parent / value per record |
| `sunburst` | `hierarchyDataset` | name / parent / value per record |
| `sankey` | `relationDataset` | source / target / value edges |
| `graph` | `relationDataset` | source / target / value edges; nodes derived automatically |
| `lines` | `relationDataset` | edges + start/end coordinate expressions |
| `boxplot` | `boxplotDataset` | category + min / q1 / median / q3 / max per record |
| `candlestick` | `candlestickDataset` | category + open / close / low / high per record |

### Variants are properties, not extra types

Just like in ECharts itself, presentation variants are attributes of the base
type:

| Attribute | Applies to | Effect |
|---|---|---|
| `showValues="true"` | most types | labels every data point with its value (on the bars, slices, nodes, ...) |
| `stacked="true"` | bar, line | stacked series |
| `horizontal="true"` | bar, line, boxplot, pictorialBar | swapped axes |
| `filled="true"` | line | area chart (`areaStyle`) |
| `smooth="true"` | line | smoothed line |
| `step="start\|middle\|end"` | line | step line |
| `innerRadius="45%"` | pie | doughnut/ring chart |
| `roseType="radius\|area"` | pie | Nightingale rose |
| `polar="true"` | bar, line | polar instead of cartesian coordinates |
| `symbol="..."` | pictorialBar, scatter | ECharts symbol name |
| `mapName="..."` | map | registered GeoJSON map (default `world`) |
| `graphLayout="circular\|force\|none"` | graph | layout (default `circular` — deterministic in PDF) |
| `sizeExpression` (in `xyDataset`) | scatter | bubble sizing |
| `valueFormat="#,##0.00 €"` | bar, line, pie | number format for value axis + labels + tooltip (see below) |
| `groupingSeparator` / `decimalSeparator` | bar, line, pie | separators for `valueFormat` (e.g. `.` / `,` for German) |
| `xAxisTitle` / `yAxisTitle` / `secondaryAxisTitle` | bar, line | axis titles |
| `colors="#5470c6,#91cc75,…"` | all | color palette (comma-separated); cycled across series / categories |
| `colorByCategory="true"` | bar (pie/funnel do it by default) | each category its own palette color (e.g. every bar different) |

Per **series** (inside a `<series>` of a `categoryDataset`):

| Attribute | Effect |
|---|---|
| `seriesType="line"` | override the chart's base type for this series (combo charts) |
| `secondaryAxis="true"` | plot this series against a second value axis (dual axis) |
| `color="#2e7d32"` | fixed color for this series (e.g. blue bars + red line) |

### Colors

`colors` sets one **harmonized palette for the whole chart**, applied at the
option root — so it cycles across *every* chart type's elements (pie slices,
funnel steps, sankey nodes, bars, lines, …), not just one type:

```xml
<component kind="chart" chartType="bar" horizontal="true"
           colors="#5470c6,#91cc75,#fac858,#ee6666,#73c0de"
           colorByCategory="true">   <!-- every bar a different palette color -->
```

- `colorByCategory` colors by data item instead of by series (pie and funnel
  already do this; it is mainly for bar charts).
- Per-series `color` gives a specific series a fixed color (combo charts).
- For per-element rules beyond a palette, the `optionExpression` escape hatch
  still applies, e.g. `"series":{"itemStyle":{"color":"js:function(p){return p.value<0?'#c0392b':'#27ae60';}"}}`.

The pre-v2 variant type names (`stackedBar`, `horizontalBar`, `area`,
`doughnut`, `bubble`) keep working as aliases.

### Combo charts & dual axis

Mix series types in one chart and give a series its own value axis — the
classic "bars on the left, trend line on the right" report chart. Set the base
`chartType` (e.g. `bar`), then override individual series with `seriesType`
and/or `secondaryAxis`:

```xml
<component kind="chart" chartType="bar" showValues="true"
           yAxisTitle="Expenses (€)" secondaryAxisTitle="Balance (€)"
           valueFormat="#,##0 €" groupingSeparator="." decimalSeparator=",">
  <categoryDataset>
    <series>                                  <!-- bars, primary (left) axis -->
      <seriesExpression><![CDATA["Expenses"]]></seriesExpression>
      <categoryExpression><![CDATA[$F{month}]]></categoryExpression>
      <valueExpression><![CDATA[$F{expenses}]]></valueExpression>
    </series>
    <series seriesType="line" secondaryAxis="true"><!-- line, secondary (right) axis -->
      <seriesExpression><![CDATA["Balance"]]></seriesExpression>
      <categoryExpression><![CDATA[$F{month}]]></categoryExpression>
      <valueExpression><![CDATA[$F{balance}]]></valueExpression>
    </series>
  </categoryDataset>
</component>
```

Both attributes are optional and backward compatible — without them the chart
renders exactly as before.

### Number & axis formatting

`valueFormat` is a compact pattern applied to the value-axis labels, the value
data labels (`showValues`) and the HTML tooltip. It works on almost every
axis-based type as well as pie labels:

| `valueFormat` | Renders (default separators) |
|---|---|
| `#,##0` | `1,234` |
| `#,##0.00` | `1,234.56` |
| `€ #,##0.00` | `€ 1,234.56` |
| `#,##0 %` | `1,234 %` |

The mask uses `#`/`0` for digits, `,` to enable thousands grouping and `.` for
the decimal point; text before/after the mask is a literal prefix/suffix. The
actual separators are configurable — for German (`1.234,56 €`) set
`groupingSeparator="."` and `decimalSeparator=","`. For anything beyond this,
the `optionExpression` escape hatch with a `js:` formatter still applies.

The `optionExpression` escape hatch also works *together* with a typed
dataset: the raw option is deep-merged over the generated option (an object
merged over the generated series array applies to every series), so you can
fine-tune any detail of a typed chart with plain ECharts option JSON. String
values starting with `js:` are revived into functions (e.g. `renderItem`,
callback formatters) — in both the HTML and the server-side rendering.

Custom GeoJSON maps: `CharteonMaps.register("mymap", geoJsonString)` or a
classpath resource `tech/charteon/maps/mymap.geo.json`, then
`mapName="mymap"`. The bundled `world` map is Natural Earth 1:110m
(public domain).

See [docs/chart-reference.md](docs/chart-reference.md) for a full JRXML
example per type and [docs/getting-started.md](docs/getting-started.md) for a
step-by-step walkthrough.

## Export format support

Charteon covers every exporter that ships with the JasperReports 7.x line:

| Format (exporter) | Chart rendering |
|---|---|
| HTML (`HtmlExporter`) | interactive vector (live ECharts: tooltips, legend toggle, zoom) |
| PDF (`JRPdfExporter`) | static vector (SVG in the PDF content stream — sharp at any zoom) |
| Graphics2D / Swing viewer / printing / print service | static vector via Batik |
| XLSX (`JRXlsxExporter`) | high-resolution raster (3× supersampled PNG) |
| DOCX (`JRDocxExporter`) | high-resolution raster |
| PPTX (`JRPptxExporter`) | high-resolution raster |
| ODT (`JROdtExporter`) | high-resolution raster |
| ODS (`JROdsExporter`) | high-resolution raster |
| RTF (`JRRtfExporter`) | high-resolution raster |
| CSV (`JRCsvExporter`) | data fallback: chart title + `name=value` pairs |
| Plain text (`JRTextExporter`) | not representable — charts are skipped (no generic-element hook in JasperReports); surrounding texts export normally |
| JR print XML (`JRXmlExporter`) | lossless roundtrip: the chart payload serializes with the document and renders again after reloading |

Why vector where possible, raster elsewhere, and data for CSV is explained in
[docs/architecture.md](docs/architecture.md). The XLS (POI) exporter of
JasperReports 6.x no longer exists in the 7.x line.

## Compatibility

| Charteon | JasperReports Library | Java | Notes |
|---|---|---|---|
| 0.2.x | 7.0.x (tested against 7.0.7) | 25+ | all exporters listed above are available from 7.0.0 |
| 0.1.x | 7.0.x (tested against 7.0.7) | 25+ | PDF/HTML/G2D/XLSX/DOCX/PPTX only |

JasperReports 6.x uses a different (namespace/XSD-based) extension XML model
and is not supported; see [docs/architecture.md](docs/architecture.md) for
background.

## Demo

`mvn test` generates a complete showcase you can open directly: the overview
report (all 22 series types plus variants, grouped into Basic / Statistical /
Hierarchy & Network / Geo / Special) exported once per format:

- `target/test-output/charteon-overview.pdf` — vector
- `target/test-output/charteon-overview.html` — interactive
- `charteon-overview.{xlsx,docx,pptx,odt,ods,rtf}` — high-resolution raster
- `charteon-overview.csv` (data fallback), `.txt`, `.jrpxml` (print XML),
  `charteon-overview-page1.png` (Graphics2D)

*(screenshots coming soon)*

## Building from source

```bash
mvn clean package
```

produces `target/charteon-core-<version>.jar` (thin) and
`target/charteon-core-<version>-all.jar` (bundled). See
[CONTRIBUTING.md](CONTRIBUTING.md).

## License

Charteon is licensed under the [GNU Lesser General Public License v3](LICENSE)
(the same license as the JasperReports Library). Bundled third-party software
is listed in the [NOTICE](NOTICE) file: Apache ECharts (Apache-2.0), GraalVM
JavaScript Community (UPL-1.0), Apache Batik (Apache-2.0).
