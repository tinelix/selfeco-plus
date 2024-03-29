package dev.tinelix.selfeco.blummer.core.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.widget.TextView;

import dev.tinelix.selfeco.blummer.BuildConfig;
import dev.tinelix.selfeco.blummer.R;

public class AboutActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        inflateUI();
    }

    private void inflateUI() {
        TextView app_version = findViewById(R.id.version);
        TextView app_description = findViewById(R.id.app_description);
        TextView app_footer = findViewById(R.id.footer);
        TextView app_copyright = findViewById(R.id.copyright);

        app_version.setText(getResources().getString(R.string.version, BuildConfig.VERSION_NAME));

        app_description.setText(
            getResources().getString(
                R.string.app_description,
                getResources().getString(R.string.app_name)
            )
        );
        app_footer.setText(getResources().getString(R.string.app_description_2));
        app_copyright.setText(
                Html.fromHtml(getResources().getString(R.string.app_copyright))
        );
    }
}
