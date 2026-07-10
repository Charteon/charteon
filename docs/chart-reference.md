# Charteon Chart Reference

Every core Apache ECharts series type, with a complete JRXML snippet per
type. All snippets assume the fields used by the showcase report
(`src/test/resources/reports/charteon-overview.jrxml`), which you can run
with `mvn test` to see every example rendered. Snippets go inside a report
band:

```xml
<element kind="component" x="0" y="0" width="500" height="300">
  <component kind="chart" chartType="...">
    ...
  </component>
</element>
```

## Common attributes

| Attribute | Type | Applies to | Description |
|---|---|---|---|
| `chartType` | string | all | the series type (see below) |
| `theme` | string | all | ECharts theme name (e.g. `dark`) |
| `showLegend` | boolean | all | overrides the legend default (shown for multi-series, pie, funnel) |
| `showValues` | boolean | most types | labels every data point with its value: on top of bars/line points (inside stacked segments), `name: value` on pie slices, funnel steps, treemap/sunburst/sankey/graph nodes, the y value on scatter points, the value in map regions and at lines ends. No effect on types that label themselves (gauge, heatmap) or where a single value label is not meaningful (boxplot, candlestick, parallel, themeRiver, tree). Per-record `labelExpression`s override it. |
| `evaluationTime` / `evaluationGroup` | enum / string | all | like native elements (`Now`, `Report`, `Page`, `Group`, ...) |
| `stacked` | boolean | bar, line | stacked series |
| `horizontal` | boolean | bar, line, boxplot, pictorialBar | swapped axes |
| `filled` | boolean | line | area fill (`areaStyle`) |
| `smooth` | boolean | line | smoothed line |
| `step` | `start`/`middle`/`end` | line | step line |
| `innerRadius` | string | pie | doughnut (e.g. `"45%"`) |
| `roseType` | `radius`/`area` | pie | Nightingale rose |
| `polar` | boolean | bar, line | polar coordinates |
| `symbol` | string | pictorialBar, scatter | ECharts symbol name |
| `mapName` | string | map | registered GeoJSON map, default `world` |
| `graphLayout` | `circular`/`force`/`none` | graph | layout, default `circular` |
| `valueFormat` | string | bar, line, pie | number format for value axis + value labels + tooltip, e.g. `#,##0.00 €` |
| `groupingSeparator` / `decimalSeparator` | string | bar, line, pie | separators for `valueFormat` (default `,` / `.`; German: `.` / `,`) |
| `xAxisTitle` / `yAxisTitle` / `secondaryAxisTitle` | string | bar, line | axis titles |
| `colors` | string | all | color palette, comma-separated (e.g. `#5470c6,#91cc75`); applied globally |
| `colorByCategory` | boolean | bar (pie/funnel default) | color by data item instead of by series |

Per-series attributes (on a `<series>` of the `categoryDataset`):

| Attribute | Applies to | Description |
|---|---|---|
| `seriesType` | bar, line | override the chart base type for this series (combo charts) |
| `secondaryAxis` | bar, line | plot this series against a second value axis (dual axis) |
| `color` | bar, line | fixed color for this series (e.g. `#2e7d32`) |

Common child elements: `titleExpression`, `subtitleExpression`, exactly one
dataset element matching the type, and the optional `optionExpression`
(raw ECharts option JSON, deep-merged over the generated option; string
values starting with `js:` are revived into functions).

## Export rendering per type

All types render the same way per format: **interactive vector** in HTML,
**static vector** in PDF/Graphics2D, **3× supersampled PNG** in
XLSX/DOCX/PPTX/ODT/ODS/RTF, **data fallback** (title + values) in CSV, and a
lossless roundtrip in JR print XML. Type-specific notes: `effectScatter`'s
ripple animation and `graph`'s force-layout fine-tuning only show in HTML;
plain text skips charts entirely (no JasperReports hook).

---

## Category-dataset types

The `categoryDataset` mirrors the native JasperReports chart dataset: one or
more `<series>` with `seriesExpression`, `categoryExpression`,
`valueExpression` and an optional `labelExpression`, evaluated per record —
optionally against a subdataset via `<datasetRun>`.

### bar

```xml
<component kind="chart" chartType="bar">
  <titleExpression><![CDATA["Monthly Sales"]]></titleExpression>
  <categoryDataset>
    <series>
      <seriesExpression><![CDATA[$F{product}]]></seriesExpression>
      <categoryExpression><![CDATA[$F{month}]]></categoryExpression>
      <valueExpression><![CDATA[$F{amount}]]></valueExpression>
    </series>
  </categoryDataset>
</component>
```

Variants: `stacked="true"`, `horizontal="true"`, `polar="true"`.

**Combo / dual axis.** A series may override the chart type via `seriesType`
and be plotted on a second value axis via `secondaryAxis` — e.g. bars with a
trend line on the right axis:

```xml
<component kind="chart" chartType="bar"
           yAxisTitle="Amount" secondaryAxisTitle="Rate" valueFormat="#,##0.00">
  <categoryDataset>
    <series>
      <seriesExpression><![CDATA["Amount"]]></seriesExpression>
      <categoryExpression><![CDATA[$F{month}]]></categoryExpression>
      <valueExpression><![CDATA[$F{amount}]]></valueExpression>
    </series>
    <series seriesType="line" secondaryAxis="true">
      <seriesExpression><![CDATA["Rate"]]></seriesExpression>
      <categoryExpression><![CDATA[$F{month}]]></categoryExpression>
      <valueExpression><![CDATA[$F{rate}]]></valueExpression>
    </series>
  </categoryDataset>
</component>
```

### line

Same dataset as `bar`. Variants: `stacked`, `filled` (area), `smooth`,
`step="start|middle|end"`, `polar`.

```xml
<component kind="chart" chartType="line" filled="true" smooth="true">
  <!-- same categoryDataset as bar -->
</component>
```

### pie

Uses the first series; each category becomes a slice. Variants:
`innerRadius="45%"` (doughnut), `roseType="area"` (Nightingale rose).

```xml
<component kind="chart" chartType="pie" innerRadius="45%">
  <categoryDataset>
    <series>
      <seriesExpression><![CDATA["Revenue"]]></seriesExpression>
      <categoryExpression><![CDATA[$F{name}]]></categoryExpression>
      <valueExpression><![CDATA[$F{value}]]></valueExpression>
    </series>
  </categoryDataset>
</component>
```

### radar

Categories become the radar indicators (axis max = 110 % of the largest
value), each series one polygon. Same dataset shape as `bar`.

### gauge

First series; each value drives a needle. Typically a single record:

```xml
<component kind="chart" chartType="gauge">
  <categoryDataset>
    <series>
      <seriesExpression><![CDATA["KPI"]]></seriesExpression>
      <categoryExpression><![CDATA["Utilization"]]></categoryExpression>
      <valueExpression><![CDATA[$F{utilization}]]></valueExpression>
    </series>
  </categoryDataset>
</component>
```

### funnel

First series; categories become the funnel steps, sorted by value
(descending) by ECharts.

### heatmap

Categories form the x axis, series names the y axis, values the cell color
(with an automatic `visualMap` over the value range and in-cell labels):

```xml
<component kind="chart" chartType="heatmap">
  <categoryDataset>
    <series>
      <seriesExpression><![CDATA[$F{weekday}]]></seriesExpression>
      <categoryExpression><![CDATA[$F{timeslot}]]></categoryExpression>
      <valueExpression><![CDATA[$F{orders}]]></valueExpression>
    </series>
  </categoryDataset>
</component>
```

### map

Choropleth over a registered GeoJSON map. Categories are region names, the
value range drives the color scale. Charteon bundles `world` (Natural Earth
1:110m; names like "Germany", "United States of America"). Register custom
maps via `CharteonMaps.register(name, geoJson)` or as a classpath resource
`tech/charteon/maps/<name>.geo.json`.

```xml
<component kind="chart" chartType="map" mapName="world">
  <categoryDataset>
    <series>
      <seriesExpression><![CDATA["Revenue"]]></seriesExpression>
      <categoryExpression><![CDATA[$F{country}]]></categoryExpression>
      <valueExpression><![CDATA[$F{revenue}]]></valueExpression>
    </series>
  </categoryDataset>
</component>
```

### parallel

Categories become the parallel axes, each series one line across them. Same
dataset shape as `bar`.

### themeRiver

Categories are the x positions (chronological order = encounter order), each
series one stream. Same dataset shape as `bar`. (Internally the categories
map to numeric axis positions; the labels show the category names.)

### pictorialBar

Like `bar`, but each bar is built from repeated symbols (`symbol` attribute,
default `circle`; any ECharts symbol incl. `image://...` works). Variant:
`horizontal="true"`.

---

## XY-dataset types

The `xyDataset` contains `<series>` with `seriesExpression`,
`xValueExpression`, `yValueExpression`, optional `sizeExpression` and
`labelExpression`.

### scatter

```xml
<component kind="chart" chartType="scatter">
  <xyDataset>
    <series>
      <seriesExpression><![CDATA[$F{cluster}]]></seriesExpression>
      <xValueExpression><![CDATA[$F{x}]]></xValueExpression>
      <yValueExpression><![CDATA[$F{y}]]></yValueExpression>
      <sizeExpression><![CDATA[$F{weight}]]></sizeExpression>
    </series>
  </xyDataset>
</component>
```

With `sizeExpression` the point sizes are normalized into a readable pixel
range (bubble chart).

### effectScatter

Same dataset as `scatter`; adds a ripple highlight around the points in the
HTML export (static symbols in PDF/raster).

### custom

The data comes from the `xyDataset` (each point's values are available as
`api.value(0)`, `api.value(1)` in the callback); the mandatory `renderItem`
callback is injected through the raw option as a revived `js:` function:

```xml
<component kind="chart" chartType="custom">
  <xyDataset><!-- as scatter --></xyDataset>
  <optionExpression><![CDATA[
    "{\"series\": {\"renderItem\": \"js:function(params, api) {"
    + " var c = api.coord([api.value(0), api.value(1)]);"
    + " return { type: 'circle', shape: { cx: c[0], cy: c[1], r: 6 },"
    + "          style: { fill: '#5470c6' } }; }\"}}"
  ]]></optionExpression>
</component>
```

---

## Hierarchy-dataset types (tree, treemap, sunburst)

The `hierarchyDataset` turns a flat record stream into a tree: each record is
one node, linked to its parent by name (`null`/unknown parent = root).

```xml
<component kind="chart" chartType="treemap">
  <hierarchyDataset>
    <nameExpression><![CDATA[$F{name}]]></nameExpression>
    <parentExpression><![CDATA[$F{parent}]]></parentExpression>
    <valueExpression><![CDATA[$F{value}]]></valueExpression>
  </hierarchyDataset>
</component>
```

- `tree`: node-link diagram; a forest is wrapped under an invisible root.
- `treemap`: area encodes the value; group nodes without a value get the sum
  of their children.
- `sunburst`: same data as concentric rings.

## Relation-dataset types (sankey, graph, lines)

The `relationDataset` collects one source→target edge per record.

```xml
<component kind="chart" chartType="sankey">
  <relationDataset>
    <sourceExpression><![CDATA[$F{from}]]></sourceExpression>
    <targetExpression><![CDATA[$F{to}]]></targetExpression>
    <valueExpression><![CDATA[$F{amount}]]></valueExpression>
  </relationDataset>
</component>
```

- `sankey`: weighted flows; nodes are derived from the distinct edge names.
- `graph`: network; node size reflects the summed edge weights. Layout via
  `graphLayout` (default `circular` — deterministic, identical in PDF and
  HTML; `force` computes a one-shot layout server-side).
- `lines`: point-to-point connections; additionally requires the coordinate
  expressions:

```xml
<component kind="chart" chartType="lines">
  <relationDataset>
    <sourceExpression><![CDATA[$F{from}]]></sourceExpression>
    <targetExpression><![CDATA[$F{to}]]></targetExpression>
    <valueExpression><![CDATA[$F{volume}]]></valueExpression>
    <sourceXExpression><![CDATA[$F{fromX}]]></sourceXExpression>
    <sourceYExpression><![CDATA[$F{fromY}]]></sourceYExpression>
    <targetXExpression><![CDATA[$F{toX}]]></targetXExpression>
    <targetYExpression><![CDATA[$F{toY}]]></targetYExpression>
  </relationDataset>
</component>
```

## boxplot

One five-number summary per record:

```xml
<component kind="chart" chartType="boxplot">
  <boxplotDataset>
    <categoryExpression><![CDATA[$F{day}]]></categoryExpression>
    <minExpression><![CDATA[$F{min}]]></minExpression>
    <q1Expression><![CDATA[$F{q1}]]></q1Expression>
    <medianExpression><![CDATA[$F{median}]]></medianExpression>
    <q3Expression><![CDATA[$F{q3}]]></q3Expression>
    <maxExpression><![CDATA[$F{max}]]></maxExpression>
  </boxplotDataset>
</component>
```

## candlestick

One OHLC tuple per record:

```xml
<component kind="chart" chartType="candlestick">
  <candlestickDataset>
    <categoryExpression><![CDATA[$F{day}]]></categoryExpression>
    <openExpression><![CDATA[$F{open}]]></openExpression>
    <closeExpression><![CDATA[$F{close}]]></closeExpression>
    <lowExpression><![CDATA[$F{low}]]></lowExpression>
    <highExpression><![CDATA[$F{high}]]></highExpression>
  </candlestickDataset>
</component>
```

---

## Raw option escape hatch

Any chart can carry an `optionExpression` producing an ECharts option object
as JSON. Standalone (no dataset) it is used as-is; combined with a dataset it
is deep-merged over the generated option — objects merge recursively, arrays
are replaced, and an object merged over the generated series array applies to
every series. `js:`-prefixed strings become functions in every output format.

Subdataset note: as with native charts, feed datasets from a subdataset with
`<datasetRun>` inside the dataset element; pass collections as report
parameters and wrap them in the `dataSourceExpression` (e.g.
`new JRMapCollectionDataSource($P{records})`).
