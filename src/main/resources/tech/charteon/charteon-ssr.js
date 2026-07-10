/*
 * Charteon SSR bootstrap for GraalVM JavaScript.
 *
 * Provides the minimal environment shims Apache ECharts expects outside a
 * browser and exposes a single render function that produces an SVG string
 * via the ECharts server-side rendering API (no DOM, no canvas).
 */
(function (global) {
    'use strict';

    // ECharts/zrender schedule frame-loop work via these; the SSR render is
    // fully synchronous (setOption + renderToSVGString), so scheduled
    // callbacks must never run - invoking them inline would recurse through
    // the frame loop until the stack overflows.
    if (typeof global.setTimeout === 'undefined') {
        global.setTimeout = function () { return 0; };
        global.clearTimeout = function () {};
    }
    if (typeof global.requestAnimationFrame === 'undefined') {
        global.requestAnimationFrame = function () { return 0; };
        global.cancelAnimationFrame = function () {};
    }

    // Function revival: option values are transported as JSON, which cannot
    // carry functions; string values prefixed with "js:" (e.g. the custom
    // series renderItem, or formatter callbacks) are revived into functions.
    // The code originates from the report template and runs with the same
    // trust as any report expression.
    function revive(node) {
        if (typeof node === 'string') {
            if (node.indexOf('js:') === 0) {
                return (0, eval)('(' + node.substring(3) + ')');
            }
            return node;
        }
        if (Array.isArray(node)) {
            for (var i = 0; i < node.length; i++) {
                node[i] = revive(node[i]);
            }
            return node;
        }
        if (node !== null && typeof node === 'object') {
            for (var key in node) {
                if (Object.prototype.hasOwnProperty.call(node, key)) {
                    node[key] = revive(node[key]);
                }
            }
            return node;
        }
        return node;
    }
    global.__charteonRevive = revive;

    var registeredMaps = {};

    global.__charteonRegisterMap = function (mapName, geoJson) {
        if (!registeredMaps[mapName]) {
            echarts.registerMap(mapName, JSON.parse(geoJson));
            registeredMaps[mapName] = true;
        }
    };

    global.__charteonRender = function (optionJson, width, height, theme) {
        var chart = echarts.init(null, theme || null, {
            renderer: 'svg',
            ssr: true,
            width: width,
            height: height
        });
        try {
            var option = revive(JSON.parse(optionJson));
            option.animation = false;
            chart.setOption(option);
            var svg = chart.renderToSVGString();
            // The emitted <style> element only carries :hover/cursor rules for
            // interactive use; Batik cannot parse style sheets in documents
            // without a base URI (as created by JasperReports), so strip it.
            svg = svg.replace(/<style[\s\S]*?<\/style>/g, '');
            // Ensure the root <svg> carries a viewBox so the chart scales
            // proportionally when drawn into a differently sized area (PDF /
            // image export and the designer canvas render at a reference size
            // and scale the vector to the element). ECharts does not always emit
            // one. Keeping the intrinsic width/height as the default box while
            // adding the viewBox lets every consumer map it onto its own frame.
            if (svg.indexOf('viewBox') === -1) {
                svg = svg.replace(/<svg\b/,
                    '<svg viewBox="0 0 ' + width + ' ' + height + '" '
                    + 'preserveAspectRatio="xMidYMid meet"');
            }
            return svg;
        } finally {
            chart.dispose();
        }
    };
})(globalThis);
