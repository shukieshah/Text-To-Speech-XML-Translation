package com.example.shuka.xmltranslates;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    EditText translateEditText;
    private Locale currentSpokenLang = Locale.US;

    //Create the Locale objects for languages not in Android Studio
    private Locale locSpanish = new Locale("es", "MX");
    private Locale locRussian = new Locale("ru", "RU");
    private Locale locPortuguese = new Locale("pt", "BR");
    private Locale locDutch = new Locale("nl", "NL");

    private Locale[] languages = {locDutch, Locale.FRENCH, Locale.GERMAN,
            Locale.ITALIAN, locPortuguese, locRussian, locSpanish};

    //Synthesizes text to speech
    private TextToSpeech textToSpeech;


    private Spinner languageSpinner;

    //Currently selected language in Spinner
    private int spinnerIndex = 0;

    //Will hold all the translations after you click "translate" button
    private String[] arrayOfTranslations;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        languageSpinner = (Spinner) findViewById(R.id.lang_spinner);

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int index, long l) {
                currentSpokenLang = languages[index];

                spinnerIndex = index;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        textToSpeech = new TextToSpeech(this, this);
    }

    //When the app closes, shutdown text to speech
    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        super.onDestroy();
    }

    // Calls for the AsyncTask to execute when the translate button is clicked
    public void onTranslateClick(View view) {

        EditText translateEditText = (EditText) findViewById(R.id.words_edit_text);

        // If the user entered words to translate then get the JSON data
        if (!isEmpty(translateEditText)) {

            Toast.makeText(this, "Getting Translations",
                    Toast.LENGTH_LONG).show();

            // Calls for the method doInBackground to execute
            new GetXMLData().execute();

        } else {

            // Post an error message if they didn't enter words
            Toast.makeText(this, "Enter Words to Translate",
                    Toast.LENGTH_SHORT).show();

        }

    }

    // Check if the user entered words to translate
    // Returns false if not empty
    protected boolean isEmpty(EditText editText) {

        // Get the text in the EditText convert it into a string, delete whitespace
        // and check length
        return editText.getText().toString().trim().length() == 0;

    }

    //Initializes text to speech capability
    @Override
    public void onInit(int status) {
        //Check if TextToSpeech is available
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(currentSpokenLang);

            if (result == textToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language Not Supported", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Text To Speech Failed", Toast.LENGTH_SHORT).show();
        }
    }

    //speaks the selected text using the correct voice for the language
    public void readTheText(View view) {
        //set the voice to use
        textToSpeech.setLanguage(currentSpokenLang);

        //check that translations are in the array
        if (arrayOfTranslations.length >= 9) {
            //There aren't voices for our first three languages so skip them
            // QUEUE_FLUSH deletes previous text to read and replaces it
            // with new text
            textToSpeech.speak(arrayOfTranslations[spinnerIndex + 4], TextToSpeech.QUEUE_FLUSH, null);
        } else {
            Toast.makeText(this, "Translate Text First", Toast.LENGTH_SHORT).show();
        }
    }


    class GetXMLData extends AsyncTask<Void, Void, Void> {

        String stringToPrint = "";

        @Override
        protected Void doInBackground(Void... voids) {

            String xmlString = "";

            String wordsToTranslate = "";

            EditText translateEditText = (EditText) findViewById(R.id.words_edit_text);

            wordsToTranslate = translateEditText.getText().toString();

            wordsToTranslate = wordsToTranslate.replace(" ", "+");

            DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());

            HttpPost httpPost = new HttpPost("http://newjustin.com/translateit.php?action=xmltranslations&english_words=" + wordsToTranslate);

            httpPost.setHeader("Content-type", "text/xml");

            InputStream inputStream = null;

            try {
                HttpResponse response = httpClient.execute(httpPost);

                HttpEntity entity = response.getEntity();

                inputStream = entity.getContent();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);

                StringBuilder sb = new StringBuilder();

                String line = null;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                xmlString = sb.toString();

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();

                factory.setNamespaceAware(true);

                XmlPullParser xpp = factory.newPullParser();

                xpp.setInput(new StringReader(xmlString));

                int eventType = xpp.getEventType();

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if ((eventType == XmlPullParser.START_TAG) && (!xpp.getName().equals("translations"))) {
                        stringToPrint = stringToPrint + xpp.getName() + " : ";
                    } else if (eventType == XmlPullParser.TEXT) {
                        stringToPrint = stringToPrint + xpp.getText() + "\n";
                    }

                    eventType = xpp.next();
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            TextView translateTextView = (TextView) findViewById(R.id.translate_text_view);

            //make the textview scrollable
            translateTextView.setMovementMethod(new ScrollingMovementMethod());

            //eliminate the "language : " part of the string for the translations
            String stringOfTranslations = stringToPrint.replaceAll("\\w+\\s:", "#");
            //store the translations ONLY into an array
            arrayOfTranslations = stringOfTranslations.split("#");
            translateTextView.setText(stringToPrint);
        }
    }

    //Converts speech to text
    public void ExceptSpeechInput(View view) {

        //Starts an Activity that will convert speech to text
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        //prompr the user to speak
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_input_phrase));

        try {
            startActivityForResult(intent, 100);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.stt_not_supported_message), Toast.LENGTH_LONG).show();
        }
    }

    //results of the speech recognizer are sent here
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {

        if ((requestCode == 100) && (data != null) && (resultCode == RESULT_OK)) {
            ArrayList<String> spokenText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            //populate the edit text with the spoken recognized data
            EditText wordsEntered = (EditText) findViewById(R.id.words_edit_text);
            wordsEntered.setText(spokenText.get(0));
        }
    }
}


