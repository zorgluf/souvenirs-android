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

public class PanoViewerActivity extends AppCompatActivity {

    private File tempPanoFile;

    @SuppressLint("SetJavaScriptEnabled")
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
        File tempDir = new File(getCacheDir(),"imagePano");
        if (! tempDir.exists()) {
            tempDir.mkdir();
        }
        if (panoUri != null) {
            try {
                tempPanoFile = File.createTempFile("panopic",null,tempDir);
                tempPanoFile.deleteOnExit();
                copyFile(getContentResolver().openInputStream(panoUri),tempPanoFile);
            } catch (IOException e) {
                Log.e(getClass().toString(),"Impossible to create temp file",e);
                return;
            }
        } else {
            return;
        }


        WebView myWebView = new WebView(this);
        myWebView.addJavascriptInterface(this,"injectedObject");
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(false);
        webSettings.setSupportZoom(false);
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .addPathHandler("/image/",new WebViewAssetLoader.InternalStoragePathHandler(this,
                        tempDir))
                .build();
        myWebView.setWebViewClient(new LocalContentWebViewClient(assetLoader));

        setContentView(myWebView);
        myWebView.loadUrl("https://appassets.androidplatform.net/assets/index.html");


    }

    private void copyFile(InputStream inputStream, File destFile) throws IOException {
        OutputStream output = new FileOutputStream(destFile);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        output.flush();
        output.close();
        inputStream.close();
    }

    @JavascriptInterface
    public String getPanoUrl() {
        return ("/image/" + tempPanoFile.getName());
    }

    private static class LocalContentWebViewClient extends WebViewClientCompat {

        private final WebViewAssetLoader mAssetLoader;

        LocalContentWebViewClient(WebViewAssetLoader assetLoader) {
            mAssetLoader = assetLoader;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                                          WebResourceRequest request) {
            return mAssetLoader.shouldInterceptRequest(request.getUrl());
        }

    }

}
