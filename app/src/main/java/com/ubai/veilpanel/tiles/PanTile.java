package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;

/**
 * Base contract for a PanOSP quick-settings tile.
 *
 * The UI only knows about this abstraction. Android-specific control logic is
 * kept in system controllers.
 */
public abstract class PanTile {

    protected final Context context;

    private final String spec;
    private final int labelRes;

    protected PanTile(
            Context context,
            String spec,
            int labelRes) {
        this.context = context;
        this.spec = spec;
        this.labelRes = labelRes;
    }

    public String getSpec() {
        return spec;
    }

    public String getLabel() {
        return context.getString(labelRes);
    }

    public abstract TileStatus getStatus();

    public abstract void handleClick();

    public boolean handleLongClick() {
        return false;
    }

    public String getStateLabel() {
        switch (getStatus()) {
            case ACTIVE:
                return context.getString(R.string.tile_state_on);

            case INACTIVE:
                return context.getString(R.string.tile_state_off);

            case UNAVAILABLE:
            default:
                return context.getString(R.string.tile_state_unavailable);
        }
    }

    public String getDisplayText() {
        return getLabel() + "\n" + getStateLabel();
    }
}
