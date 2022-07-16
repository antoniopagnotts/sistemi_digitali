package com.example.prova_app;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;

import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import butterknife.OnClick;

public class SpeechToTextActivity extends AppCompatActivity {

    //speech to text
    private ImageView iv_mic;
    private TextView tv_Speech_to_text;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1;

    //text to speech
    TextToSpeech tts;
    String trovato;
    StringBuilder text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_to_text);
        iv_mic = findViewById(R.id.iv_mic);
        tv_Speech_to_text = findViewById(R.id.tv_speech_to_text);

        iv_mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent
                        = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                        Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text");

                try {
                    startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
                }
                catch (Exception e) {
                    Toast
                            .makeText(SpeechToTextActivity.this, " " + e.getMessage(),
                                    Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                tv_Speech_to_text.setText(
                        Objects.requireNonNull(result).get(0));

                //text to speech
                switch(result.get(0).toString().toLowerCase()){
                    case "citta d'oro":
                    case "enoteca italiana":
                    case "forno brisa":
                    case "la forchetta":
                    case "la pizza da zero":
                    case "nuovo caffè del porto":
                    case "pokè rainbow caffè":
                    case "trattoria belfiore":
                        trovato = result.get(0).toString();
                        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                if(status==TextToSpeech.SUCCESS){
                                    tts.setLanguage(Locale.getDefault());
                                    tts.setSpeechRate(1.0f);
                                    tts.speak(result.get(0).toString()+", trovato!\n" + text.toString(),TextToSpeech.QUEUE_ADD,null);
                                    tts.speak(text.toString(),TextToSpeech.QUEUE_ADD,null);
                                }
                            }
                        });
                        break;
                    default:
                        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                if(status==TextToSpeech.SUCCESS){
                                    tts.setLanguage(Locale.getDefault());
                                    tts.setSpeechRate(1.0f);
                                    tts.speak("Locale non riconosciuto, riprova!",TextToSpeech.QUEUE_ADD,null);
                                }
                            }
                        });
                        break;
                }
            }
        }
    }

    @OnClick({R.id.openMenu})
    void openMenu(){
        text = new StringBuilder();
        BufferedReader reader = null;
        Context context = this.getApplicationContext();
        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open(trovato+".txt");

            reader = new BufferedReader(
                    new InputStreamReader(is));

            // do reading, usually loop until end of file reading
            String mLine = null;
            while ((mLine = reader.readLine()) != null) {
                text.append(mLine);
                text.append('\n');
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),"Error reading file!",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
            trovato = "";
        }
    }
}