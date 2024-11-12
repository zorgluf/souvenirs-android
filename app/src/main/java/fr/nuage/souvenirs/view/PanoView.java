package fr.nuage.souvenirs.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PanoView extends WebView {

    private File tempPanoFile;

    public PanoView(@NonNull Context context) {
        super(context);
    }

    public PanoView(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void setPanoUri(Uri panoUri) {
        //create temp dir to save copy of pano
        File tempDir = new File(getContext().getCacheDir(),"imagePano");
        if (! tempDir.exists()) {
            tempDir.mkdir();
        }
        if (panoUri != null) {
            try {
                tempPanoFile = File.createTempFile("panopic",null,tempDir);
                tempPanoFile.deleteOnExit();
                copyFile(getContext().getContentResolver().openInputStream(panoUri),tempPanoFile);
            } catch (IOException e) {
                Log.e(getClass().toString(),"Impossible to create temp file",e);
                return;
            }
        } else {
            return;
        }
        //prepare webview
        addJavascriptInterface(this,"injectedObject");
        WebSettings webSettings = getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(false);
        webSettings.setSupportZoom(false);
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(getContext()))
                .addPathHandler("/image/",new WebViewAssetLoader.InternalStoragePathHandler(getContext(),
                        tempDir))
                .build();
        addJavascriptInterface(new WebAppInterface(this), "Android");
        setWebViewClient(new LocalContentWebViewClient(assetLoader));
        loadUrl("https://appassets.androidplatform.net/assets/index.html");
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

    private class LocalContentWebViewClient extends WebViewClientCompat {

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

    public class WebAppInterface {

        PanoView panoView;

        WebAppInterface(PanoView panoView) {
            this.panoView = panoView;
        }

        @JavascriptInterface
        public void fullScreen() {
            new Handler(Looper.getMainLooper()).post(() -> panoView.performClick());

        }
    }
}
