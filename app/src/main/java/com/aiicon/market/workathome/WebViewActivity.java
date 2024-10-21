package com.aiicon.market.workathome;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;

public class WebViewActivity extends AppCompatActivity {

    // region variables
    public BackgroundWebView webView;
    public ImageView imageView;
    public final static int FILECHOOSER_NORMAL_REQ_CODE = 2001;
    public final static int FILECHOOSER_LOLLIPOP_REQ_CODE = 2002;
    public ValueCallback<Uri> filePathCallbackNormal;
    private Uri cameraImageUri = null;
    private long backBtnTime = 0;
    private static int storedVersionCode;
    private static String storedVersionName;
    // endregion

    // region App Life Cycle
    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        webView = findViewById(R.id.webview);
        imageView = findViewById(R.id.loadingImg);

        // 캡쳐방지
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        // 캡쳐방지 해제
//        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);

        // 권한확인
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        checkPermission(permissions);

        // 버전 체크 및 업데이트
        saveVersionNameAndCode(this);
        isAppUpdated(this);


        // 웹뷰 세팅
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDefaultTextEncodingName("UTF-8");
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // 뒤로가기
        this.getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {

            @Override
            public void handleOnBackPressed() {

                long curTime = System.currentTimeMillis();
                long gapTime = curTime - backBtnTime;

                if (webView.canGoBack() && !webView.getUrl().contains("/client/mn/main/main.do".toLowerCase())) {
                    webView.goBack();
                } else if (0 <= gapTime && 2000 >= gapTime) {
                    finish();
                } else {
                    backBtnTime = curTime;
                    Toast.makeText(WebViewActivity.this, "'뒤로' 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 다운로드
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String s, String s1, String s2, String s3, long l) {

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(s));

                try {
                    s2 = URLDecoder.decode(s2, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                request.setMimeType(s3);

                String cookies = CookieManager.getInstance().getCookie(s);
                request.addRequestHeader("cookie", cookies);

                request.addRequestHeader("User-Agent", s1);
                request.setDescription("Downloading file...");
                request.setTitle(URLUtil.guessFileName(s, s2, s3));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(s, s2, s3));
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), "파일을 다운로드합니다.", Toast.LENGTH_LONG).show();
            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                String url = request.getUrl().toString();
                if (request.getUrl().getScheme().equals("intent")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

                        String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                        if (fallbackUrl != null) {
                            webView.loadUrl(fallbackUrl);
                            return true;
                        }
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    webView.loadUrl(url);
                }

                return super.shouldOverrideUrlLoading(view, request);
            }

            // 네트워크(WI-FI, 모바일데이터) 상태에 이상이 생길 시 호출되는 콜백함수
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);

                if (error.getDescription().equals("net::ERR_INTERNET_DISCONNECTED")) {
                    webView.removeView(view);
                    Intent intent = new Intent(getApplicationContext(), NetworkNotConnectionActivity.class);
                    startActivity(intent);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {

                WebView newWebview = new WebView(WebViewActivity.this);
                final Dialog dialog = new Dialog(WebViewActivity.this);

                dialog.setContentView(newWebview);
                dialog.show();

                newWebview.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView window) {
                        dialog.dismiss();
                    }
                });

                ((WebView.WebViewTransport)resultMsg.obj).setWebView(newWebview);
                resultMsg.sendToTarget();

                return true;
            }

            // Alert
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(view.getContext())
                        .setTitle("알림")
                        .setMessage(message)
                        .setPositiveButton("예", new AlertDialog.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
                return true;
            }

            // Confirm
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(view.getContext())
                        .setTitle("확인")
                        .setMessage(message)
                        .setPositiveButton("예", new AlertDialog.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setNegativeButton("아니오", new AlertDialog.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
                return true;
            }

            // Prompt
            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                return super.onJsPrompt(view, url, message, defaultValue, result);
            }

            // Console log
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("console.log", consoleMessage.message() + " -- From line " + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }

            // 파일 선택
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                boolean isCapture = fileChooserParams.isCaptureEnabled();
                runCamera(isCapture);
                return true;
            }
        });

        // url load
        webView.loadUrl("http://dpis.mnd.go.kr:8090");
//        webView.loadUrl("https://www.kafb2b.or.kr");
//        webView.loadUrl("file:///android_asset/test/test.html");

        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        webView.reload();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        switch (requestCode) {
            case FILECHOOSER_NORMAL_REQ_CODE:
                if (resultCode == RESULT_OK) {
                    if (filePathCallbackNormal == null) return;
                    Uri result = (data == null || resultCode != RESULT_OK) ? null : data.getData();
                    filePathCallbackNormal.onReceiveValue(result);
                    filePathCallbackNormal = null;
                }
                break;
            default:
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {
            if (grantResults.length > 0) {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED && !(permissions[i].equals("android.permission.WRITE_EXTERNAL_STORAGE")
                            || permissions[i].equals("android.permission.READ_EXTERNAL_STORAGE")))  {

                    }
                }
            }
        }
    }
    // endregion

    // region methods
    // 권한체크
    private void checkPermission(String[] permissions) {

        ArrayList<String> targetList = new ArrayList<>();

        for (int i = 0; i < permissions.length; i++) {

            String curPermission = permissions[i];
            int permissionCheck = ContextCompat.checkSelfPermission(this, curPermission);

            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {

            } else {
                targetList.add(curPermission);
            }
        }

        String[] targets = new String[targetList.size()];
        targetList.toArray(targets);

        if (targets.length > 0) {
            requestPermissions(targets, 101);
        }
    }

    // 버전 넘버 추출
    public static void saveVersionNameAndCode(Context context){
        try{
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            int versionCode = packageInfo.versionCode;
            String versionName=packageInfo.versionName;

            storedVersionCode = versionCode;
            storedVersionName = versionName;

        }catch(Exception e){
        }
    }

    public boolean isAppUpdated(Context context){
        boolean result=false;
        try{
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            int versionCode = packageInfo.versionCode;
            String versionName=packageInfo.versionName;

            String prevVersionName= storedVersionName;
            if(prevVersionName!=null && !prevVersionName.equals("") &&
                    !prevVersionName.equals(versionName)){
//                showLog("App Updated");
                new AlertDialog.Builder(this)
                        .setTitle("업데이트")
                        .setMessage("새로운 버전의 앱이 있습니다. 업데이트 하시겠습니까?")
                        .setPositiveButton("업데이트", new AlertDialog.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which) {
                                final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                } catch (android.content.ActivityNotFoundException anfe) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                }
                            }
                        })
                        .setNegativeButton("아니오", new AlertDialog.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
                result=true;
            }else{
//                showLog("App Not updated");
                new AlertDialog.Builder(this)
                        .setTitle("업데이트")
                        .setMessage("새로운 버전의 앱이 있습니다. 업데이트 하시겠습니까?")
                        .setPositiveButton("업데이트", new AlertDialog.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which) {
                                final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                } catch (android.content.ActivityNotFoundException anfe) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                }
                            }
                        })
                        .setNegativeButton("아니오", new AlertDialog.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            }

        }catch(Exception e){
        }
        return result;
    }

    // 사진 촬영 및 사진 선택
    private void runCamera(boolean _isCapture) {

        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File path = getFilesDir();
        File file = new File(path, "image.png");

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cameraImageUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", file);
        } else {
            cameraImageUri = Uri.fromFile(file);
        }
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

        if (!_isCapture) {
            Intent pickIntent = new Intent(Intent.ACTION_PICK);
            pickIntent.setType(MediaStore.Images.Media.CONTENT_TYPE);
            pickIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            String pickTitle = "사진을 가져올 방법을 선택하세요.";
            Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);

            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{intentCamera});
            startActivityForResult(chooserIntent, FILECHOOSER_LOLLIPOP_REQ_CODE);
        } else {
            startActivityForResult(intentCamera, FILECHOOSER_LOLLIPOP_REQ_CODE);
        }
    }
    // endregion

    // region Web App Interface
    public class WebAppInterface {

        Context mContext;
        WebAppInterface(Context c) {
            mContext = c;
        }

        // 앱 종료
        @JavascriptInterface
        public void forceQuitApp() {
            finish();
        }
    }
    // endregion
}