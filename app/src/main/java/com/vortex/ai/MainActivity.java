package com.vortex.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.ArrayList;
import java.util.Locale;
import android.speech.tts.TextToSpeech;

public class MainActivity extends Activity {
    WebView web;
    SpeechRecognizer sr;
    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        web.setBackgroundColor(0xFF090D18);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() { public void onInit(int s) { if (s == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("tr", "TR")); } });
        web.loadUrl("file:///android_asset/index.html");
    }

    class Bridge {
        @JavascriptInterface
        public void speak(final String t) { runOnUiThread(new Runnable() { public void run() { if (tts != null) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "v"); } }); }
        @JavascriptInterface
        public void listen() {
            runOnUiThread(new Runnable() {
                public void run() { startListen(); }
            });
        }
    }

    void js(String s) { web.evaluateJavascript(s, null); }

    String esc(String t) {
        return t.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
    }

    void startListen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            js("onVoiceErr('Ses tanima servisi bulunamadi')");
            return;
        }
        if (sr == null) {
            sr = SpeechRecognizer.createSpeechRecognizer(this);
            sr.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle b) {}
                public void onBeginningOfSpeech() {}
                public void onRmsChanged(float v) {}
                public void onBufferReceived(byte[] b) {}
                public void onEndOfSpeech() {}
                public void onEvent(int i, Bundle b) {}
                public void onError(int e) { js("onVoiceErr('Anlasilamadi, tekrar dene')"); }
                public void onResults(Bundle b) {
                    ArrayList<String> l = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (l != null && !l.isEmpty()) js("onVoice('" + esc(l.get(0)) + "',true)");
                    else js("onVoiceErr('Anlasilamadi, tekrar dene')");
                }
                public void onPartialResults(Bundle b) {
                    ArrayList<String> l = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (l != null && !l.isEmpty()) js("onVoice('" + esc(l.get(0)) + "',false)");
                }
            });
        }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        sr.startListening(i);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        super.onRequestPermissionsResult(code, p, r);
        if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) startListen();
        else js("onVoiceErr('Mikrofon izni verilmedi')");
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (sr != null) sr.destroy();
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}
