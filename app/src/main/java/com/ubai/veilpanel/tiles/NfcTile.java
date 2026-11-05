package com.ubai.veilpanel.tiles;

import android.content.Context;
import android.nfc.NfcAdapter;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.SettingsLauncher;

public final class NfcTile extends PanTile {

    private final SettingsLauncher settingsLauncher;

    public NfcTile(
            Context context,
            SettingsLauncher settingsLauncher) {

        super(context, "nfc", R.string.tile_nfc);
        this.settingsLauncher = settingsLauncher;
    }

    private boolean isEnabled() {
        NfcAdapter adapter =
                NfcAdapter.getDefaultAdapter(context);

        return adapter != null && adapter.isEnabled();
    }

    @Override
    public TileStatus getStatus() {
        return isEnabled()
                ? TileStatus.ACTIVE
                : TileStatus.INACTIVE;
    }

    @Override
    public void handleClick() {
        settingsLauncher.openNfcSettings();
    }
}
