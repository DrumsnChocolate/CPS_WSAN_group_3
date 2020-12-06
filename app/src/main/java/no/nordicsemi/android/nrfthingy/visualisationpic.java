package no.nordicsemi.android.nrfthingy;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class visualisationpic extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visualisationpicture);
        TextView soundwoord;
        TextView timewoord;
        TextView thingywoord;
        TextView aplitudewoord;
        ImageView soundpicture;
        soundpicture = (ImageView) findViewById(R.id.imageView);
        soundwoord = (TextView) findViewById(R.id.textView);
        thingywoord = findViewById(R.id.thingy);
        aplitudewoord = findViewById(R.id.amplitude);
        soundpicture = (ImageView) findViewById(R.id.imageView);
        soundwoord = (TextView) findViewById(R.id.textView);
        thingywoord = findViewById(R.id.thingy);
        aplitudewoord = findViewById(R.id.amplitude);
        timewoord = findViewById(R.id.time);
    }}
