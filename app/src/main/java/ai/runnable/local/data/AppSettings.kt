package ai.runnable.local.data

import android.content.Context

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var huggingFaceToken: String?
        get() = prefs.getString(KEY_HF_TOKEN, null)
        set(value) {
            val trimmed = value?.trim().orEmpty()
            if (trimmed.isBlank()) {
                prefs.edit().remove(KEY_HF_TOKEN).apply()
            } else {
                prefs.edit().putString(KEY_HF_TOKEN, trimmed).apply()
            }
        }

    companion object {
        private const val PREFS_NAME = "runnable_settings"
        private const val KEY_HF_TOKEN = "hugging_face_token"
    }
}
