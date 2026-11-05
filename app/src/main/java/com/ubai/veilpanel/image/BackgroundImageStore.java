package com.ubai.veilpanel.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/** Private, immutable image generations. All image and file work runs off the UI thread. */
public final class BackgroundImageStore {
    public static final long MAX_IMAGE_BYTES = 128L * 1024 * 1024;
    private final Context context;
    private final File directory;

    public BackgroundImageStore(Context context) {
        this.context = context.getApplicationContext();
        directory = new File(this.context.getFilesDir(), "background");
    }

    private File newFile() throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create image directory");
        return new File(directory, UUID.randomUUID() + ".webp");
    }

    public String prepare(Uri source) throws IOException {
        File file = newFile();
        Bitmap decoded = null;
        Bitmap scaled = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream in = open(source)) { BitmapFactory.decodeStream(in, null, options); }
            if (options.outWidth <= 0 || options.outHeight <= 0) throw new IOException("Invalid source image");
            int screen = Math.max(context.getResources().getDisplayMetrics().widthPixels,
                    context.getResources().getDisplayMetrics().heightPixels);
            int target = Math.min(4096, Math.max(2048, (int) Math.ceil(1.5 * screen)));
            int largest = Math.max(options.outWidth, options.outHeight);
            options.inSampleSize = 1;
            while (largest / (options.inSampleSize * 2L) >= target) options.inSampleSize *= 2;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            // BitmapFactory preserves the cropper's source pixel axes, without applying EXIF rotation.
            try (InputStream in = open(source)) { decoded = BitmapFactory.decodeStream(in, null, options); }
            if (decoded == null) throw new IOException("Cannot decode source image");
            float ratio = Math.min(1f, (float) target / Math.max(decoded.getWidth(), decoded.getHeight()));
            scaled = ratio < 1f ? Bitmap.createScaledBitmap(decoded,
                    Math.max(1, Math.round(decoded.getWidth() * ratio)),
                    Math.max(1, Math.round(decoded.getHeight() * ratio)), true) : decoded;
            try (FileOutputStream out = new FileOutputStream(file)) {
                if (!scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 95, out)) throw new IOException("WebP encoding failed");
                out.getFD().sync();
            }
            validate(file);
            return Uri.fromFile(file).toString();
        } catch (IOException | RuntimeException | OutOfMemoryError error) {
            file.delete();
            throw new IOException("Image preparation failed", error);
        } finally {
            if (scaled != null && scaled != decoded) scaled.recycle();
            if (decoded != null) decoded.recycle();
        }
    }

    public File stageArchive(InputStream input) throws IOException {
        File file = File.createTempFile("panosp-backup-", ".zip", context.getCacheDir());
        try {
            try (FileOutputStream output = new FileOutputStream(file)) {
                copy(input, output, MAX_IMAGE_BYTES + 2L * 1024 * 1024);
            }
            return file;
        } catch (IOException | RuntimeException error) {
            file.delete();
            throw error;
        }
    }

    public String prepareWebp(InputStream input) throws IOException {
        File file = newFile();
        try {
            try (FileOutputStream out = new FileOutputStream(file)) {
                copy(input, out, MAX_IMAGE_BYTES);
                out.getFD().sync();
            }
            validate(file);
            return Uri.fromFile(file).toString();
        } catch (IOException | RuntimeException | OutOfMemoryError error) {
            file.delete();
            throw new IOException("Invalid backup image", error);
        }
    }

    public void validate(File file) throws IOException {
        if (file.length() == 0 || file.length() > MAX_IMAGE_BYTES) throw new IOException("Invalid image size");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), options);
        if (!"image/webp".equals(options.outMimeType) || options.outWidth <= 0 || options.outHeight <= 0
                || options.outWidth > 4096 || options.outHeight > 4096) throw new IOException("Invalid WebP dimensions");
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), options);
        if (bitmap == null) throw new IOException("Undecodable WebP");
        bitmap.recycle();
    }

    public InputStream open(Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) throw new IOException("Cannot open image");
        return input;
    }

    public boolean isManaged(String uri) {
        return managedFile(uri) != null;
    }

    private File managedFile(String value) {
        if (value == null) return null;
        try {
            Uri uri = Uri.parse(value);
            if (!"file".equals(uri.getScheme()) || uri.getPath() == null) return null;
            File file = new File(uri.getPath()).getCanonicalFile();
            return directory.getCanonicalFile().equals(file.getParentFile())
                    && file.getName().endsWith(".webp") ? file : null;
        } catch (IOException | RuntimeException ignored) { return null; }
    }

    public void deleteManaged(String uri) {
        File file = managedFile(uri);
        if (file != null) file.delete();
    }

    public static void copy(InputStream input, java.io.OutputStream output, long limit) throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > limit) throw new IOException("Backup entry too large");
            output.write(buffer, 0, count);
        }
    }
}
