package ru.dmsh.victorina;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private static final int PICK_FILE = 10, SAVE_JSON = 11;
    private static final int PAPER = Color.rgb(246, 245, 239);
    private WebView web;
    private ProgressBar progress;
    private LinearLayout errorPanel;
    private ValueCallback<Uri[]> fileCallback;
    private String exportJson;
    private String lastPage = SitePolicy.HOME;

    // The website exports JSON with a Blob link. WebView does not download Blob URLs itself.
    // Intercept only JSON exports on our page and ask Android's document picker to save them.
    // No JavaScript interface with filesystem access is exposed to page scripts or frames.
    private static final String EXPORT_SCRIPT = "(function(){"
            + "if(window.__dmshExportInstalled)return;window.__dmshExportInstalled=true;"
            + "document.addEventListener('click',function(e){var a=e.target;"
            + "while(a&&a.tagName!=='A')a=a.parentElement;"
            + "if(!a||!a.download||!a.href.startsWith('blob:')||!a.download.endsWith('.json'))return;"
            + "e.preventDefault();e.stopImmediatePropagation();"
            + "fetch(a.href).then(function(r){return r.blob();}).then(function(b){"
            + "if(b.size>2097152)throw Error('Слишком большой файл JSON');"
            + "var reader=new FileReader();reader.onload=function(){"
            + "prompt('__DMSH_SAVE_JSON__',JSON.stringify({name:a.download,text:reader.result}));};"
            + "reader.readAsText(b);}).catch(function(){alert('Не удалось сохранить JSON. Повторите экспорт.');});"
            + "},true);})();";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(PAPER);
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets padding = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(padding.left, padding.top, padding.right, padding.bottom);
                return insets;
            });
        }
        web = new WebView(this);
        web.setBackgroundColor(PAPER);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        root.addView(progress, new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP));
        createErrorPanel(root);
        configureWebView();
        if (state != null) {
            exportJson = state.getString("exportJson");
            String saved = state.getString("lastPage");
            if (SitePolicy.isSite(saved)) lastPage = saved;
        }
        if (state == null || web.restoreState(state) == null || !SitePolicy.isSite(web.getUrl())) {
            web.loadUrl(lastPage);
        }
    }

    private int dp(int n) { return (int) (n * getResources().getDisplayMetrics().density + .5f); }
    private void createErrorPanel(FrameLayout root) {
        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setPadding(dp(28), dp(28), dp(28), dp(28));
        errorPanel.setBackgroundColor(PAPER);
        TextView title = new TextView(this);
        title.setText("Не удалось открыть викторину");
        title.setTextColor(Color.rgb(36, 59, 52));
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        TextView text = new TextView(this);
        text.setText("Проверьте подключение к интернету и попробуйте ещё раз. Приложение открывает сайт музыкальной викторины.");
        text.setTextSize(16);
        text.setGravity(Gravity.CENTER);
        text.setPadding(0, dp(16), 0, dp(20));
        Button retry = new Button(this);
        retry.setText("Повторить");
        retry.setOnClickListener(v -> { errorPanel.setVisibility(View.GONE); web.loadUrl(lastPage); });
        errorPanel.addView(title); errorPanel.addView(text); errorPanel.addView(retry);
        errorPanel.setVisibility(View.GONE);
        root.addView(errorPanel, new FrameLayout.LayoutParams(-1, -1));
    }

    private void configureWebView() {
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true); // Explicitly selected content:// documents from the picker.
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return true;
                String url = request.getUrl().toString();
                if (SitePolicy.isSite(url)) return false;
                openExternal(url);
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                progress.setVisibility(View.VISIBLE);
                if (SitePolicy.isSite(url)) lastPage = url;
            }
            @Override public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
                if (SitePolicy.isSite(url)) view.evaluateJavascript(EXPORT_SCRIPT, null);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showLoadError();
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode() >= 400) showLoadError();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) { progress.setProgress(value); }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (!SitePolicy.isSite(view.getUrl())) return false;
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                String[] types = params.getAcceptTypes();
                if (types.length > 0 && (types[0].contains("audio") || types[0].contains("mp3"))) intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "application/octet-stream"});
                if (types.length > 0 && types[0].contains("json")) intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain", "application/octet-stream"});
                try { startActivityForResult(intent, PICK_FILE); }
                catch (ActivityNotFoundException e) { fileCallback.onReceiveValue(null); fileCallback = null; toast("На устройстве нет приложения для выбора файлов."); }
                return true;
            }
            @Override public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(MainActivity.this).setMessage(message)
                        .setPositiveButton("ОК", (d, w) -> result.confirm())
                        .setOnCancelListener(d -> result.cancel()).show();
                return true;
            }
            @Override public boolean onJsConfirm(WebView view, String url, String message, JsResult result) { return confirm(message, result); }
            @Override public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
                return confirm("Покинуть страницу? Не забудьте экспортировать черновик викторины.", result);
            }
            @Override public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                result.cancel();
                if ("__DMSH_SAVE_JSON__".equals(message) && SitePolicy.isSite(url) && SitePolicy.isSite(view.getUrl())) saveExport(defaultValue);
                return true;
            }
        });
        web.setDownloadListener((url, agent, disposition, mime, length) -> {
            if (!url.startsWith("blob:")) openExternal(url);
            else toast("Для сохранения подборки нажмите «Экспорт JSON».");
        });
    }

    private boolean confirm(String message, JsResult result) {
        new AlertDialog.Builder(this).setMessage(message)
                .setPositiveButton("Да", (d, w) -> result.confirm())
                .setNegativeButton("Отмена", (d, w) -> result.cancel())
                .setOnCancelListener(d -> result.cancel()).show();
        return true;
    }
    private void showLoadError() { progress.setVisibility(View.GONE); errorPanel.setVisibility(View.VISIBLE); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private void openExternal(String url) {
        if (!SitePolicy.isExternalWebLink(url)) return;
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (ActivityNotFoundException e) { toast("Не удалось открыть ссылку во внешнем браузере."); }
    }
    private void saveExport(String payload) {
        try {
            if (payload == null || payload.length() > 2200000 || exportJson != null) throw new Exception();
            JSONObject data = new JSONObject(payload);
            String name = data.getString("name"), text = data.getString("text");
            if (!name.matches("[a-zA-Z0-9_-]+\\.json") || text.length() > 2097152) throw new Exception();
            JSONObject quiz = new JSONObject(text);
            if (quiz.getInt("schemaVersion") != 1 || quiz.getJSONArray("tracks").length() == 0) throw new Exception();
            exportJson = text;
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, name);
            startActivityForResult(intent, SAVE_JSON);
        } catch (Exception e) { exportJson = null; toast("Не удалось начать сохранение JSON. Повторите экспорт."); }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && fileCallback != null) {
            Uri uri = resultCode == RESULT_OK && data != null ? data.getData() : null;
            fileCallback.onReceiveValue(uri != null && "content".equals(uri.getScheme()) ? new Uri[]{uri} : null);
            fileCallback = null;
        }
        if (requestCode == SAVE_JSON) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && exportJson != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData(), "wt")) {
                    if (out == null) throw new Exception();
                    out.write(exportJson.getBytes(StandardCharsets.UTF_8));
                    toast("JSON сохранён. Загрузите его в папку quizzes/ на GitHub.");
                } catch (Exception e) { toast("Не удалось записать файл. Повторите экспорт."); }
            }
            exportJson = null;
        }
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        web.saveState(state);
        state.putString("lastPage", lastPage);
        if (exportJson != null && exportJson.length() < 200000) state.putString("exportJson", exportJson);
        super.onSaveInstanceState(state);
    }
    @Override public void onBackPressed() {
        if (errorPanel.getVisibility() == View.VISIBLE) { errorPanel.setVisibility(View.GONE); web.loadUrl(SitePolicy.HOME); }
        else if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
    @Override protected void onPause() {
        web.evaluateJavascript("document.querySelectorAll('audio').forEach(function(a){a.pause();});", null);
        web.onPause(); super.onPause();
    }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
    @Override protected void onDestroy() {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        web.stopLoading(); web.destroy(); super.onDestroy();
    }
}
