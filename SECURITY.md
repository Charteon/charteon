# Security Policy

## Reporting a vulnerability

Please report security issues **privately**, not as a public issue.

Use GitHub's private reporting — *Security* → *Report a vulnerability* on
<https://github.com/Charteon/charteon> — or write to
[dev@charteon.tech](mailto:dev@charteon.tech).

Please include what you need to make the problem reproducible: the Charteon
version, the JasperReports version, and the JRXML or ECharts option that
triggers it. A report that can be reproduced is fixed in a fraction of the
time one that cannot.

You will get an acknowledgement within a week. Charteon is maintained by one
person, so please do not expect same-day turnaround — but you will be told
where the report stands rather than left in silence.

## Supported versions

The latest released version is the one that receives fixes. Charteon is
pre-1.0; there is no long-term support branch, and the honest answer is that
backports to an older minor version will not happen.

## What is in scope

Charteon renders charts by running JavaScript (Apache ECharts on GraalJS) and
by rasterising SVG (Apache Batik). The interesting boundaries are therefore:

- **Report input reaching the JavaScript engine.** Option values prefixed with
  `js:` are revived into functions and executed. This is deliberate and
  documented — a report template is code, and it runs with the same trust as
  any other report expression. A report author can already run arbitrary Java
  through a JasperReports expression. So: *`js:` executing code is not a
  vulnerability*. A way to execute code **without** `js:`, from data rather
  than from the template, is.
- **GeoJSON and other resources resolved by name.** Map names are restricted
  so that a crafted name cannot walk out of the resource prefix. A way around
  that restriction is in scope.
- **Anything that makes the renderer hang, exhaust memory, or leak state from
  one report into another.** The JavaScript contexts are pooled and reused
  between reports; state crossing that boundary is a real finding.

## What is not in scope

- Vulnerabilities in JasperReports itself — report those to its maintainers.
- Findings that require the attacker to already control the report template
  (see above).
- Reports produced only by a scanner, with no demonstration that the issue is
  reachable in Charteon.

## Dependencies

Charteon depends on GraalJS, Batik and PDFBox, all of which publish advisories
of their own. Dependency updates are proposed automatically (see
`.github/dependabot.yml`) and a known-vulnerable dependency is treated as a
release blocker rather than as a backlog item.
