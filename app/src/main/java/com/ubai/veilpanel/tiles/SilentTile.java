package com.ubai.veilpanel.tiles;

import android.content.Context;
import android.media.AudioManager;

import com.ubai.veilpanel.R;

/**
 * Direct Android silent-mode tile.
 */
public final class SilentTile extends PanTile {

    private final AudioManager audioManager;

    public SilentTile(Context context) {
        super(context, "silent", R.string.tile_silent);

        audioManager =
                context.getSystemService(AudioManager.class);
    }

    @Override
    public TileStatus getStatus() {
        if (audioManager == null) {
            return TileStatus.UNAVAILABLE;
        }

        return audioManager.getRingerMode()
                == AudioManager.RINGER_MODE_SILENT
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
                            == AudioManager.RINGER_MODE_SILENT
                            ? AudioManager.RINGER_MODE_NORMAL
                            : AudioManager.RINGER_MODE_SILENT;

            audioManager.setRingerMode(target);

        } catch (RuntimeException ignored) {
        }
    }
}
