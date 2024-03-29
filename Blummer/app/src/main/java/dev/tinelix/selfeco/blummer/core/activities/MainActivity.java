package dev.tinelix.selfeco.blummer.core.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.monobodgan.selfeco.BuildConfig;
import com.monobodgan.selfeco.R;
import dev.tinelix.selfeco.blummer.api.models.History;
import dev.tinelix.selfeco.blummer.api.VideoDownloader;
import dev.tinelix.selfeco.blummer.api.YTAPI;
import dev.tinelix.selfeco.blummer.core.utilities.SSLDummyChecker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private YTAPI api;
    private TabHost tabs;

    private History history;
    private ArrayList<YTAPI.Video> videos;

    private ViewGroup trendsView;
    private View loadingView;

    private VideoDownloader downloader;

    public YTAPI getApi() {
        return api;
    }

    public static AlertDialog.Builder buildGenericError(Context ctx, String errorString) {
        AlertDialog.Builder al = new AlertDialog.Builder(ctx)
                .setMessage(errorString)
                .setTitle(ctx.getResources().getString(R.string.error_occurred));
        al.show();
        return al;
    }

    private void inflateUI(View parent, ArrayList<YTAPI.Video> videos) {
        ((ViewGroup) parent).removeAllViews();

        for (final YTAPI.Video video : videos) {
            View fragment = getLayoutInflater().inflate(R.layout.frag_video, null, false);

            fragment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPlayVideo(video);
                }
            });

            ((ViewGroup) parent).addView(fragment.findViewById(R.id.view_video));
            TextView vName = fragment.findViewById(R.id.video_name);
            vName.setText(video.name);
            TextView vDesc = fragment.findViewById(R.id.video_desc);
            vDesc.setText(video.length);
            final ImageView vPreview = fragment.findViewById(R.id.video_preview);

            api.schedulePreviewDownload(video.preview, video.id + ".png", new YTAPI.PreviewCallback() {
                @Override
                public void loaded(final Bitmap scaledBitmap) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            vPreview.setImageBitmap(scaledBitmap);
                        }
                    });
                }

                @Override
                public void error(String reason) {

                }
            });
        }
    }

    private void playVideo(String url) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url), "video/mp4");

        startActivity(intent);
    }

    private void onPlayVideo(YTAPI.Video video) {
        Log.i("TAG", "onPlayVideo: Fetching video...");

        final ProgressDialog preDialog = new ProgressDialog(MainActivity.this);
        preDialog.setMessage(getResources().getString(R.string.getting_info));
        preDialog.setCancelable(false);
        preDialog.show();

        api.request("videos/" + video.id, new YTAPI.Callback() {
            @Override
            public void success(String obj) {
                try {
                    YTAPI.Video video = api.getVideoDescription(new JSONObject(obj));

                    preDialog.cancel();

                    final ProgressDialog dlg = new ProgressDialog(MainActivity.this);
                    dlg.setMessage(getResources().getString(R.string.loading));
                    dlg.setCancelable(false);
                    dlg.show();

                    history.put(video);
                    updateHistory();

                    if(Build.VERSION.SDK_INT > 999) {
                        playVideo(video.url);
                    }
                    else
                    {
                        downloader.beginDownloading(video.url, video.id, new VideoDownloader.Callback() {
                            @Override
                            public void success(String fileName) {
                                dlg.cancel();

                                playVideo("file://" + fileName);
                            }

                            @Override
                            public void reportProgress(final int progress, final int total) {
                                runOnUiThread(new Runnable() {
                                    @SuppressLint("StringFormatMatches")
                                    @Override
                                    public void run() {
                                        int percent = (int) (((float) progress / total) * 100);
                                        dlg.setMessage(
                                                getResources().getString(R.string.loading_with_percent, percent)
                                        );
                                    }
                                });
                            }

                            @Override
                            public void failed(final String reason) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dlg.cancel();

                                        new AlertDialog.Builder(MainActivity.this)
                                                .setTitle(getResources().getString(R.string.error_occurred))
                                                .setMessage(getResources().getString(R.string.error_dialog_text, reason))
                                                .show();
                                    }
                                });

                            }
                        });
                    }
                } catch (JSONException e) {
                    preDialog.cancel();
                    buildGenericError(MainActivity.this, e.getLocalizedMessage()).show();
                }
            }

            @Override
            public void error(String reason) {
                buildGenericError(MainActivity.this, "Не удалось получить данные о видео :(").show();
            }
        });
    }

    public void onSearch(View view) {
        String query = ((EditText) findViewById(R.id.query_search)).getText().toString();

        api.request("search?q=" + URLEncoder.encode(query) + "&type=video&region=RU&hl=ru", new YTAPI.Callback() {
            @Override
            public void success(String obj) {
                try {
                    videos = api.getVideoList(new JSONArray(obj));

                    inflateUI(findViewById(R.id.search_content), videos);
                } catch (JSONException e) {
                    buildGenericError(MainActivity.this, getResources().getString(R.string.loading_error_dialog_text));

                    e.printStackTrace();
                }
            }

            @Override
            public void error(String reason) {
                Log.i("", "error: ");
                buildGenericError(MainActivity.this, reason).show();
            }
        });
    }

    private void updateTrending() {
        api.request("trending?region=RU&hl=ru", new YTAPI.Callback() {
            @Override
            public void success(String obj) {
                try {
                    videos = api.getVideoList(new JSONArray(obj));

                    inflateUI(findViewById(R.id.trends_content), videos);
                } catch (JSONException e) {
                    buildGenericError(MainActivity.this, getResources().getString(R.string.loading_error_dialog_text));

                    e.printStackTrace();
                }
            }

            @Override
            public void error(String reason) {
                Log.i("", "error: ");
                buildGenericError(MainActivity.this, reason).show();
            }
        });
    }

    private void updatePopular() {
        api.request("popular?region=RU&hl=ru", new YTAPI.Callback() {
            @Override
            public void success(String obj) {
                //loadingView.setVisibility(View.GONE);

                try {
                    videos = api.getVideoList(new JSONArray(obj));

                    inflateUI(findViewById(R.id.popular_content), videos);
                } catch (JSONException e) {
                    buildGenericError(MainActivity.this, getResources().getString(R.string.loading_error_dialog_text));

                    e.printStackTrace();
                }
            }

            @Override
            public void error(String reason) {
                Log.i("", "error: ");
                buildGenericError(MainActivity.this, reason).show();
            }
        });
    }

    private void prepareUI() {

    }

    private void updateHistory()
    {
        inflateUI(findViewById(R.id.history_content), history.getVideos());
    }

    private View createTabView(String text)
    {
        View hdr = getLayoutInflater().inflate(R.layout.frag_tab, null, false);

        ((TextView) hdr.findViewById(R.id.indicator)).setText(text);
        return hdr;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SSLDummyChecker.disableSSLCertificateChecking();

        history = new History(this);

        api = new YTAPI(this);
        downloader = new VideoDownloader(this);
        downloader.createCacheFolder();

        setContentView(R.layout.activity_main);
        tabs = (TabHost) findViewById(R.id.tab_host);
        tabs.setup();
        tabs.addTab(tabs.newTabSpec("trends")
                .setIndicator(
                        "Тренды", getResources().getDrawable(R.drawable.trending)
                ).setContent(R.id.tab1));
        tabs.addTab(tabs.newTabSpec("popular")
                .setIndicator(
                        "Популярное", getResources().getDrawable(R.drawable.star)
                ).setContent(R.id.tab2)
        );
        tabs.addTab(tabs.newTabSpec("history")
                .setIndicator(
                        "История", getResources().getDrawable(R.drawable.history)
        ).setContent(R.id.tab3));
        tabs.addTab(tabs.newTabSpec("search")
                .setIndicator(
                        "Поиск", getResources().getDrawable(R.drawable.search)
                ).setContent(R.id.tab4));

        tabs.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (tabId.equals("popular"))
                    updatePopular();
            }
        });

        prepareUI();

        updateTrending();
        updateHistory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Очистить кэш").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                File[] files = downloader.getCacheDir().listFiles();
                if(files != null) {
                    for (File f : files) {
                        if (!f.isDirectory())
                            f.delete();
                    }

                    Toast.makeText(
                            MainActivity.this, getResources().getString(R.string.cache_cleared),
                            Toast.LENGTH_SHORT
                    ).show();
                }
                return true;
            }
        });

        menu.add(getResources().getString(R.string.about_app_title)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}