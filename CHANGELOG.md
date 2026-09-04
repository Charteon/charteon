# Changelog

All notable changes to Charteon are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project
adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Changed

- **Bars are capped at 72px.** ECharts sizes a bar to its share of the category
  band, so a two-category chart in a 640px-wide element drew 156px-wide bars —
  a colour field rather than a bar. Line series are unaffected.
- **The legend pages instead of growing into the plot.** It is now
  `type: "scroll"`. A report element has a fixed height, so a legend that grew
  upwards ate the plot area and was then clipped, with no way for the reader to
  scroll it.
- **Magnitude scales use an explicit sequential ramp.** Heatmap and map
  inherited the ECharts default, whose light end is 1.37:1 against white paper —
  the bottom third of every scale disappeared in print — and whose blue was a
  different blue from palette slot 1. The ramp is now `#86b6ef → #2a78d6 →
  #184f95`, anchored on the categorical slot, monotonically darker, starting at
  2.11:1 and ending at 8.10:1.
- **Charts have a default color palette.** Without `colors` and without a
  `theme`, charts used the ECharts stock palette, which is tuned for screens:
  five of its nine hues sit below a 3:1 contrast ratio against paper, its
  yellow/green pair is barely separable even with full color vision, and its
  orange/green pair collapses under protanopia. Reports get printed,
  photocopied and archived in greyscale, so the default is now an eight-slot
  palette that keeps a lightness spread and stays separable under simulated
  color-vision deficiency. `colors` still wins, and a `theme` keeps its own
  palette untouched.
- **Charts inherit the report's font.** They drew their text in the ECharts
  default face while the report around them used its own — two typefaces in one
  page. `fontName`/`fontSize` now default to the chart element's style font
  (resolved through the usual JasperReports style inheritance); setting either
  attribute overrides it.
- **Number separators follow the report locale.** `groupingSeparator` /
  `decimalSeparator` defaulted to `,` / `.` regardless of the report, so a
  chart in a German report printed `1,234.56` while the text around it printed
  `1.234,56`. They now default to the separators of the locale the report is
  filled with; setting them explicitly still overrides that.

### Fixed

- **The colour scale no longer sits on top of the legend.** `visualMap` and
  `legend` were both placed at `bottom: 0`; with `showLegend="true"` on a
  heatmap or map they drew over each other. The legend keeps the edge and the
  colour scale moves up by its height.

### Added

- **A warning when the palette cycles.** With more series than palette slots,
  ECharts silently reuses hue 1 for series 9, so two series share a colour
  while the legend claims they differ. Nothing can be fixed at render time, but
  the build now logs a warning naming the series count and the alternatives.
- **`textColor`, `fontName` and `fontSize` attributes.** The label colour was
  the literal `#333` at seven places in the option builder — needed because SSR
  otherwise inherits white, but unreachable except through `optionExpression`,
  and wrong on a dark report background. It is now one constant behind
  `textColor`; an explicit value also drives title, legend and axis text.
- **`decal` attribute** — adds a texture per series (ECharts `aria.decal`) on
  top of the palette color, so series identity survives black-and-white
  printing and color-vision deficiency. Off by default, because it visibly
  changes every existing chart. The accessibility layer itself (`aria.enabled`,
  which puts a generated chart description into the exported SVG) is now always
  on.
- **License texts are now packaged into the JARs.** Both artifacts previously
  shipped without any license file, although they distribute third-party code:
  the thin JAR bundles Apache ECharts, the `all` JAR additionally bundles
  GraalJS/Truffle (UPL-1.0) and Batik (Apache-2.0), and Charteon itself is
  LGPLv3. Every JAR now carries `META-INF/LICENSE` (LGPLv3),
  `META-INF/GPL-3.0.txt`, `META-INF/NOTICE` and the verbatim upstream texts in
  `META-INF/licenses/` (`apache-2.0.txt`, `upl-1.0.txt`). In the shaded `all`
  JAR the `ApacheLicenseResourceTransformer` deletes `META-INF/LICENSE`
  (by design — it removes duplicates rather than keeping one) and the
  `ApacheNoticeResourceTransformer` rewrites `META-INF/NOTICE`, so Charteon's
  own texts are additionally included verbatim as
  `META-INF/licenses/charteon-*`.
- **`AUTHORS`** file, and the `<developers>` block in `pom.xml` that Maven
  Central requires.

## [0.3.5] - 2026-08-29

### Changed

- **Dependency refresh.** JasperReports 7.0.7 -> 7.0.8, GraalJS/polyglot
  25.1.3 -> 25.3.4.1, PDFBox (test only) 3.0.7 -> 3.0.8, JUnit Jupiter
  5.11.4 -> 6.1.3. Build plugins: maven-compiler-plugin 3.15.0,
  maven-shade-plugin 3.6.2. The shaded runtime JAR still carries
  `Multi-Release: true`, which GraalVM/Truffle requires inside the
  Jaspersoft Studio OSGi bundle.

## [0.3.4] - 2026-07-10

### Fixed

- **Text is now vertically centered in static exports.** ECharts emits SVG
  text anchored with `dominant-baseline="central"`, which Batik does not
  implement — every label sat about half the x-height too high in
  PDF/raster/preview output (most visible next to the legend swatches). The
  SSR post-processing now emulates the central baseline with an explicit
  `dy` shift. Interactive HTML output was always correct and is unchanged.

## [0.3.3] - 2026-07-10

### Fixed

- **Sankey, tree and themeRiver labels are legible in report output.**
  Server-side rendering inherits white as the text color, so the node/stream
  labels of these types were white on pale fills (invisible for tree). They
  now use an explicit dark label color in the report output, matching what
  the design-time preview already did since 0.3.0.

## [0.3.2] - 2026-07-10

### Fixed

- **Title collisions fixed for all remaining full-canvas layouts.** A visual
  audit of all 31 overview charts found the same title overlap the 0.3.1
  sunburst fix addressed in four more layouts: polar coordinates (bar/line on
  polar), circular graph, sankey and parallel axes now shrink or shift down
  when a title/subtitle is present. Explicit geometry via `optionExpression`
  still takes precedence.

## [0.3.1] - 2026-07-10

### Fixed

- **Sunburst no longer collides with the chart title.** A titled sunburst
  filled 90% of the canvas from the exact center, so the top ring ran into the
  title text. With a title/subtitle present the series is now shrunk and
  lowered automatically (report output and design preview alike); an explicit
  `radius`/`center` in `optionExpression` still takes precedence.

## [0.3.0] - 2026-07-10

### Added

- **First-class colors.** New `colors` attribute (comma-separated palette,
  applied at the option root so it harmonizes across *all* chart types),
  `colorByCategory` (each category its own palette color, e.g. every bar
  different), and a per-series `color` attribute for combo charts — no
  `optionExpression` needed for the common cases.
- **Combo charts & dual axis.** A `<series>` of a `categoryDataset` can now
  override the chart's base type with `seriesType` (e.g. a `line` over `bar`s)
  and be plotted against a second value axis with `secondaryAxis="true"` — the
  classic "bars + trend line on the right axis" report chart. Fully backward
  compatible.
- **Number & axis formatting.** New component attributes `valueFormat` (e.g.
  `#,##0.00 €`), `groupingSeparator`/`decimalSeparator` (default `,`/`.`;
  German `.`/`,`), and `xAxisTitle`/`yAxisTitle`/`secondaryAxisTitle`. The value
  format is applied to the value axis labels, the value data labels
  (`showValues`) and the HTML tooltip on bar/line, and to pie slice labels.

### Fixed

- **Value labels no longer overlap with many categories.** Bar/line value
  labels (`showValues`) now use ECharts `labelLayout.hideOverlap`, so colliding
  labels are dropped automatically — all show when there is room, thinned out
  when the chart has many/thin bars (e.g. expenses over ~20 categories).
- **Combo is now visible in the design canvas.** The sample-data preview
  reflects the per-series `seriesType`/`secondaryAxis`, so a bar+line / dual-axis
  chart previews as a combo in the designer instead of generic grouped bars.
- **Design-time preview legibility** (wizard thumbnails / canvas): a cosmetic
  styling layer is now merged over the sample option so small previews read
  well — fewer, non-overlapping axis ticks and smaller fonts (bar/line/
  pictorialBar), a decluttered single-needle gauge with a readable centre value,
  and dark labels for the previously white/invisible funnel, themeRiver, tree,
  sankey and graph labels. Axis styling is only applied to types that have axes,
  so gauge/pie/funnel no longer gain a stray axis line. Report output is
  unchanged (preview-only).

### Changed

- Bytecode target lowered from Java 25 to **Java 17** so the library classes
  load inside older host JVMs (e.g. the JRE bundled with Jaspersoft Studio,
  for the charteon-studio designer plugin). GraalJS still requires a
  **JDK 21+ at runtime** for SVG rendering; pure model/design classes work
  on 17+.
- `CharteonExtensionsRegistryFactory` is now engine-adaptive: on a
  JasperReports **6.x** classpath it skips the JR7 registration and logs a
  warning instead of silently producing a broken (parser-less) components
  bundle — the JR7 registration code accidentally links under JR6 due to
  erased generics. JR 6.x design/fill support lives in the separate
  `charteon-jr6-adapter` artifact of the charteon-studio project.

## [0.2.0] - 2026-07-06

Full ECharts core coverage and full exporter coverage.

### Added

- **All 22 core ECharts series types are now typed** (no raw option needed):
  `line`, `bar`, `pie`, `scatter`, `effectScatter`, `radar`, `tree`,
  `treemap`, `sunburst`, `boxplot`, `candlestick`, `heatmap`, `map`,
  `parallel`, `lines`, `graph`, `sankey`, `funnel`, `gauge`, `pictorialBar`,
  `themeRiver`, `custom`
- New declarative datasets matching the data structures:
  `hierarchyDataset` (name/parent/value), `relationDataset`
  (source/target/value + optional coordinates for `lines`),
  `boxplotDataset` (five-number summary), `candlestickDataset` (OHLC)
- Variant attributes on the base types (instead of duplicate chart types):
  `stacked`, `horizontal`, `filled`, `smooth`, `step`, `innerRadius`,
  `roseType`, `polar`, `symbol`, `mapName`, `graphLayout`
- GeoJSON map support: bundled `world` map (Natural Earth 1:110m, public
  domain), custom maps via `CharteonMaps.register(...)` or classpath
  resources `tech/charteon/maps/<name>.geo.json`
- Function revival: option string values prefixed with `js:` become
  functions in HTML and server-side rendering (enables the `custom` series'
  `renderItem` and callback formatters)
- New export handlers: ODT, ODS, RTF (high-resolution raster) and CSV
  (data fallback: chart title + name=value pairs)
- JR print XML: chart elements survive the `JRXmlExporter` /
  `JRPrintXmlLoader` roundtrip and render again after reloading
- One JUnit test per export format; `mvn test` leaves one showcase file per
  format in `target/test-output/`
- Overview report extended to 31 charts in six groups (Basic, Statistical,
  Hierarchy & Network, Geo, Special, Raw option)
- `showValues="true"` attribute: labels every data point with its value —
  bars/lines (on top, inside when stacked), pie/funnel (`name: value`),
  treemap/sunburst/sankey/graph nodes, scatter points (y value), map
  regions, lines ends, radar vertices, pictorialBar

### Changed

- The variant type names `stackedBar`, `horizontalBar`, `area`, `doughnut`,
  `bubble` are now aliases of the base types (still fully supported)

### Fixed

- ODT: charts were invisible when the exported ODT was opened in Microsoft
  Word (Word does not render the `draw:text-box`-wrapped image frames the
  JasperReports ODT exporter emits). The ODT handler now writes a plain
  `draw:frame`/`draw:image`, which renders in both Word and LibreOffice.

### Known limitations

- The plain-text exporter has no generic-element hook in JasperReports 7;
  charts are skipped there (report texts export normally)
- JasperReports 7 no longer ships the POI-based XLS exporter (use XLSX)

## [0.1.0] - 2026-07-06

Initial release.

### Added

- JasperReports 7.x chart component (`<component kind="chart">`) with
  JasperReports-style declarative datasets:
  - `categoryDataset` (series/category/value/label expressions)
  - `xyDataset` (series/x/y/size/label expressions)
- 12 typed chart types: `bar`, `stackedBar`, `horizontalBar`, `line`, `area`,
  `pie`, `doughnut`, `radar`, `gauge`, `funnel`, `scatter`, `bubble`
- Raw option escape hatch (`optionExpression`): any Apache ECharts series
  type via option JSON, standalone or deep-merged over a typed chart
- PDF export: server-side rendering to SVG via GraalVM JavaScript (pooled
  contexts, no browser), embedded as true vector graphics via the
  JasperReports/Batik bridge
- HTML export: interactive ECharts instances (library embedded once per
  report)
- Graphics2D export (Swing viewer/printing): vector via Batik
- XLSX/DOCX/PPTX export: 3× supersampled PNG fallback
- Compile-time expression collection and verification like native chart
  elements; delayed evaluation (`evaluationTime`) support
- Themes (`theme` attribute), title/subtitle/legend options
- Showcase report and end-to-end tests generating
  `target/test-output/charteon-overview.{pdf,html}`
