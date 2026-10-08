package fr.nuage.souvenirs.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PanoView extends WebView {

    private static final String TEMP_DIR = "imagePano";

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
        File tempDir = getTempDir(getContext());
        if (! tempDir.exists()) {
            tempDir.mkdir();
        }
        if (panoUri != null) {
            deleteTempPanoFile();
            try {
                tempPanoFile = File.createTempFile("panopic",null,tempDir);
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
        setWebChromeClient(new ConsoleLogWebChromeClient());
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

    @Override
    public void destroy() {
        //webview must be removed from view hierarchy before being destroyed
        if (getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).removeView(this);
        }
        deleteTempPanoFile();
        super.destroy();
    }

    private static File getTempDir(Context context) {
        return new File(context.getCacheDir(), TEMP_DIR);
    }

    //delete pano copies left by previous runs, must be called before any PanoView is created
    public static void clearTempPanoFiles(Context context) {
        File[] files = getTempDir(context).listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    private void deleteTempPanoFile() {
        if (tempPanoFile != null) {
            tempPanoFile.delete();
            tempPanoFile = null;
        }
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

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            //renderer is shared by all webviews of the app : drop this one instead of letting the app be killed
            Log.e(PanoView.class.getName(), "WebView renderer gone, crash: " + detail.didCrash() + ", priority at exit: " + detail.rendererPriorityAtExit());
            view.destroy();
            return true;
        }

    }

    private static class ConsoleLogWebChromeClient extends WebChromeClient {

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            //forward photo sphere viewer errors to logcat
            String message = consoleMessage.message() + " (" + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + ")";
            switch (consoleMessage.messageLevel()) {
                case ERROR:
                    Log.e(PanoView.class.getName(), message);
                    break;
                case WARNING:
                    Log.w(PanoView.class.getName(), message);
                    break;
                default:
                    Log.d(PanoView.class.getName(), message);
            }
            return true;
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
