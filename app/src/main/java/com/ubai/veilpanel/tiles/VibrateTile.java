package com.ubai.veilpanel.tiles;

import android.content.Context;
import android.media.AudioManager;

import com.ubai.veilpanel.R;

/**
 * Direct Android vibrate-mode tile.
 */
public final class VibrateTile extends PanTile {

    private final AudioManager audioManager;

    public VibrateTile(Context context) {
        super(context, "vibrate", R.string.tile_vibrate);

        audioManager =
                context.getSystemService(AudioManager.class);
    }

    @Override
    public TileStatus getStatus() {
        if (audioManager == null) {
            return TileStatus.UNAVAILABLE;
        }

        return audioManager.getRingerMode()
                == AudioManager.RINGER_MODE_VIBRATE
                ? TileStatus.ACTIVE
                : TileStatus.INACTIVE;
    }

    @Override
    public void handleClick() {
        if (audioManager == null) {
            return;
        }

        try {
            int target =
                    audioManager.getRingerMode()
                            == AudioManager.RINGER_MODE_VIBRATE
                            ? AudioManager.RINGER_MODE_NORMAL
                            : AudioManager.RINGER_MODE_VIBRATE;

            audioManager.setRingerMode(target);

        } catch (RuntimeException ignored) {
        }
    }
}
