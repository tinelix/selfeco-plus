package dev.tinelix.selfeco.blummer.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class VideoDownloader {
    private File cacheDir;

    public interface Callback
    {
        void success(String fileName);
        void reportProgress(int progress, int total);
        void failed(String reason);
    }

    private boolean jobInProgress;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SdCardPath")
    public VideoDownloader(Context ct)
    {
        cacheDir = new File("/sdcard/DCIM/selfeco-invidious/");
        cacheDir.mkdirs();
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
                    HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
                    conn.setDoInput(true);
                    conn.setRequestMethod("GET");
                    Log.i("", "run: " + fileName);
                    conn.connect();

                    int len = conn.getContentLength();
                    BufferedInputStream reader = new BufferedInputStream(conn.getInputStream(), 4096000);

                    cached.createNewFile();
                    incompleted.createNewFile();
                    FileOutputStream os = new FileOutputStream(cached);
                    byte[] buffer = new byte[4096000];
                    int numRead = 0;

                    while(numRead < len)
                    {
                        int amt = reader.read(buffer);
                        os.write(buffer, 0, amt);

                        numRead += amt;
                        cb.reportProgress(numRead / 1024, len / 1024);
                    }

                    conn.disconnect();
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
