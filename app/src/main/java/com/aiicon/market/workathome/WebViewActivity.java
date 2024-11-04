package com.aiicon.market.workathome;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
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
import android.provider.Settings;
import android.util.Log;
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

import com.raonsecure.appironlib.AppIronManager;
import com.raonsecure.appironlib.error.AppIronError;
import com.raonsecure.appironlib.exception.AppIronException;
import com.raonsecure.appironlib.listener.AppIronListener;
import com.aiicon.market.workathome.crypto.CryptoUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;

public class WebViewActivity extends AppCompatActivity {

    // region variables
    public BackgroundWebView webView;
    public ImageView imageView;
    private Uri cameraImageUri = null;
    private long backBtnTime = 0;
    private static int storedVersionCode;
    private static String storedVersionName;
    public ValueCallback<Uri[]> filePathCallbackLollipop;
    private String mServerAddress = "https://s.raonsecure.co.kr:9456";
    // endregion

    // region App Life Cycle
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
        //isAppUpdated(this);


        // 웹뷰 세팅
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDefaultTextEncodingName("UTF-8");
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        webSettings.setBuiltInZoomControls(true);

        // 뒤로가기
        this.getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {

            @Override
            public void handleOnBackPressed() {

                long curTime = System.currentTimeMillis();
                long gapTime = curTime - backBtnTime;

                // 현재는 팝업 열어 뒤로 가기 위한 예제로 ! 해제 해둠
//                if (webView.canGoBack() && !webView.getUrl().contains("/client/mn/main/main.do".toLowerCase())) {
                if (webView.canGoBack() && webView.getUrl().contains("kafb2b".toLowerCase())) {
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

        webView.setWebChromeClient(new WebChromeClient() {

            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {

                WebView newWebview = new WebView(WebViewActivity.this);
                final Dialog dialog = new Dialog(WebViewActivity.this);

                dialog.setContentView(newWebview);
                dialog.show();

                newWebview.setWebChromeClient(new WebChromeClient() {

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
            // For Android 5.0+
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {

                // Callback 초기화 (중요!)
                if (filePathCallbackLollipop != null) {
                    filePathCallbackLollipop.onReceiveValue(null);
                    filePathCallbackLollipop = null;
                }
                filePathCallbackLollipop = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");

                startActivityResult.launch(intent);
                return true;
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
                } else if (url.contains("kafb2b")) {
                    webView.canGoBack();
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

        // url load
//        webView.loadUrl("http://dpis.mnd.go.kr:8090");
//        webView.loadUrl("https://www.kafb2b.or.kr");

        webView.loadUrl("file:///android_asset/test/test.html");

        new AlertDialog.Builder(webView.getContext())
                .setTitle("알림")
                .setMessage("현재는 임시 테스트용으로 만들어졌으며\n추후 기능 변경이 있을 수 있습니다.")
                .setPositiveButton("확인", new AlertDialog.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setCancelable(false)
                .create()
                .show();

        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        // 앱 위변조 검증 요청
//        try {
//            new AppIronManager()
//                    .setActivity(WebViewActivity.this)
//                    .setCallbackListener(mAppIronListener)
//                    .setDomain(mServerAddress)
//                    .setUserValue("id: raon")
//                    .start();
//        } catch (AppIronException e) {
//            Toast.makeText(WebViewActivity.this, ""+e.getErrorMessage(), Toast.LENGTH_SHORT).show();
//        }
    }

    // region AppIron
    private AppIronListener mAppIronListener = new AppIronListener() {
        @Override
        public void onResult(byte[] bytes) {
            decrypt(bytes);
        }

        @Override
        public void onError(AppIronError appIronError) {
            Toast.makeText(
                            WebViewActivity.this,
                            "[TouchEnAppIron 에러] 에러코드 : " + appIronError.getCode() + ", 에러 메세지 : " + appIronError.getMessage(),
                            Toast.LENGTH_SHORT)
                    .show();
            Log.v("AppIRON", "[TouchEnAppIron 에러] 에러코드 : " + appIronError.getCode() + ", 에러 메세지 : " + appIronError.getMessage());
        }
    };

    private void decrypt(byte[] data) {
        try {
            String decryptData = new CryptoUtil(getApplicationContext()).decryptSeed(data);

            JSONObject jsonObject = new JSONObject(decryptData);
            String result1 = jsonObject.getString("result1");
            String result2 = jsonObject.getString("result2");
            String result3 = jsonObject.getString("result3");

            if(confirm(result1)) {
                // 앱 위변조 검증 실패
                Toast.makeText(WebViewActivity.this, "앱 위변조를 탐지하였습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if(confirm(result2)) {
                // OS 위변조 검증 실패
                Toast.makeText(WebViewActivity.this, "OS 위변조를 탐지하였습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if(confirm(result3)) {
                // 디버깅 검증 실패
                Toast.makeText(WebViewActivity.this, "디버깅을 탐지하였습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // success
            Toast.makeText(WebViewActivity.this, "검증에 성공하였습니다.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean confirm(String result) {
        return result.contentEquals("true") ? true : false;
    }
    // endregion

//    @Override
//    protected void onRestart() {
//        super.onRestart();
//        webView.reload();
//    }

        ActivityResultLauncher<Intent> startActivityResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            Intent data = new Intent(result.getData());
            Uri mResult = Uri.parse(data.toString());
            // 선택
            if (result.getResultCode() == RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    filePathCallbackLollipop.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), data));
                } else {
                    filePathCallbackLollipop.onReceiveValue(new Uri[]{mResult});
                }
                filePathCallbackLollipop = null;
            } else { // 선택 X
                if (filePathCallbackLollipop != null) {
                    filePathCallbackLollipop.onReceiveValue(null);
                    filePathCallbackLollipop = null;
                }
            }
        }
    });

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
            String versionName = packageInfo.versionName;

            storedVersionCode = versionCode;
            storedVersionName = versionName;

        }catch(Exception e){
        }
    }

    // 앱 업데이트 확인
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

        // 앱 업데이트
        @JavascriptInterface
        public void updateApp() {
            isAppUpdated(mContext);
        }

        // 앱버전 받기
        @JavascriptInterface
        public void setAppVersion() {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl("javascript:window.NativeInterface.setAppVersion('"+storedVersionName+"')");
                }
            });
        }

        // 디바이스 아이디
        @JavascriptInterface
        public void setDeviceId() {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                    JSONObject deviceIds = new JSONObject();
                    try {
                        deviceIds.put("deviceId", deviceId);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    webView.loadUrl("javascript:window.NativeInterface.setDeviceId('"+deviceIds+"')");
                }
            });
        }
    }
    // endregion
}