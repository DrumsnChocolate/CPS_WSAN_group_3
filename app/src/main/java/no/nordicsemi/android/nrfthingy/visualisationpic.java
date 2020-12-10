package no.nordicsemi.android.nrfthingy;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;

public class visualisationpic extends AppCompatActivity {
    BarChart barChart;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visualisationpicture);
        barChart = (BarChart) findViewById(R.id.bargraph);
        ArrayList<BarEntry>barEntries =new ArrayList<>();
        barEntries.add(new BarEntry(44f,0));
        barEntries.add(new BarEntry(88f,1));
        BarDataSet barDataSet = new BarDataSet(barEntries,"thingies");
        ArrayList<String> thethingies = new ArrayList<>();
        thethingies.add("thingy1");
        thethingies.add("thingy2");
        thethingies.add("thingy3");
        thethingies.add("thingy4");
        thethingies.add("thingy5");
        thethingies.add("thingy6");
        thethingies.add("thingy7");
        thethingies.add("thingy8");
        thethingies.add("thingy9");
        thethingies.add("thingy10");
        thethingies.add("thingy11");
        thethingies.add("thingy12");
        thethingies.add("thingy13");
        thethingies.add("thingy14");
        thethingies.add("thingy15");
        thethingies.add("thingy16");
        thethingies.add("thingy17");
        thethingies.add("thingy18");
        thethingies.add("thingy19");
        thethingies.add("thingy20");
        BarData theData = new BarData(thethingies,barDataSet);
        barChart.setData(theData);

    }}
