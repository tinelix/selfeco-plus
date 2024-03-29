package dev.tinelix.selfeco.blummer.core;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;

public class Global {

    public static Drawable tintDrawable(
            Context ctx, @DrawableRes int drawableResId, @ColorInt final int colorResId
    ) {
        Drawable drawable = ctx.getResources().getDrawable(drawableResId);
        drawable.setColorFilter(colorResId, PorterDuff.Mode.SRC_IN);
        return drawable;
    }
}
