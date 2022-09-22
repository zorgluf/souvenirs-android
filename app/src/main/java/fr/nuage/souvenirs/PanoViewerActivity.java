package fr.nuage.souvenirs;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.view.PanoView;

public class PanoViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Uri panoUri = null;

        if (Intent.ACTION_SEND.equals(action) && type.equals(ImageElement.GOOGLE_PANORAMA_360_MIMETYPE)) {
            panoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }

        PanoView panoView = new PanoView(this);

        setContentView(panoView);

        panoView.setPanoUri(panoUri);

    }

}
