# Contributing to Charteon

Thanks for your interest in improving Charteon!

## Building

Prerequisites: JDK 25+, Maven 3.9+.

```bash
mvn clean package
```

produces two artifacts in `target/`:

- `charteon-core-<version>.jar` — thin JAR (classes + bundled ECharts)
- `charteon-core-<version>-all.jar` — additionally bundles GraalJS and Batik

```bash
mvn test
```

runs the test suite and generates the visual showcase in
`target/test-output/` (`charteon-overview.pdf`, `charteon-overview.html`) —
please eyeball both after changes to the rendering pipeline.

## Project layout

| Path | Contents |
|---|---|
| `src/main/java/tech/charteon/component` | component model, compiler, extension registration |
| `src/main/java/tech/charteon/fill` | fill-time dataset evaluation, generic element production |
| `src/main/java/tech/charteon/model` | ECharts option generation |
| `src/main/java/tech/charteon/export` | per-format export handlers (pdf/html/raster/ssr) |
| `src/main/resources/tech/charteon` | bundled `echarts.min.js` + SSR bootstrap |
| `docs/` | user and architecture documentation |

## Code style

- Follow the existing style, which intentionally mirrors JasperReports
  conventions (tabs, braces on their own line, one class per concern) so the
  code reads naturally next to the JR sources.
- Every public class/method that is part of the user-facing surface gets
  Javadoc.
- No new runtime dependencies without discussion — the dependency footprint
  (GraalJS, Batik) is a feature.

## Licensing rules for contributions

- Charteon is LGPLv3; by contributing you agree to license your contribution
  under LGPLv3.
- **Clean-room policy:** do not copy code, schemas, namespaces, or
  option/property vocabularies from any commercial charting product or from
  proprietary JasperReports extensions. The public Apache ECharts option API
  and the public JasperReports extension APIs are the only design references.
- When upgrading the bundled ECharts, update `NOTICE` and keep the
  Apache-2.0 license header intact.

## Pull requests

1. Fork, create a feature branch (`feature/<topic>`).
2. Keep changes focused; include/adjust tests (`mvn test` must be green).
3. Update documentation (`README.md`, `docs/`) when behavior changes.
4. Add an entry to `CHANGELOG.md` under "Unreleased".
5. Open the PR with a short description of the motivation and the approach.

## Reporting issues

Please include: Charteon version, JasperReports version, Java version, a
minimal JRXML that reproduces the problem, and (for rendering issues) the
generated SVG if you can get it (`EChartsSvgRenderer.renderSvg(...)` is
public and easy to call from a test).
