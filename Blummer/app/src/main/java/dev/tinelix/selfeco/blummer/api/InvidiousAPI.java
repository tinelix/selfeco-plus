package dev.tinelix.selfeco.blummer.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pixmob.httpclient.HttpClient;
import org.pixmob.httpclient.HttpRequestBuilder;
import org.pixmob.httpclient.HttpRequestHandler;
import org.pixmob.httpclient.HttpResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.tinelix.selfeco.blummer.api.entities.Video;
import dev.tinelix.selfeco.blummer.core.utilities.SSLDummyChecker;

public class InvidiousAPI {

    private String instance = "https://inv.tux.pizza/api/v1/";
    private final static String RELAY = "http://minvk.ru/apirelay.php";
    private final static String USER_AGENT = "Mozilla/4.0 (compatible; MSIE 4.01; Windows NT)";
    private final static String TAG = "InvidiousAPI";
    private final Handler handler;
    private final Context ctx;
    private HttpClient httpClient = null;

    public interface APICallback
    {
        void onSuccess(String obj);
        void onError(String reason);
    }

    public interface APIPreviewCallback
    {
        void onLoad(Bitmap scaledBitmap);
        void onError(String reason);
    }

    private ExecutorService imageLoaderThreadPool;

    public InvidiousAPI(Context ctx, String instance)
    {
        if(instance != null && instance.length() > 0) {
            this.instance = instance;
        }
        this.ctx = ctx;
        this.handler = new Handler(Looper.getMainLooper());
        imageLoaderThreadPool = Executors.newFixedThreadPool(3);
        httpClient = new HttpClient(ctx);
        httpClient.setConnectTimeout(30000);
        httpClient.setReadTimeout(30000);
        httpClient.setUserAgent(USER_AGENT);
        httpClient.setSSLStore(SSLDummyChecker.disableSSLCertificateChecking());
    }

    public void schedulePreviewDownload(final String url, final String cacheName, final APIPreviewCallback callback)
    {
        imageLoaderThreadPool.execute(new Runnable() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public void run() {
                try
                {
                    File cacheDir = new File(ctx.getApplicationContext().getCacheDir().getAbsolutePath() + "/preview/");
                    cacheDir.mkdir();
                    File cachedPreview = new File(cacheDir.getAbsolutePath() + cacheName);

                    Bitmap bmp = null;

                    long filesize = 0;

                    if(cachedPreview.exists())
                    {
                        bmp = BitmapFactory.decodeFile(cachedPreview.getAbsolutePath());
                    }
                    else {
                        HttpRequestBuilder request = httpClient.post(RELAY);
                        request.content(
                                url.getBytes(),
                                null
                        );
                        HttpResponse response = null;

                        request.setupSecureConnection(httpClient.getSSLStore());
                        response = request.execute();
                        assert response != null;

                        InputStream response_in = response.getPayload();
                        String response_body = response.readString();
                        int response_code = response.getStatusCode();
                        long content_length = response.getContentLength();
                        byte[] buf = new byte[(int) content_length];
                        FileOutputStream fos = new FileOutputStream(cachedPreview);
                        int inByte;

                        while ((inByte = response_in.read()) != -1) {
                            fos.write(inByte);
                            filesize++;
                        }
                        Log.i(TAG, "run: " + url + " " + buf.length);
                        bmp = BitmapFactory.decodeByteArray(buf, 0, buf.length);
                        fos.write(buf);
                        response_in.close();
                    }

                    if(bmp != null) {
                        callback.onLoad(Bitmap.createScaledBitmap(bmp, 128, 128, true));
                        bmp.recycle();
                    }
                    //callback.error("Ошибка при загрузке значков");

                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Generic error: " + e.getLocalizedMessage());
                        }
                    });
                }
            }
        });
    }

    public Video getVideoDescription(JSONObject obj) throws JSONException
    {
        Video ret = new Video();

        ret.name = obj.getString("title");
        ret.id = obj.getString("videoId");
        ret.likes = obj.getInt("likeCount");
        ret.views = obj.getInt("viewCount");

        JSONArray adaptiveFormats = obj.getJSONArray("formatStreams");

        for(int i = 0; i < adaptiveFormats.length(); i++)
        {
            JSONObject fmt = adaptiveFormats.getJSONObject(i);
            Log.i("", "getVideoDescription: " + fmt.getString("type"));
            if(fmt.getString("type").contains("video/mp4"))
            {
                ret.url = fmt.getString("url");
                break;
            }
        }

        return ret;
    }

    public ArrayList<Video> getVideoList(JSONArray obj) throws JSONException
    {
        ArrayList<Video> ret = new ArrayList<>();

        for (int i = 0; i < obj.length(); i++) {
            JSONObject jVideo = obj.getJSONObject(i);
            Video video = new Video();

                video.id = jVideo.getString("videoId");
                video.name = jVideo.getString("title");
                video.uploadDate = jVideo.getString("publishedText");
                video.views = jVideo.getInt("viewCount");
                JSONArray preview = jVideo.getJSONArray("videoThumbnails");
                video.preview = preview.getJSONObject(preview.length() - 1).getString("url");

                int length = jVideo.getInt("lengthSeconds");
                video.length = (length / 60) + ":" + (length - ((length / 60) * 60));

                ret.add(video);
        }

        return ret;
    }

    public void request(final String method, final APICallback runnable)
    {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try
                {
                    HttpRequestBuilder request = httpClient.post(RELAY);
                    httpClient.setSSLStore(SSLDummyChecker.disableSSLCertificateChecking());
                    request.setupSecureConnection(httpClient.getSSLStore());
                    request.content(
                            String.format("%s%s", instance, method).getBytes(),
                            null
                    );
                    HttpResponse response = request.execute();
                    assert response != null;
                    final String response_body = response.readString();
                    int response_code = response.getStatusCode();
                    Log.i(TAG, "run: " + instance + method);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            runnable.onSuccess(response_body);
                        }
                    });
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            runnable.onError("Generic error: " + e.getLocalizedMessage());
                        }
                    });
                }
            }
        });

    }
}
