package dev.tinelix.selfeco.blummer.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import org.pixmob.httpclient.HttpClient;
import org.pixmob.httpclient.HttpRequestBuilder;
import org.pixmob.httpclient.HttpResponse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

import dev.tinelix.selfeco.blummer.core.utilities.SSLDummyChecker;

public class VideoDownloader {
    private File cacheDir;

    public interface Callback
    {
        void success(String fileName);
        void reportProgress(int progress, int total);
        void failed(String reason);
    }

    private boolean jobInProgress;
    private HttpClient httpClient = null;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SdCardPath")
    public VideoDownloader(Context ctx)
    {
        cacheDir = new File("/sdcard/DCIM/selfeco-invidious/");
        cacheDir.mkdirs();
        httpClient = new HttpClient(ctx);
        httpClient.setConnectTimeout(30000);
        httpClient.setReadTimeout(30000);
        httpClient.setSSLStore(SSLDummyChecker.disableSSLCertificateChecking());
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public boolean createCacheFolder()
    {
        return cacheDir.mkdirs();
    }

    public void beginDownloading(final String url, final String fileName, final Callback cb)
    {
        if(jobInProgress)
        {
            cb.failed("Загрузка уже идёт");

            return;
        }

        final File incompleted = new File(cacheDir.getAbsolutePath() + "/" + fileName + ".incompleted");
        final File cached = new File(cacheDir.getAbsolutePath() + "/" + fileName + ".mp4");
        if(cached.exists() && !incompleted.exists())
        {
            cb.success(cached.getAbsolutePath());
            return;
        }

        jobInProgress = true;
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public void run() {
                try
                {
                    HttpRequestBuilder request = httpClient.get(url);
                    request.setupSecureConnection(SSLDummyChecker.disableSSLCertificateChecking());
                    HttpResponse response = request.execute();
                    assert response != null;
                    InputStream response_in = response.getPayload();
                    long content_length = response.getContentLength();
                    Log.i("", "run: " + fileName);
                    BufferedInputStream reader = new BufferedInputStream(response_in, 4096000);

                    cached.createNewFile();
                    incompleted.createNewFile();
                    FileOutputStream os = new FileOutputStream(cached);
                    byte[] buffer = new byte[4096000];
                    int numRead = 0;

                    while(numRead < content_length)
                    {
                        int amt = reader.read(buffer);
                        os.write(buffer, 0, amt);

                        numRead += amt;
                        cb.reportProgress(numRead / 1024, (int) (content_length / 1024));
                    }

                    os.close();
                    incompleted.delete();
                    cb.success(cached.getAbsolutePath());
                }
                catch (Exception e)
                {
                    e.printStackTrace();

                    cb.failed(e.getLocalizedMessage());
                }

                jobInProgress = false;
            }
        });
    }
}
