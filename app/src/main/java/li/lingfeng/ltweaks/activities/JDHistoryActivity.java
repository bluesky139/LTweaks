package li.lingfeng.ltweaks.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.utils.ShoppingUtils;

// This name should be PriceHistoryActivity, due to compatible with old version, keep it for now.
public class JDHistoryActivity extends AppCompatActivity implements
        OnChartGestureListener, OnChartValueSelectedListener {

    private ProgressBar mProgressBar;
    private LineChart mChart;
    private PriceHistoryGrabber.Result mData;
    private DecimalFormat mDec = new DecimalFormat("#,###.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getIntent().getAction().equals(Intent.ACTION_SEND) || !getIntent().getType().equals("text/plain")) {
            Toast.makeText(this, R.string.not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        Logger.i("Got share text: " + text);
        Pair<String, Integer> item = ShoppingUtils.findItemId(text);
        if (item == null) {
            Toast.makeText(this, R.string.not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String itemId = item.first;
        @ShoppingUtils.Store int store = item.second;

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_jd_history);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mChart = (LineChart) findViewById(R.id.chart1);
        PriceHistoryGrabber grabber = new PriceHistoryGrabber(store, itemId, new PriceHistoryGrabber.GrabCallback() {
            @Override
            public void onResult(final PriceHistoryGrabber.Result result) {
                Logger.i("Prices result " + result);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result == null) {
                            Toast.makeText(JDHistoryActivity.this, R.string.jd_history_can_not_get_prices, Toast.LENGTH_SHORT).show();
                            JDHistoryActivity.this.finish();
                            return;
                        }
                        mData = result;
                        createChart(result);
                        mProgressBar.setVisibility(View.INVISIBLE);
                        mChart.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        grabber.startRequest();
    }

    private void createChart(final PriceHistoryGrabber.Result data) {
        mChart.setOnChartGestureListener(this);
        mChart.setOnChartValueSelectedListener(this);
        mChart.setDrawGridBackground(false);

        // no description text
        mChart.getDescription().setEnabled(false);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(false);
        // mChart.setScaleXEnabled(true);
        // mChart.setScaleYEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);

        // set an alternative background color
        // mChart.setBackgroundColor(Color.GRAY);

        // create a custom MarkerView (extend MarkerView) and specify the layout
        // to use for it
        MyMarkerView mv = new MyMarkerView(this, R.layout.jd_history_marker_view);
        mv.setChartView(mChart); // For bounds control
        mChart.setMarker(mv); // Set the marker to the chart

        // x-axis limit line
        /*LimitLine llXAxis = new LimitLine(10f, "Index 10");
        llXAxis.setLineWidth(4f);
        llXAxis.enableDashedLine(10f, 10f, 0f);
        llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        llXAxis.setTextSize(18f);*/

        mChart.setExtraTopOffset(10f);
        XAxis xAxis = mChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum((data.endTime - data.startTime) / 24f / 3600f);
        xAxis.setLabelCount(3);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                value *= 24 * 3600;
                value += data.startTime;
                //Timestamp timestamp = new Timestamp();
                Date date = new Date(((long) value) * 1000);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                //Log.d("test", "getFormattedValue " + ((long) value) + ", " + dateFormat.format(date));
                return dateFormat.format(date);
            }
        });
        xAxis.setTextSize(12f);
        //xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //xAxis.addLimitLine(llXAxis); // add x-axis limit line


        LimitLine ll1 = new LimitLine(data.maxPrice, getString(R.string.jd_history_highest) + ": " + mDec.format(data.maxPrice));
        ll1.setLineWidth(2f);
        ll1.enableDashedLine(10f, 10f, 0f);
        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll1.setTextSize(14f);
        ll1.setTextColor(Color.RED);

        LimitLine ll2 = new LimitLine(data.minPrice, getString(R.string.jd_history_lowest) + ": " + mDec.format(data.minPrice));
        ll2.setLineWidth(2f);
        ll2.enableDashedLine(10f, 10f, 0f);
        ll2.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        ll2.setTextSize(14f);
        ll2.setTextColor(Color.RED);

        float axisHead = (data.maxPrice - data.minPrice) / 5f;
        axisHead = axisHead == 0f ? 20f : axisHead;
        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
        leftAxis.addLimitLine(ll1);
        leftAxis.addLimitLine(ll2);
        leftAxis.setAxisMaximum(data.maxPrice + axisHead);
        leftAxis.setAxisMinimum(data.minPrice - axisHead);
        //leftAxis.setYOffset(20f);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setTextSize(12f);

        // limit lines are drawn behind data (and not on top)
        leftAxis.setDrawLimitLinesBehindData(true);

        mChart.getAxisRight().setEnabled(false);

        //mChart.getViewPortHandler().setMaximumScaleY(2f);
        //mChart.getViewPortHandler().setMaximumScaleX(2f);

        // add data
        setData(data);

//        mChart.setVisibleXRange(20);
//        mChart.setVisibleYRange(20f, AxisDependency.LEFT);
//        mChart.centerViewTo(20, 50, AxisDependency.LEFT);

        mChart.animateX(1000);
        //mChart.invalidate();

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextSize(12f);
        //l.setPosition(Legend.LegendPosition.ABOVE_CHART_LEFT);

        // // dont forget to refresh the drawing
        // mChart.invalidate();
    }

    private void setData(PriceHistoryGrabber.Result data) {

        ArrayList<Entry> values = new ArrayList<Entry>();
        for (int i = 0; i < data.prices.size(); i++) {

            float val = data.prices.get(i);
            values.add(new Entry(i, val));
        }

        LineDataSet set1;
        // create a dataset and give it a type
        set1 = new LineDataSet(values, getString(R.string.app_name));

        // set the line to be drawn like this "- - - - - -"
        //set1.enableDashedLine(10f, 5f, 0f);
        //set1.enableDashedHighlightLine(10f, 5f, 0f);
        set1.setColor(getResources().getColor(R.color.jd_history_main_line));
        set1.setCircleColor(Color.BLACK);
        set1.setLineWidth(2f);
        set1.setCircleRadius(3f);
        set1.setDrawCircleHole(false);
        set1.setValueTextSize(14f);
        set1.setDrawFilled(false);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.STEPPED);
        set1.setFormLineWidth(1f);
        //set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
        set1.setFormSize(15.f);

        if (Utils.getSDKInt() >= 18) {
            // fill drawable only supported on api level 18 and above
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.jd_history_fade_red);
            set1.setFillDrawable(drawable);
        }
        else {
            set1.setFillColor(Color.BLACK);
        }

        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(set1); // add the datasets

        // create a data object with the datasets
        // set data
        mChart.setData(new LineData(dataSets));
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        //Log.i("Gesture", "START, x: " + me.getX() + ", y: " + me.getY());
    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        //Log.i("Gesture", "END, lastGesture: " + lastPerformedGesture);

        // un-highlight values after the gesture is finished and no single-tap
        if(lastPerformedGesture != ChartTouchListener.ChartGesture.SINGLE_TAP)
            mChart.highlightValues(null); // or highlightTouch(null) for callback to onNothingSelected(...)
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {

    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }

    public class MyMarkerView extends MarkerView {

        private TextView tvContent;

        public MyMarkerView(Context context, int layoutResource) {
            super(context, layoutResource);

            tvContent = (TextView) findViewById(R.id.tvContent);
        }

        // callbacks everytime the MarkerView is redrawn, can be used to update the
        // content (user-interface)
        @Override
        public void refreshContent(Entry e, Highlight highlight) {

            float x = e.getX();
            x *= 24 * 3600;
            x += mData.startTime;
            Date date = new Date(((long) x) * 1000);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            //Log.d("test", "getFormattedValue " + ((long) x) + ", " + dateFormat.format(date));
            tvContent.setText(mDec.format(e.getY()) + "\n" + dateFormat.format(date));

            super.refreshContent(e, highlight);
        }

        @Override
        public MPPointF getOffset() {
            //return new MPPointF(0f, 0f);
            return new MPPointF(-(getWidth() / 2), -getHeight());
        }

        /*@Override
        public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
            return new MPPointF(0f, 0f);
        }*/

        @Override
        public void draw(Canvas canvas, float posX, float posY) {
            super.draw(canvas, posX, 200f);
        }
    }
}
