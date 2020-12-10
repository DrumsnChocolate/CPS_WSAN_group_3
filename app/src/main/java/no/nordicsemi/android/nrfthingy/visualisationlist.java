package no.nordicsemi.android.nrfthingy;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class visualisationlist extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visualisationlist);
        ListView bullshit = findViewById(R.id.listvis);
        ArrayList<String> list = new ArrayList<>();

        ArrayAdapter listarray = new ArrayAdapter(this, android.R.layout.simple_list_item_1,list);
        list.add("scream");//addhere reference to event
        bullshit.setAdapter(listarray);

        list.add("scream");
        list.add("not a scream");
        list.add("scream");


}}
