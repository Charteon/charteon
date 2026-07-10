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

import net.sf.jasperreports.engine.JRExpressionCollector;
import net.sf.jasperreports.engine.base.JRBaseObjectFactory;
import net.sf.jasperreports.engine.component.Component;
import net.sf.jasperreports.engine.component.ComponentCompiler;
import net.sf.jasperreports.engine.design.JRVerifier;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;

/**
 * Compiles the Charteon chart component: collects the expressions so that they
 * are compiled like the ones of native report elements, verifies the component
 * at compile time and produces the compiled (base) component instance.
 */
public class ChartComponentCompiler implements ComponentCompiler
{

	@Override
	public void collectExpressions(Component component, JRExpressionCollector collector)
	{
		ChartComponent chart = (ChartComponent) component;

		collector.addExpression(chart.getTitleExpression());
		collector.addExpression(chart.getSubtitleExpression());
		collector.addExpression(chart.getOptionExpression());

		collectExpressions(chart.getCategoryDataset(), collector);
		collectExpressions(chart.getXyDataset(), collector);
		collectExpressions(chart.getHierarchyDataset(), collector);
		collectExpressions(chart.getRelationDataset(), collector);
		collectExpressions(chart.getBoxplotDataset(), collector);
		collectExpressions(chart.getCandlestickDataset(), collector);
	}

	public static void collectExpressions(ChartCategoryDataset dataset, JRExpressionCollector collector)
	{
		if (dataset != null)
		{
			collector.collect(dataset);

			// series expressions are evaluated in the context of the dataset
			// (which may be a subdataset run), hence the sub-collector
			JRExpressionCollector seriesCollector = collector.getCollector(dataset);
			for (CategorySeries series : dataset.getSeriesList())
			{
				seriesCollector.addExpression(series.getSeriesExpression());
				seriesCollector.addExpression(series.getCategoryExpression());
				seriesCollector.addExpression(series.getValueExpression());
				seriesCollector.addExpression(series.getLabelExpression());
			}
		}
	}

	public static void collectExpressions(ChartXyDataset dataset, JRExpressionCollector collector)
	{
		if (dataset != null)
		{
			collector.collect(dataset);

			JRExpressionCollector seriesCollector = collector.getCollector(dataset);
			for (XySeries series : dataset.getSeriesList())
			{
				seriesCollector.addExpression(series.getSeriesExpression());
				seriesCollector.addExpression(series.getXValueExpression());
				seriesCollector.addExpression(series.getYValueExpression());
				seriesCollector.addExpression(series.getSizeExpression());
				seriesCollector.addExpression(series.getLabelExpression());
			}
		}
	}

	public static void collectExpressions(ChartHierarchyDataset dataset, JRExpressionCollector collector)
	{
		if (dataset != null)
		{
			collector.collect(dataset);

			JRExpressionCollector datasetCollector = collector.getCollector(dataset);
			datasetCollector.addExpression(dataset.getNameExpression());
			datasetCollector.addExpression(dataset.getParentExpression());
			datasetCollector.addExpression(dataset.getValueExpression());
		}
	}

	public static void collectExpressions(ChartRelationDataset dataset, JRExpressionCollector collector)
	{
		if (dataset != null)
		{
			collector.collect(dataset);

			JRExpressionCollector datasetCollector = collector.getCollector(dataset);
			datasetCollector.addExpression(dataset.getSourceExpression());
			datasetCollector.addExpression(dataset.getTargetExpression());
			datasetCollector.addExpression(dataset.getValueExpression());
			datasetCollector.addExpression(dataset.getSourceXExpression());
			datasetCollector.addExpression(dataset.getSourceYExpression());
			datasetCollector.addExpression(dataset.getTargetXExpression());
			datasetCollector.addExpression(dataset.getTargetYExpression());
		}
	}

	public static void collectExpressions(ChartBoxplotDataset dataset, JRExpressionCollector collector)
	{
		if (dataset != null)
		{
			collector.collect(dataset);

			JRExpressionCollector datasetCollector = collector.getCollector(dataset);
			datasetCollector.addExpression(dataset.getCategoryExpression());
			datasetCollector.addExpression(dataset.getMinExpression());
			datasetCollector.addExpression(dataset.getQ1Expression());
			datasetCollector.addExpression(dataset.getMedianExpression());
			datasetCollector.addExpression(dataset.getQ3Expression());
			datasetCollector.addExpression(dataset.getMaxExpression());
		}
	}

	public static void collectExpressions(ChartCandlestickDataset dataset, JRExpressionCollector collector)
	{
		if (dataset != null)
		{
			collector.collect(dataset);

			JRExpressionCollector datasetCollector = collector.getCollector(dataset);
			datasetCollector.addExpression(dataset.getCategoryExpression());
			datasetCollector.addExpression(dataset.getOpenExpression());
			datasetCollector.addExpression(dataset.getCloseExpression());
			datasetCollector.addExpression(dataset.getLowExpression());
			datasetCollector.addExpression(dataset.getHighExpression());
		}
	}

	@Override
	public void verify(Component component, JRVerifier verifier)
	{
		ChartComponent chart = (ChartComponent) component;

		verifyEvaluation(verifier, chart);

		ChartTypeEnum chartType = chart.getChartType();
		boolean hasRawOption = chart.getOptionExpression() != null;

		if (chartType == null && !hasRawOption)
		{
			verifier.addBrokenRule(
				"Charteon chart: either a chartType attribute or an optionExpression is required",
				chart);
			return;
		}

		if (chartType == ChartTypeEnum.CUSTOM && !hasRawOption)
		{
			verifier.addBrokenRule(
				"Charteon chart: chart type \"custom\" requires an optionExpression"
					+ " providing the renderItem callback (as a \"js:function...\" string)",
				chart);
		}

		if (chartType != null && !hasRawOption)
		{
			switch (chartType.getDatasetKind())
			{
				case CATEGORY:
					if (chart.getCategoryDataset() == null
						|| chart.getCategoryDataset().getSeriesList().isEmpty())
					{
						verifier.addBrokenRule(
							"Charteon chart: chart type \"" + chartType.getName()
								+ "\" requires a categoryDataset with at least one series"
								+ " (or an optionExpression)",
							chart);
					}
					break;
				case XY:
					if (chart.getXyDataset() == null
						|| chart.getXyDataset().getSeriesList().isEmpty())
					{
						verifier.addBrokenRule(
							"Charteon chart: chart type \"" + chartType.getName()
								+ "\" requires an xyDataset with at least one series"
								+ " (or an optionExpression)",
							chart);
					}
					break;
				case HIERARCHY:
					if (chart.getHierarchyDataset() == null)
					{
						verifier.addBrokenRule(
							"Charteon chart: chart type \"" + chartType.getName()
								+ "\" requires a hierarchyDataset (or an optionExpression)",
							chart);
					}
					break;
				case RELATION:
					if (chart.getRelationDataset() == null)
					{
						verifier.addBrokenRule(
							"Charteon chart: chart type \"" + chartType.getName()
								+ "\" requires a relationDataset (or an optionExpression)",
							chart);
					}
					break;
				case BOXPLOT:
					if (chart.getBoxplotDataset() == null)
					{
						verifier.addBrokenRule(
							"Charteon chart: chart type \"boxplot\" requires a boxplotDataset"
								+ " (or an optionExpression)",
							chart);
					}
					break;
				case CANDLESTICK:
					if (chart.getCandlestickDataset() == null)
					{
						verifier.addBrokenRule(
							"Charteon chart: chart type \"candlestick\" requires a"
								+ " candlestickDataset (or an optionExpression)",
							chart);
					}
					break;
			}
		}

		if (chartType == ChartTypeEnum.LINES && chart.getRelationDataset() != null
			&& (chart.getRelationDataset().getSourceXExpression() == null
				|| chart.getRelationDataset().getSourceYExpression() == null
				|| chart.getRelationDataset().getTargetXExpression() == null
				|| chart.getRelationDataset().getTargetYExpression() == null))
		{
			verifier.addBrokenRule(
				"Charteon chart: chart type \"lines\" requires the relationDataset"
					+ " coordinate expressions (sourceX/sourceY/targetX/targetY)",
				chart);
		}

		if (chart.getCategoryDataset() != null)
		{
			verifier.verifyElementDataset(chart.getCategoryDataset());
		}
		if (chart.getXyDataset() != null)
		{
			verifier.verifyElementDataset(chart.getXyDataset());
		}
		if (chart.getHierarchyDataset() != null)
		{
			verifier.verifyElementDataset(chart.getHierarchyDataset());
		}
		if (chart.getRelationDataset() != null)
		{
			verifier.verifyElementDataset(chart.getRelationDataset());
		}
		if (chart.getBoxplotDataset() != null)
		{
			verifier.verifyElementDataset(chart.getBoxplotDataset());
		}
		if (chart.getCandlestickDataset() != null)
		{
			verifier.verifyElementDataset(chart.getCandlestickDataset());
		}
	}

	protected void verifyEvaluation(JRVerifier verifier, ChartComponent chart)
	{
		EvaluationTimeEnum evaluationTime = chart.getEvaluationTime();
		if (evaluationTime == EvaluationTimeEnum.AUTO)
		{
			verifier.addBrokenRule("Charteon chart evaluation time cannot be Auto", chart);
		}
		else if (evaluationTime == EvaluationTimeEnum.GROUP)
		{
			String groupName = chart.getEvaluationGroup();
			if (groupName == null)
			{
				verifier.addBrokenRule("Evaluation group not set for Charteon chart", chart);
			}
			else
			{
				JasperDesign report = verifier.getReportDesign();
				if (!report.getGroupsMap().containsKey(groupName))
				{
					verifier.addBrokenRule("Charteon chart evaluation group " + groupName
						+ " not found in report", chart);
				}
			}
		}
	}

	@Override
	public Component toCompiledComponent(Component component, JRBaseObjectFactory baseFactory)
	{
		ChartComponent chart = (ChartComponent) component;
		return new ChartComponent(chart, baseFactory);
	}
}
