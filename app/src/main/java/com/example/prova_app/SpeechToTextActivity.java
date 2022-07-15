package com.example.prova_app;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class SpeechToTextActivity extends AppCompatActivity {

    //speech to text
    private ImageView iv_mic;
    private TextView tv_Speech_to_text;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1;

    //text to speech
    TextToSpeech tts;

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

                switch(result.get(0)){
                    case "Citta D'oro":
                    case "Enoteca Italiana":
                    case "Forno Brisa":
                    case "La Forchetta":
                    case "La Pizza Da Zero":
                    case "Nuovo Caffè del Porto":
                    case "Pokè Rainbow Caffè":
                    case "Trattoria Belfiore":
                        //leggi il menu
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
                }
            }
        }
    }
}