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
package tech.charteon.component;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import net.sf.jasperreports.engine.JRCloneable;
import net.sf.jasperreports.engine.JRConstants;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRGenericElementType;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.base.JRBaseObjectFactory;
import net.sf.jasperreports.engine.component.BaseComponentContext;
import net.sf.jasperreports.engine.component.ComponentContext;
import net.sf.jasperreports.engine.component.ContextAwareComponent;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.util.JRCloneUtils;

/**
 * The Charteon chart component.
 *
 * <p>
 * Used inside a {@code <element kind="component">} element:
 *
 * <pre>{@code
 * <element kind="component" x="0" y="0" width="400" height="300">
 *   <component kind="chart" chartType="bar">
 *     <titleExpression><![CDATA["Monthly Sales"]]></titleExpression>
 *     <categoryDataset>
 *       <series>
 *         <seriesExpression><![CDATA["2026"]]></seriesExpression>
 *         <categoryExpression><![CDATA[$F{month}]]></categoryExpression>
 *         <valueExpression><![CDATA[$F{amount}]]></valueExpression>
 *       </series>
 *     </categoryDataset>
 *   </component>
 * </element>
 * }</pre>
 */
@JsonPropertyOrder({
	"chartType",
	"theme",
	"showLegend",
	"showValues",
	"stacked",
	"horizontal",
	"filled",
	"smooth",
	"step",
	"innerRadius",
	"roseType",
	"polar",
	"symbol",
	"mapName",
	"graphLayout",
	"colors",
	"colorByCategory",
	"valueFormat",
	"groupingSeparator",
	"decimalSeparator",
	"xAxisTitle",
	"yAxisTitle",
	"secondaryAxisTitle",
	"evaluationTime",
	"evaluationGroup",
	"titleExpression",
	"subtitleExpression",
	"categoryDataset",
	"xyDataset",
	"hierarchyDataset",
	"relationDataset",
	"boxplotDataset",
	"candlestickDataset",
	"optionExpression"
	})
@JsonTypeName(ChartComponent.COMPONENT_NAME)
public class ChartComponent implements ContextAwareComponent, JRCloneable, Serializable
{
	private static final long serialVersionUID = JRConstants.SERIAL_VERSION_UID;

	/**
	 * The component name, i.e. the value of the {@code kind} attribute of the
	 * {@code <component>} JRXML element.
	 */
	public static final String COMPONENT_NAME = "chart";

	/**
	 * The namespace of the generic print element produced at fill time.
	 */
	public static final String ELEMENT_NAMESPACE = "http://charteon.tech/charts";

	/**
	 * The name of the generic print element produced at fill time.
	 */
	public static final String ELEMENT_NAME = "chart";

	public static final JRGenericElementType CHART_PRINT_ELEMENT_TYPE =
		new JRGenericElementType(ELEMENT_NAMESPACE, ELEMENT_NAME);

	/** Generic print element parameter: the ECharts option object as JSON string. */
	public static final String PARAMETER_OPTION = "option";
	/** Generic print element parameter: the chart theme name (optional). */
	public static final String PARAMETER_THEME = "theme";
	/** Generic print element parameter: the Charteon chart type name (informational). */
	public static final String PARAMETER_CHART_TYPE = "chartType";
	/** Generic print element parameter: the registered geo map name (map charts only). */
	public static final String PARAMETER_MAP_NAME = "mapName";

	private ChartTypeEnum chartType;
	private String theme;
	private Boolean showLegend;
	private Boolean showValues;
	private Boolean stacked;
	private Boolean horizontal;
	private Boolean filled;
	private Boolean smooth;
	private String step;
	private String innerRadius;
	private String roseType;
	private Boolean polar;
	private String symbol;
	private String mapName;
	private String graphLayout;
	private String colors;
	private Boolean colorByCategory;
	private String valueFormat;
	private String groupingSeparator;
	private String decimalSeparator;
	private String xAxisTitle;
	private String yAxisTitle;
	private String secondaryAxisTitle;
	private EvaluationTimeEnum evaluationTime;
	private String evaluationGroup;

	private JRExpression titleExpression;
	private JRExpression subtitleExpression;
	private ChartCategoryDataset categoryDataset;
	private ChartXyDataset xyDataset;
	private ChartHierarchyDataset hierarchyDataset;
	private ChartRelationDataset relationDataset;
	private ChartBoxplotDataset boxplotDataset;
	private ChartCandlestickDataset candlestickDataset;
	private JRExpression optionExpression;

	private ComponentContext context;

	public ChartComponent()
	{
	}

	protected ChartComponent(ChartComponent component, JRBaseObjectFactory baseFactory)
	{
		this.chartType = component.getChartType();
		this.theme = component.getTheme();
		this.showLegend = component.getShowLegend();
		this.showValues = component.getShowValues();
		this.stacked = component.getStacked();
		this.horizontal = component.getHorizontal();
		this.filled = component.getFilled();
		this.smooth = component.getSmooth();
		this.step = component.getStep();
		this.innerRadius = component.getInnerRadius();
		this.roseType = component.getRoseType();
		this.polar = component.getPolar();
		this.symbol = component.getSymbol();
		this.mapName = component.getMapName();
		this.graphLayout = component.getGraphLayout();
		this.colors = component.getColors();
		this.colorByCategory = component.getColorByCategory();
		this.valueFormat = component.getValueFormat();
		this.groupingSeparator = component.getGroupingSeparator();
		this.decimalSeparator = component.getDecimalSeparator();
		this.xAxisTitle = component.getXAxisTitle();
		this.yAxisTitle = component.getYAxisTitle();
		this.secondaryAxisTitle = component.getSecondaryAxisTitle();
		this.evaluationTime = component.getEvaluationTime();
		this.evaluationGroup = component.getEvaluationGroup();
		this.context = new BaseComponentContext(component.getContext(), baseFactory);

		this.titleExpression = baseFactory.getExpression(component.getTitleExpression());
		this.subtitleExpression = baseFactory.getExpression(component.getSubtitleExpression());
		this.optionExpression = baseFactory.getExpression(component.getOptionExpression());

		if (component.getCategoryDataset() != null)
		{
			this.categoryDataset = new ChartCategoryDataset(component.getCategoryDataset(), baseFactory);
		}
		if (component.getXyDataset() != null)
		{
			this.xyDataset = new ChartXyDataset(component.getXyDataset(), baseFactory);
		}
		if (component.getHierarchyDataset() != null)
		{
			this.hierarchyDataset = new ChartHierarchyDataset(component.getHierarchyDataset(), baseFactory);
		}
		if (component.getRelationDataset() != null)
		{
			this.relationDataset = new ChartRelationDataset(component.getRelationDataset(), baseFactory);
		}
		if (component.getBoxplotDataset() != null)
		{
			this.boxplotDataset = new ChartBoxplotDataset(component.getBoxplotDataset(), baseFactory);
		}
		if (component.getCandlestickDataset() != null)
		{
			this.candlestickDataset = new ChartCandlestickDataset(component.getCandlestickDataset(), baseFactory);
		}
	}

	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public ChartTypeEnum getChartType()
	{
		return chartType;
	}

	public void setChartType(ChartTypeEnum chartType)
	{
		this.chartType = chartType;
	}

	/**
	 * The ECharts theme name. The themes bundled with Apache ECharts
	 * ({@code dark}) are available out of the box.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getTheme()
	{
		return theme;
	}

	public void setTheme(String theme)
	{
		this.theme = theme;
	}

	/**
	 * Whether the chart legend is displayed. Defaults to {@code true} when
	 * there is more than one series.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public Boolean getShowLegend()
	{
		return showLegend;
	}

	public void setShowLegend(Boolean showLegend)
	{
		this.showLegend = showLegend;
	}

	/**
	 * Whether each data point is labeled with its value directly in the
	 * chart (e.g. on top of every bar). Applies to bar, line and
	 * pictorialBar charts; defaults to {@code false}, matching ECharts. For
	 * per-record custom labels use the series {@code labelExpression}
	 * instead, which overrides this setting.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public Boolean getShowValues()
	{
		return showValues;
	}

	public void setShowValues(Boolean showValues)
	{
		this.showValues = showValues;
	}

	/**
	 * For bar/line/area charts: whether the series are stacked.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public Boolean getStacked()
	{
		return stacked;
	}

	public void setStacked(Boolean stacked)
	{
		this.stacked = stacked;
	}

	/**
	 * For bar/line charts: whether the category axis runs vertically
	 * (horizontal bars).
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public Boolean getHorizontal()
	{
		return horizontal;
	}

	public void setHorizontal(Boolean horizontal)
	{
		this.horizontal = horizontal;
	}

	/**
	 * For line charts: whether the area below the line is filled
	 * (ECharts {@code areaStyle}).
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public Boolean getFilled()
	{
		return filled;
	}

	public void setFilled(Boolean filled)
	{
		this.filled = filled;
	}

	/**
	 * For line charts: whether the line is smoothed.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public Boolean getSmooth()
	{
		return smooth;
	}

	public void setSmooth(Boolean smooth)
	{
		this.smooth = smooth;
	}

	/**
	 * For line charts: renders a step line; one of {@code start},
	 * {@code middle} or {@code end}.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getStep()
	{
		return step;
	}

	public void setStep(String step)
	{
		this.step = step;
	}

	/**
	 * For pie charts: the inner radius (e.g. {@code "40%"}), which turns the
	 * pie into a doughnut/ring chart.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getInnerRadius()
	{
		return innerRadius;
	}

	public void setInnerRadius(String innerRadius)
	{
		this.innerRadius = innerRadius;
	}

	/**
	 * For pie charts: the Nightingale/rose mode; one of {@code radius} or
	 * {@code area}.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getRoseType()
	{
		return roseType;
	}

	public void setRoseType(String roseType)
	{
		this.roseType = roseType;
	}

	/**
	 * For bar/line charts: whether the chart uses polar instead of cartesian
	 * coordinates.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public Boolean getPolar()
	{
		return polar;
	}

	public void setPolar(Boolean polar)
	{
		this.polar = polar;
	}

	/**
	 * For pictorialBar/scatter charts: the ECharts symbol name (e.g.
	 * {@code circle}, {@code rect}, {@code triangle}, {@code image://...}).
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getSymbol()
	{
		return symbol;
	}

	public void setSymbol(String symbol)
	{
		this.symbol = symbol;
	}

	/**
	 * For map charts: the name of the registered geo map. Defaults to
	 * {@code world}, which Charteon bundles; custom maps are registered
	 * through {@code tech.charteon.util.CharteonMaps} or as a classpath
	 * resource {@code tech/charteon/maps/<name>.geo.json}.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getMapName()
	{
		return mapName;
	}

	public void setMapName(String mapName)
	{
		this.mapName = mapName;
	}

	/**
	 * For graph charts: the layout; one of {@code circular} (default),
	 * {@code force} or {@code none}. The default is {@code circular} because
	 * it is deterministic and renders identically in server-side (PDF) and
	 * browser (HTML) output.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getGraphLayout()
	{
		return graphLayout;
	}

	public void setGraphLayout(String graphLayout)
	{
		this.graphLayout = graphLayout;
	}

	/**
	 * Optional chart color palette as a comma-separated list of CSS/ECharts
	 * colors (e.g. {@code "#5470c6,#91cc75,#fac858"}); cycled across series (or
	 * across categories when {@code colorByCategory} is set). {@code null} uses
	 * the ECharts/theme default palette.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getColors()
	{
		return colors;
	}

	public void setColors(String colors)
	{
		this.colors = colors;
	}

	/**
	 * Whether each category gets its own palette color (ECharts
	 * {@code colorBy: "data"}) instead of one color per series. Useful for a
	 * single-series bar chart where every bar should be a different color.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public Boolean getColorByCategory()
	{
		return colorByCategory;
	}

	public void setColorByCategory(Boolean colorByCategory)
	{
		this.colorByCategory = colorByCategory;
	}

	/**
	 * Optional number format applied to the value axis labels and the value
	 * data labels (and the tooltip in HTML). A compact pattern with an optional
	 * prefix/suffix around a numeric mask, e.g. {@code "#,##0.00"} (grouped, two
	 * decimals), {@code "€ #,##0"} (currency prefix) or {@code "#,##0 %"}
	 * (percent suffix). Grouping/decimal separators default to {@code ,} and
	 * {@code .}; override with {@code groupingSeparator}/{@code decimalSeparator}
	 * (e.g. {@code .} and {@code ,} for German). Applies to axis-based types
	 * (bar, line) and pie labels.
	 */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getValueFormat()
	{
		return valueFormat;
	}

	public void setValueFormat(String valueFormat)
	{
		this.valueFormat = valueFormat;
	}

	/** The thousands grouping separator for {@code valueFormat}; default {@code ,}. */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getGroupingSeparator()
	{
		return groupingSeparator;
	}

	public void setGroupingSeparator(String groupingSeparator)
	{
		this.groupingSeparator = groupingSeparator;
	}

	/** The decimal separator for {@code valueFormat}; default {@code .}. */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getDecimalSeparator()
	{
		return decimalSeparator;
	}

	public void setDecimalSeparator(String decimalSeparator)
	{
		this.decimalSeparator = decimalSeparator;
	}

	/** Optional title (name) for the horizontal axis of axis-based charts. */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true, localName = "xAxisTitle")
	public String getXAxisTitle()
	{
		return xAxisTitle;
	}

	public void setXAxisTitle(String xAxisTitle)
	{
		this.xAxisTitle = xAxisTitle;
	}

	/** Optional title (name) for the (primary) vertical axis of axis-based charts. */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true, localName = "yAxisTitle")
	public String getYAxisTitle()
	{
		return yAxisTitle;
	}

	public void setYAxisTitle(String yAxisTitle)
	{
		this.yAxisTitle = yAxisTitle;
	}

	/** Optional title (name) for the secondary value axis (combo charts). */
	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getSecondaryAxisTitle()
	{
		return secondaryAxisTitle;
	}

	public void setSecondaryAxisTitle(String secondaryAxisTitle)
	{
		this.secondaryAxisTitle = secondaryAxisTitle;
	}

	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public EvaluationTimeEnum getEvaluationTime()
	{
		return evaluationTime;
	}

	public void setEvaluationTime(EvaluationTimeEnum evaluationTime)
	{
		this.evaluationTime = evaluationTime;
	}

	@JsonInclude(Include.NON_NULL)
	@JacksonXmlProperty(isAttribute = true)
	public String getEvaluationGroup()
	{
		return evaluationGroup;
	}

	public void setEvaluationGroup(String evaluationGroup)
	{
		this.evaluationGroup = evaluationGroup;
	}

	/**
	 * Expression providing the chart title.
	 */
	public JRExpression getTitleExpression()
	{
		return titleExpression;
	}

	public void setTitleExpression(JRExpression titleExpression)
	{
		this.titleExpression = titleExpression;
	}

	/**
	 * Expression providing the chart subtitle.
	 */
	public JRExpression getSubtitleExpression()
	{
		return subtitleExpression;
	}

	public void setSubtitleExpression(JRExpression subtitleExpression)
	{
		this.subtitleExpression = subtitleExpression;
	}

	public ChartCategoryDataset getCategoryDataset()
	{
		return categoryDataset;
	}

	public void setCategoryDataset(ChartCategoryDataset categoryDataset)
	{
		this.categoryDataset = categoryDataset;
	}

	public ChartXyDataset getXyDataset()
	{
		return xyDataset;
	}

	public void setXyDataset(ChartXyDataset xyDataset)
	{
		this.xyDataset = xyDataset;
	}

	public ChartHierarchyDataset getHierarchyDataset()
	{
		return hierarchyDataset;
	}

	public void setHierarchyDataset(ChartHierarchyDataset hierarchyDataset)
	{
		this.hierarchyDataset = hierarchyDataset;
	}

	public ChartRelationDataset getRelationDataset()
	{
		return relationDataset;
	}

	public void setRelationDataset(ChartRelationDataset relationDataset)
	{
		this.relationDataset = relationDataset;
	}

	public ChartBoxplotDataset getBoxplotDataset()
	{
		return boxplotDataset;
	}

	public void setBoxplotDataset(ChartBoxplotDataset boxplotDataset)
	{
		this.boxplotDataset = boxplotDataset;
	}

	public ChartCandlestickDataset getCandlestickDataset()
	{
		return candlestickDataset;
	}

	public void setCandlestickDataset(ChartCandlestickDataset candlestickDataset)
	{
		this.candlestickDataset = candlestickDataset;
	}

	/**
	 * The raw option escape hatch: an expression producing a
	 * {@code java.lang.String} that contains an Apache ECharts option object
	 * as JSON. If present, it is deep-merged over the option generated from
	 * the declarative model (the raw option wins). If no dataset is declared,
	 * the raw option is used as-is, which makes every ECharts series type
	 * available.
	 */
	public JRExpression getOptionExpression()
	{
		return optionExpression;
	}

	public void setOptionExpression(JRExpression optionExpression)
	{
		this.optionExpression = optionExpression;
	}

	@Override
	public ComponentContext getContext()
	{
		return context;
	}

	@Override
	public void setContext(ComponentContext context)
	{
		this.context = context;
	}

	@Override
	public Object clone()
	{
		try
		{
			ChartComponent clone = (ChartComponent) super.clone();
			clone.titleExpression = JRCloneUtils.nullSafeClone(titleExpression);
			clone.subtitleExpression = JRCloneUtils.nullSafeClone(subtitleExpression);
			clone.optionExpression = JRCloneUtils.nullSafeClone(optionExpression);
			clone.categoryDataset = categoryDataset == null ? null : (ChartCategoryDataset) categoryDataset.clone();
			clone.xyDataset = xyDataset == null ? null : (ChartXyDataset) xyDataset.clone();
			clone.hierarchyDataset = hierarchyDataset == null ? null : (ChartHierarchyDataset) hierarchyDataset.clone();
			clone.relationDataset = relationDataset == null ? null : (ChartRelationDataset) relationDataset.clone();
			clone.boxplotDataset = boxplotDataset == null ? null : (ChartBoxplotDataset) boxplotDataset.clone();
			clone.candlestickDataset = candlestickDataset == null ? null : (ChartCandlestickDataset) candlestickDataset.clone();
			return clone;
		}
		catch (CloneNotSupportedException e)
		{
			throw new JRRuntimeException(e);
		}
	}
}
