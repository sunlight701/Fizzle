package ch.seme.views;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

public class EmojiEditText extends hani.momanii.supernova_emoji_library.Helper.EmojiconEditText{
    static private final String[] SUPPORTED_MIME_TYPES = new String[]{"image/png", "image/jpg", "image/gif", "image/webp"};

    public interface MediaListener {
        void onMedia(InputContentInfoCompat mediaUri);
    }

    private MessageEditText.MediaListener listener = null;

    public EmojiEditText(Context context) {
        super(context);
    }

    public EmojiEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmojiEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setMediaListener(MessageEditText.MediaListener l) {
        listener = l;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        final InputConnection ic = super.onCreateInputConnection(editorInfo);
        EditorInfoCompat.setContentMimeTypes(editorInfo, SUPPORTED_MIME_TYPES);
        return InputConnectionCompat.createWrapper(ic, editorInfo, (inputContentInfo, flags, opts) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
                    && (flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
                try {
                    inputContentInfo.requestPermission();
                } catch (Exception e) {
                    return false;
                }
            }
            if (listener != null)
                listener.onMedia(inputContentInfo);
            return true;
        });
    }
}
