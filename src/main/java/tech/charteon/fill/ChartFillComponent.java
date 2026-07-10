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
package tech.charteon.fill;

import net.sf.jasperreports.engine.JRComponentElement;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.component.BaseFillComponent;
import net.sf.jasperreports.engine.component.FillPrepareResult;
import net.sf.jasperreports.engine.fill.JRFillObjectFactory;
import net.sf.jasperreports.engine.fill.JRTemplateGenericElement;
import net.sf.jasperreports.engine.fill.JRTemplateGenericPrintElement;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;

import tech.charteon.component.ChartComponent;
import tech.charteon.component.ChartTypeEnum;
import tech.charteon.model.ChartData;
import tech.charteon.model.EChartsOptionBuilder;

/**
 * Fill-time component: evaluates the dataset and settings expressions, builds
 * the ECharts option JSON and stores it on a generic print element. Rendering
 * happens later, per export format, in the generic element handlers.
 */
public class ChartFillComponent extends BaseFillComponent
{
	private final ChartComponent component;
	private final FillCategoryDataset categoryDataset;
	private final FillXyDataset xyDataset;
	private final FillHierarchyDataset hierarchyDataset;
	private final FillRelationDataset relationDataset;
	private final FillBoxplotDataset boxplotDataset;
	private final FillCandlestickDataset candlestickDataset;

	private String optionJson;

	public ChartFillComponent(ChartComponent component, JRFillObjectFactory factory)
	{
		this.component = component;

		if (component.getCategoryDataset() != null)
		{
			this.categoryDataset = new FillCategoryDataset(component.getCategoryDataset(), factory);
			factory.registerElementDataset(this.categoryDataset);
		}
		else
		{
			this.categoryDataset = null;
		}

		if (component.getXyDataset() != null)
		{
			this.xyDataset = new FillXyDataset(component.getXyDataset(), factory);
			factory.registerElementDataset(this.xyDataset);
		}
		else
		{
			this.xyDataset = null;
		}

		if (component.getHierarchyDataset() != null)
		{
			this.hierarchyDataset = new FillHierarchyDataset(component.getHierarchyDataset(), factory);
			factory.registerElementDataset(this.hierarchyDataset);
		}
		else
		{
			this.hierarchyDataset = null;
		}

		if (component.getRelationDataset() != null)
		{
			this.relationDataset = new FillRelationDataset(component.getRelationDataset(), factory);
			factory.registerElementDataset(this.relationDataset);
		}
		else
		{
			this.relationDataset = null;
		}

		if (component.getBoxplotDataset() != null)
		{
			this.boxplotDataset = new FillBoxplotDataset(component.getBoxplotDataset(), factory);
			factory.registerElementDataset(this.boxplotDataset);
		}
		else
		{
			this.boxplotDataset = null;
		}

		if (component.getCandlestickDataset() != null)
		{
			this.candlestickDataset = new FillCandlestickDataset(component.getCandlestickDataset(), factory);
			factory.registerElementDataset(this.candlestickDataset);
		}
		else
		{
			this.candlestickDataset = null;
		}
	}

	protected boolean isEvaluateNow()
	{
		// null check instead of EvaluationTimeEnum.getValueOrDefault(..): that
		// helper only exists in JR 7; this class is also reused on JR 6.x via
		// the charteon-jr6-adapter (charteon-studio project)
		EvaluationTimeEnum evaluationTime = component.getEvaluationTime();
		return evaluationTime == null || evaluationTime == EvaluationTimeEnum.NOW;
	}

	@Override
	public void evaluate(byte evaluation) throws JRException
	{
		if (isEvaluateNow())
		{
			evaluateChart(evaluation);
		}
	}

	protected void evaluateChart(byte evaluation) throws JRException
	{
		String title = asString(fillContext.evaluate(component.getTitleExpression(), evaluation));
		String subtitle = asString(fillContext.evaluate(component.getSubtitleExpression(), evaluation));
		String rawOption = asString(fillContext.evaluate(component.getOptionExpression(), evaluation));

		if (categoryDataset != null)
		{
			categoryDataset.evaluateDatasetRun(evaluation);
			categoryDataset.finishDataset();
		}
		if (xyDataset != null)
		{
			xyDataset.evaluateDatasetRun(evaluation);
			xyDataset.finishDataset();
		}
		if (hierarchyDataset != null)
		{
			hierarchyDataset.evaluateDatasetRun(evaluation);
			hierarchyDataset.finishDataset();
		}
		if (relationDataset != null)
		{
			relationDataset.evaluateDatasetRun(evaluation);
			relationDataset.finishDataset();
		}
		if (boxplotDataset != null)
		{
			boxplotDataset.evaluateDatasetRun(evaluation);
			boxplotDataset.finishDataset();
		}
		if (candlestickDataset != null)
		{
			candlestickDataset.evaluateDatasetRun(evaluation);
			candlestickDataset.finishDataset();
		}

		ChartData data = new ChartData(
			categoryDataset == null ? null : categoryDataset.getData(),
			xyDataset == null ? null : xyDataset.getData(),
			hierarchyDataset == null ? null : hierarchyDataset.getData(),
			relationDataset == null ? null : relationDataset.getData(),
			boxplotDataset == null ? null : boxplotDataset.getData(),
			candlestickDataset == null ? null : candlestickDataset.getData());

		optionJson = EChartsOptionBuilder.buildOption(component, title, subtitle, data, rawOption);
	}

	private static String asString(Object value)
	{
		return value == null ? null : value.toString();
	}

	@Override
	public JRPrintElement fill()
	{
		JRComponentElement element = fillContext.getComponentElement();

		JRTemplateGenericElement template = new JRTemplateGenericElement(
			fillContext.getElementOrigin(),
			fillContext.getDefaultStyleProvider(),
			ChartComponent.CHART_PRINT_ELEMENT_TYPE);
		template.setStyle(fillContext.getElementStyle());
		template = deduplicate(template);

		JRTemplateGenericPrintElement printElement =
			new JRTemplateGenericPrintElement(template, printElementOriginator);
		printElement.setUUID(element.getUUID());
		printElement.setX(element.getX());
		printElement.setY(fillContext.getElementPrintY());
		printElement.setWidth(element.getWidth());
		printElement.setHeight(element.getHeight());

		if (isEvaluateNow())
		{
			copy(printElement);
		}
		else
		{
			fillContext.registerDelayedEvaluation(printElement,
				component.getEvaluationTime(), component.getEvaluationGroup());
		}

		return printElement;
	}

	@Override
	public void evaluateDelayedElement(JRPrintElement element, byte evaluation) throws JRException
	{
		evaluateChart(evaluation);
		copy((JRGenericPrintElement) element);
	}

	protected void copy(JRGenericPrintElement printElement)
	{
		printElement.setParameterValue(ChartComponent.PARAMETER_OPTION, optionJson);
		if (component.getTheme() != null)
		{
			printElement.setParameterValue(ChartComponent.PARAMETER_THEME, component.getTheme());
		}
		if (component.getChartType() != null)
		{
			printElement.setParameterValue(ChartComponent.PARAMETER_CHART_TYPE,
				component.getChartType().getName());
		}
		if (component.getChartType() != null
			&& component.getChartType().getBaseType() == ChartTypeEnum.MAP)
		{
			printElement.setParameterValue(ChartComponent.PARAMETER_MAP_NAME,
				component.getMapName() == null ? "world" : component.getMapName());
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public FillPrepareResult prepare(int availableHeight)
	{
		return prepare(availableHeight, true);
	}

	@Override
	public FillPrepareResult prepare(int availableHeight, boolean isOverflowAllowed)
	{
		return FillPrepareResult.PRINT_NO_STRETCH;
	}
}
