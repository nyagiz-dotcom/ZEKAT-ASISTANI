package com.example.zekatasistani;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import android.util.Base64;

public class MainActivity extends AppCompatActivity {
    private WebView myWebView;
    private RelativeLayout rootLayout;
    private View splashOverlay;
    private ValueCallback<Uri[]> uploadMessage;
    private final static int FILE_CHOOSER_RESULT_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { WindowManager.LayoutParams cutoutParams = getWindow().getAttributes(); cutoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES; getWindow().setAttributes(cutoutParams); }
        rootLayout = new RelativeLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        myWebView = new WebView(this);
        myWebView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        WebSettings ws = myWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setSupportZoom(false);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);
        ws.setMediaPlaybackRequiresUserGesture(false);
        myWebView.setWebViewClient(new WebViewClient());
        myWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                if (url != null && url.startsWith("blob:")) {
                    return;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "İndirme başlatılamadı", Toast.LENGTH_SHORT).show();
                }
            }
        });
        final ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        RelativeLayout.LayoutParams pbParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        pbParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        progressBar.setLayoutParams(pbParams);
        progressBar.setMax(100);
        myWebView.clearCache(true);
        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (false) {
                    if (newProgress == 100) progressBar.setVisibility(View.GONE);
                    else {
                        if (progressBar.getVisibility() == View.GONE) progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(newProgress);
                    }
                }
                super.onProgressChanged(view, newProgress);
            }
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                }
                uploadMessage = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE);
                } catch (Exception e) {
                    uploadMessage = null;
                    return false;
                }
                return true;
            }
        });
        myWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public boolean hasNotificationPermission() { return Build.VERSION.SDK_INT < 33 || checkSelfPermission("android.permission.POST_NOTIFICATIONS") == android.content.pm.PackageManager.PERMISSION_GRANTED; }
            @JavascriptInterface
            public void requestNotificationPermission() { runOnUiThread(() -> { if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermissions(new String[] { "android.permission.POST_NOTIFICATIONS" }, 2102); }); }
            @JavascriptInterface
            public void showNotification(String title, String message) {
                runOnUiThread(() -> {
                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[] { "android.permission.POST_NOTIFICATIONS" }, 2102); return; }
                    String channelId = "user_notifications";
                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(new NotificationChannel(channelId, "Uygulama bildirimleri", NotificationManager.IMPORTANCE_DEFAULT));
                    Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(MainActivity.this, channelId) : new Notification.Builder(MainActivity.this);
                    builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title == null ? getString(R.string.app_name) : title).setContentText(message == null ? "" : message);
                    manager.notify((int)(System.currentTimeMillis() & 0x7fffffff), builder.build());
                });
            }
            @JavascriptInterface
            public boolean hasNotificationPolicyAccess() { return Build.VERSION.SDK_INT < 23 || ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).isNotificationPolicyAccessGranted(); }
            @JavascriptInterface
            public void requestNotificationPolicyAccess() { if (Build.VERSION.SDK_INT >= 23 && !((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).isNotificationPolicyAccessGranted()) { try { startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception e) { e.printStackTrace(); } } }
            @JavascriptInterface
            public void scheduleSilentMode(String windowKey, long startMillis, long endMillis) { if (startMillis <= 0 || endMillis <= 0) { cancelSilentAlarm(windowKey, true); cancelSilentAlarm(windowKey, false); scheduleSilentAlarm(windowKey, false, System.currentTimeMillis()); return; } if (Build.VERSION.SDK_INT >= 23 && !((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).isNotificationPolicyAccessGranted()) { requestNotificationPolicyAccess(); return; } scheduleSilentAlarm(windowKey, true, startMillis); scheduleSilentAlarm(windowKey, false, endMillis); }
            private void cancelSilentAlarm(String windowKey, boolean start) { Intent intent = new Intent(MainActivity.this, SilentModeReceiver.class); intent.setAction(start ? "start" : "end"); intent.putExtra("windowKey", windowKey); int requestId = 700000 + Math.abs(windowKey.hashCode() % 100000) * 2 + (start ? 0 : 1); PendingIntent pending = PendingIntent.getBroadcast(MainActivity.this, requestId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); ((AlarmManager) getSystemService(ALARM_SERVICE)).cancel(pending); pending.cancel(); }
            private void scheduleSilentAlarm(String windowKey, boolean start, long whenMillis) { Intent intent = new Intent(MainActivity.this, SilentModeReceiver.class); intent.setAction(start ? "start" : "end"); intent.putExtra("windowKey", windowKey); int requestId = 700000 + Math.abs(windowKey.hashCode() % 100000) * 2 + (start ? 0 : 1); PendingIntent pending = PendingIntent.getBroadcast(MainActivity.this, requestId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); AlarmManager alarms = (AlarmManager) getSystemService(ALARM_SERVICE); long trigger = Math.max(whenMillis, System.currentTimeMillis()); if (Build.VERSION.SDK_INT >= 31 && alarms.canScheduleExactAlarms()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending); else if (Build.VERSION.SDK_INT >= 23) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending); else alarms.set(AlarmManager.RTC_WAKEUP, trigger, pending); }
            @JavascriptInterface
            public void cancelScheduledNotifications() { android.content.SharedPreferences prefs = getSharedPreferences("scheduled_prayer_alarms", MODE_PRIVATE); java.util.Set<String> ids = prefs.getStringSet("ids", java.util.Collections.emptySet()); AlarmManager alarms = (AlarmManager) getSystemService(ALARM_SERVICE); for (String raw : ids) { try { int id = Integer.parseInt(raw); Intent alarmIntent = new Intent(MainActivity.this, NotificationReceiver.class); PendingIntent pending = PendingIntent.getBroadcast(MainActivity.this, id, alarmIntent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE); if (pending != null) { alarms.cancel(pending); pending.cancel(); } } catch (Exception ignored) {} } prefs.edit().clear().apply(); }
            @JavascriptInterface
            public void scheduleNotification(String title, String message, long whenMillis) {
                Intent intent = new Intent(MainActivity.this, NotificationReceiver.class); intent.putExtra("title", title); intent.putExtra("message", message);
                int requestId = (title + "|" + whenMillis).hashCode() & 0x7fffffff; PendingIntent pending = PendingIntent.getBroadcast(MainActivity.this, requestId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); android.content.SharedPreferences prefs = getSharedPreferences("scheduled_prayer_alarms", MODE_PRIVATE); java.util.Set<String> ids = new java.util.HashSet<>(prefs.getStringSet("ids", java.util.Collections.emptySet())); ids.add(String.valueOf(requestId)); prefs.edit().putStringSet("ids", ids).apply();
                AlarmManager alarms = (AlarmManager) getSystemService(ALARM_SERVICE); long trigger = Math.max(whenMillis, System.currentTimeMillis());
                if (Build.VERSION.SDK_INT >= 31 && alarms.canScheduleExactAlarms()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending); else if (Build.VERSION.SDK_INT >= 23) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending); else alarms.set(AlarmManager.RTC_WAKEUP, trigger, pending);
            }
            @JavascriptInterface
            public void updatePrayerStatus(String title, String message) {
                String statusTitle = title == null ? getString(R.string.app_name) : title;
                String statusText = message == null ? "" : message;
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[] { "android.permission.POST_NOTIFICATIONS" }, 2102); return; }
                String statusChannel = "prayer_status"; NotificationManager statusManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= 26) { NotificationChannel channel = new NotificationChannel(statusChannel, "Namaz vakitleri", NotificationManager.IMPORTANCE_LOW); channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); statusManager.createNotificationChannel(channel); }
                Notification.Builder statusBuilder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(MainActivity.this, statusChannel) : new Notification.Builder(MainActivity.this);
                statusBuilder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(statusTitle).setContentText(statusText).setStyle(new Notification.BigTextStyle().bigText(statusText)).setOngoing(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PUBLIC).setCategory(Notification.CATEGORY_REMINDER);
                statusManager.notify(1701, statusBuilder.build());
            }
            @JavascriptInterface
            public void saveFile(String fileName, String mimeType, String base64Data) {
                Uri createdUri = null;
                try {
                    String safeName = fileName == null ? "indirilen_dosya" : fileName.replaceAll("[^A-Za-z0-9._-]", "_");
                    if (Build.VERSION.SDK_INT < 29) {
                        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        if (!downloads.exists() && !downloads.mkdirs()) throw new Exception("İndirilenler klasörü oluşturulamadı");
                        File target = new File(downloads, safeName);
                        try (FileOutputStream legacy = new FileOutputStream(target)) { legacy.write(Base64.decode(base64Data, Base64.DEFAULT)); }
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Dosya İndirilenler klasörüne kaydedildi ✅", Toast.LENGTH_LONG).show()); return;
                    }
                    ContentResolver resolver = getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, safeName);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType == null || mimeType.isEmpty() ? "application/octet-stream" : mimeType);
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    createdUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (createdUri == null) throw new Exception("Dosya kaydı başlatılamadı");
                    byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
                    try (OutputStream os = resolver.openOutputStream(createdUri)) {
                        if (os == null) throw new Exception("Dosya yazma akışı açılamadı");
                        os.write(bytes);
                    }
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Dosya İndirilenler klasörüne kaydedildi ✅", Toast.LENGTH_LONG).show());
                } catch (Exception e) {
                    if (createdUri != null) getContentResolver().delete(createdUri, null, null);
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Kayıt hatası: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }
        }, "AndroidBridge");
        myWebView.loadUrl("file:///android_asset/index.html");
        rootLayout.addView(myWebView);
        rootLayout.addView(progressBar);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermissions(new String[] { "android.permission.POST_NOTIFICATIONS" }, 2102);
        if (Build.VERSION.SDK_INT < 29 && checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermissions(new String[] { "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE" }, 2103);
        setContentView(rootLayout);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (getWindow().getInsetsController() != null) {
                android.view.WindowInsetsController controller = getWindow().getInsetsController();
                controller.hide(android.view.WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (uploadMessage == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK && intent != null) {
                if (intent.getClipData() != null) {
                    int count = intent.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) results[i] = intent.getClipData().getItemAt(i).getUri();
                } else if (intent.getData() != null) {
                    results = new Uri[] { intent.getData() };
                }
            }
            uploadMessage.onReceiveValue(results);
            uploadMessage = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}