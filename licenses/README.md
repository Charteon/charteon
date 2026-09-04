# Bundled third-party license texts

These files are packaged into the Charteon JARs under `META-INF/licenses/` so
that every distributed artifact carries the license text of the software it
bundles. They are verbatim upstream texts — do not edit them.

| File | Applies to |
|---|---|
| `apache-2.0.txt` | Apache ECharts (bundled in every JAR as `tech/charteon/echarts.min.js`), Apache Batik, Apache XML Graphics Commons, Apache Commons IO/Logging (bundled in the `all` JAR) |
| `upl-1.0.txt` | GraalVM JavaScript (`js-community`) and the Truffle framework (bundled in the `all` JAR) |

Charteon's own license (LGPLv3, plus the GPLv3 it incorporates) lives in
`LICENSE` and `GPL-3.0.txt` in the project root and is packaged as
`META-INF/LICENSE` / `META-INF/GPL-3.0.txt`. Attribution for all of the above
is in `NOTICE` (packaged as `META-INF/NOTICE`).

The world map GeoJSON derived from Natural Earth is public domain and needs no
license file; it is credited in `NOTICE`.
