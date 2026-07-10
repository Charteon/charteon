# Getting Started with Charteon

This walkthrough takes you from an empty report to a filled PDF/HTML with a
Charteon chart. It assumes basic JasperReports knowledge (JRXML, fill,
export).

## 1. Dependencies

```xml
<dependencies>
    <!-- your existing JasperReports 7.x setup -->
    <dependency>
        <groupId>net.sf.jasperreports</groupId>
        <artifactId>jasperreports</artifactId>
        <version>7.0.7</version>
    </dependency>
    <!-- for PDF export -->
    <dependency>
        <groupId>net.sf.jasperreports</groupId>
        <artifactId>jasperreports-pdf</artifactId>
        <version>7.0.7</version>
    </dependency>

    <!-- Charteon -->
    <dependency>
        <groupId>tech.charteon</groupId>
        <artifactId>charteon-core</artifactId>
        <version>0.3.0</version>
    </dependency>
</dependencies>
```

Nothing else: Charteon self-registers via the JasperReports extension
mechanism when it is on the classpath.

If you prefer to manage GraalJS/Batik versions yourself, exclude them and pin
your own versions — or use the self-contained `all` classifier instead:

```xml
<dependency>
    <groupId>tech.charteon</groupId>
    <artifactId>charteon-core</artifactId>
    <version>0.1.0</version>
    <classifier>all</classifier>
</dependency>
```

## 2. A first chart

Charteon charts are component elements. The structure mirrors native
JasperReports chart datasets: a dataset with one or more series, each series
defined by expressions that are evaluated once per record.

```xml
<jasperReport name="sales" pageWidth="595" pageHeight="842" columnWidth="535"
              leftMargin="30" rightMargin="30" topMargin="30" bottomMargin="30"
              uuid="a3b1c2d3-0000-0000-0000-000000000001">

    <field name="month" class="java.lang.String"/>
    <field name="product" class="java.lang.String"/>
    <field name="amount" class="java.lang.Integer"/>

    <summary height="320">
        <element kind="component" x="0" y="0" width="535" height="300">
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
    </summary>
</jasperReport>
```

Notes:

- The chart accumulates values from **all records** of the surrounding
  dataset, exactly like a native chart element. Because the element sits in
  the summary band, it is filled after the last record.
- `seriesExpression` may return a different value per record (as above, one
  series per product) or a constant (a single fixed series).
- The generated axis follows category encounter order.

## 3. Filling and exporting

```java
JasperReport report = JasperCompileManager.compileReport("sales.jrxml");
JasperPrint print = JasperFillManager.fillReport(report, new HashMap<>(), dataSource);

JasperExportManager.exportReportToPdfFile(print, "sales.pdf");   // vector charts

HtmlExporter exporter = new HtmlExporter();
exporter.setExporterInput(new SimpleExporterInput(print));
exporter.setExporterOutput(new SimpleHtmlExporterOutput("sales.html")); // interactive charts
exporter.exportReport();
```

## 4. Using a subdataset

Like native charts, Charteon datasets accept a `datasetRun`, so a chart can be
driven by any subdataset regardless of where the element sits:

```xml
<dataset name="sales" uuid="a3b1c2d3-0000-0000-0000-000000000002">
    <field name="month" class="java.lang.String"/>
    <field name="amount" class="java.lang.Integer"/>
</dataset>
...
<categoryDataset>
    <datasetRun subDataset="sales" uuid="a3b1c2d3-0000-0000-0000-000000000003">
        <dataSourceExpression><![CDATA[new net.sf.jasperreports.engine.data.JRMapCollectionDataSource($P{salesRecords})]]></dataSourceExpression>
    </datasetRun>
    <series>...</series>
</categoryDataset>
```

Tip: pass *collections* as parameters and wrap them in a fresh data source
inside `dataSourceExpression` — a `JRDataSource` instance can only be consumed
once, so sharing one across several charts fails.

## 5. Common options

| Attribute / element | Meaning |
|---|---|
| `chartType` | one of the supported type names (see chart reference) |
| `theme` | ECharts theme name (`dark` is bundled with ECharts) |
| `showLegend` | force legend on/off; default: on when useful (multi-series, pie, …) |
| `evaluationTime` / `evaluationGroup` | as for native charts (`Now`, `Report`, `Page`, `Group`, …) |
| `titleExpression` / `subtitleExpression` | chart title/subtitle (any expression) |
| `optionExpression` | raw ECharts option JSON, merged over the generated option |

## 6. Beyond category charts

Every core ECharts series type has a matching declarative dataset — see the
[chart reference](chart-reference.md) for all of them:

- `xyDataset` (scatter, effectScatter, custom): x/y/size expressions;
- `hierarchyDataset` (tree, treemap, sunburst): name/parent/value per record,
  Charteon assembles the tree;
- `relationDataset` (sankey, graph, lines): source/target/value edges;
- `boxplotDataset` / `candlestickDataset`: five-number summary / OHLC.

Variants (stacked, horizontal, area, smooth, step, doughnut, rose, polar,
bubble) are attributes of the base type, e.g.
`<component kind="chart" chartType="bar" stacked="true">`.

### Geo maps

`chartType="map"` renders a choropleth over a GeoJSON map. The bundled
`world` map works out of the box; the categories are region names
("Germany", "United States of America", …). To use your own map:

```java
CharteonMaps.register("europe", geoJsonString);   // or a classpath resource
// tech/charteon/maps/europe.geo.json
```

then set `mapName="europe"` on the component.

## 7. The raw option escape hatch

Any ECharts feature that has no dedicated markup is available through
`optionExpression`. It must evaluate to a `String` containing an ECharts
option object as JSON:

- **standalone** (no dataset): the option is used as-is;
- **combined with a dataset**: the raw option is deep-merged over the
  generated one, so you can tweak any generated detail (axis formatting,
  colors, label styles, …) without abandoning the declarative dataset. An
  object under `series` applies to every generated series.

A convenient pattern is to keep the JSON in a report parameter:

```xml
<parameter name="tuningOption" class="java.lang.String"/>
...
<component kind="chart" chartType="sankey">
    <optionExpression><![CDATA[$P{tuningOption}]]></optionExpression>
</component>
```

Functions: JSON cannot carry JavaScript functions directly, but any string
value starting with `js:` is revived into a function in every export format —
that is how the `custom` series gets its `renderItem`, and how callback
formatters work. For simple cases, prefer ECharts string template formatters
(e.g. `"{b}: {c}"`).

## 8. Try the showcase

Clone the repository and run `mvn test`; then open the files in
`target/test-output/`: the overview report with all 22 series types is
exported to PDF (vector), HTML (interactive), XLSX/DOCX/PPTX/ODT/ODS/RTF
(high-resolution raster), CSV (data fallback), plain text, JR print XML and
a Graphics2D PNG.

The JRXML behind it (`src/test/resources/reports/charteon-overview.jrxml`)
doubles as a copy-paste catalog for every supported chart type.
