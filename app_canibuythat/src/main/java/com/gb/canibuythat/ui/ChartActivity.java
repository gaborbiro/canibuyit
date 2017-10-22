package com.gb.canibuythat.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.gb.canibuythat.R;
import com.gb.canibuythat.UserPreferences;
import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.model.Spending;
import com.gb.canibuythat.presenter.BasePresenter;
import com.gb.canibuythat.repository.BalanceCalculator;
import com.gb.canibuythat.db.SpendingDBHelper;
import com.gb.canibuythat.ui.model.BalanceReading;
import com.gb.canibuythat.util.DateUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.j256.ormlite.dao.Dao;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class ChartActivity extends BaseActivity implements OnChartValueSelectedListener {

    @Inject Dao<Spending, Integer> spendingDao;
    @Inject UserPreferences userPreferences;

    private static SimpleDateFormat MONTH_ONLY = new SimpleDateFormat("MMM.dd");
    private static SimpleDateFormat MONTH_YEAR = new SimpleDateFormat("yyyy.MMM");
    protected BarChart chart;

    public static void show(Context context) {
        Intent i = new Intent(context, ChartActivity.class);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);
        chart = (BarChart) findViewById(R.id.chart);
        chart.setOnChartValueSelectedListener(this);
        chart.setDescription("");
        chart.setMaxVisibleValueCount(6);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(false);

        // change the position of the y-labels
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setValueFormatter(new MyYAxisValueFormatter());
        chart.getAxisRight().setEnabled(false);

        XAxis xLabels = chart.getXAxis();
        xLabels.setPosition(XAxis.XAxisPosition.TOP);

        Legend l = chart.getLegend();
        l.setPosition(Legend.LegendPosition.BELOW_CHART_RIGHT);
        l.setFormSize(8f);
        l.setFormToTextSpace(4f);
        l.setXEntrySpace(6f);

        new CalculateProjectionsTask().execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected BasePresenter inject() {
        Injector.INSTANCE.getGraph().inject(this);
        return null;
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        ProjectionItem projectionItem = (ProjectionItem) e.getData();
        final Date estimateDate = userPreferences.getEstimateDate();
        String readingDateStr = estimateDate != null ? DateUtils.getFORMAT_MONTH_DAY_YR().format(estimateDate) : getString(R.string.today);
        Toast.makeText(this, getString(R.string.estimate_at_date, projectionItem.bestCase,
                projectionItem.worstCase, readingDateStr), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected() {
        // nothing to do
    }

    class ProjectionItem {
        Date month;
        Float bestCase;
        Float worstCase;

        ProjectionItem(Date month, Float bestCase, Float worstCase) {
            this.month = month;
            this.bestCase = bestCase;
            this.worstCase = worstCase;
        }
    }

    private class CalculateProjectionsTask
            extends AsyncTask<Void, Void, ProjectionItem[]> {

        @Override
        protected ProjectionItem[] doInBackground(Void... params) {
            Calendar currTarget = Calendar.getInstance();
            Calendar oneYearLater = Calendar.getInstance();
            oneYearLater.add(Calendar.YEAR, 1);
            try {
                BalanceReading balanceReading = userPreferences.getBalanceReading();
                Date startDate;

                if (balanceReading != null) {
                    currTarget.setTime(balanceReading.getWhen());
                    currTarget.add(Calendar.DAY_OF_MONTH, 1);
                    startDate = balanceReading.getWhen();
                } else {
                    startDate = null;
                }
                List<ProjectionItem> result = new ArrayList<>();

                do {
                    float bestCase = 0;
                    float worstCase = 0;

                    for (com.gb.canibuythat.model.Spending item : spendingDao) {
                        if (item.getEnabled()) {
                            BalanceCalculator.BalanceResult br = BalanceCalculator.INSTANCE.getEstimatedBalance(item, startDate, currTarget.getTime());
                            bestCase += br.getDefinitely();
                            worstCase += br.getMaybeEvenThisMuch();
                        }
                    }

                    if (balanceReading != null) {
                        bestCase += balanceReading.getBalance();
                        worstCase += balanceReading.getBalance();
                    }
                    result.add(new ProjectionItem(currTarget.getTime(), bestCase, worstCase));
                    currTarget.add(Calendar.WEEK_OF_YEAR, 1);
                } while (currTarget.before(oneYearLater));

                return result.toArray(new ProjectionItem[result.size()]);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(ChartActivity.this, "Error calculating projection. See logs.", Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ProjectionItem[] projectionItems) {
            setData(projectionItems);
        }
    }

    public void setData(ProjectionItem[] projectionItems) {
        ArrayList<String> xValues = new ArrayList<>();
        int lastYear = Calendar.getInstance().get(Calendar.YEAR);
        Calendar c = Calendar.getInstance();
        for (ProjectionItem projectionItem : projectionItems) {
            c.setTime(projectionItem.month);
            String label;

            if (c.get(Calendar.YEAR) != lastYear) {
                lastYear = c.get(Calendar.YEAR);
                label = MONTH_YEAR.format(c.getTime());
            } else {
                label = MONTH_ONLY.format(c.getTime());
            }
            xValues.add(label);
        }

        ArrayList<BarEntry> yValues = new ArrayList<>();
        for (int i = 0; i < projectionItems.length; i++) {
            float best = projectionItems[i].bestCase;
            float worst = projectionItems[i].worstCase;
            BarEntry entry;
            if (Math.abs(best - worst) < Math.abs(best) * 0.1) {
                entry = new BarEntry(new float[]{(best + worst) / 2}, i);
            } else {
                float[] val = getDisplayedValues(best, worst);
                entry = new BarEntry(new float[]{val[0], val[1]}, i);
            }
            entry.setData(projectionItems[i]);
            yValues.add(entry);
        }

        BarDataSet set = new BarDataSet(yValues, "Spending Projections");
        set.setColors(getColors());
        set.setStackLabels(new String[]{"Best case", "Worst case"});

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(set);

        BarData data = new BarData(xValues, dataSets);
        data.setValueFormatter(new MyValueFormatter());
        data.setValueTextSize(13f);

        chart.setData(data);
        chart.zoom(12, 5, 0, -3000);
        chart.invalidate();
    }

    private int[] getColors() {
        return new int[]{Color.rgb(0, 255, 0), Color.rgb(0, 255, 255)};
    }

    private class MyYAxisValueFormatter implements YAxisValueFormatter {

        private DecimalFormat mFormat;

        MyYAxisValueFormatter() {
            mFormat = new DecimalFormat("###,###,###,##0.00");
        }

        @Override
        public String getFormattedValue(float value, YAxis yAxis) {
            return mFormat.format(value);
        }
    }

    private class MyValueFormatter implements ValueFormatter {

        private DecimalFormat mFormat;

        MyValueFormatter() {
            mFormat = new DecimalFormat("###,###,###,##0.00");
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            ProjectionItem item = (ProjectionItem) entry.getData();
            float[] values = getDisplayedValues(item.bestCase, item.worstCase);
            float finalValue;

            if (value == values[0]) {
                finalValue = item.worstCase;
            } else if (value == values[1]) {
                finalValue = item.bestCase;
            } else {
                finalValue = value;
            }
            return mFormat.format(finalValue);
        }
    }

    /**
     * @return the first value is always the one adjacent to the axis
     */
    private static float[] getDisplayedValues(float best, float worst) {
        float val1;
        float val2;
        if (worst * best < 0) {
            // different sign, the order is not important
            val1 = worst;
            val2 = best;
        } else {
            // same sign
            if (best < 0) {
                val1 = worst - best; // far
                val2 = best; // adjacent
            } else {
                val1 = worst; // adjacent
                val2 = best - worst; // far
            }
        }
        return new float[]{val1, val2};
    }
}