package ai.runnable.local.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun ChatActions(
    textToShare: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = {
                clipboardManager.setText(AnnotatedString(textToShare))
            },
            enabled = textToShare.isNotBlank()
        ) {
            Text("Copy")
        }
        OutlinedButton(
            onClick = {
                if (textToShare.isNotBlank()) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, textToShare)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share output"))
                }
            },
            enabled = textToShare.isNotBlank()
        ) {
            Text("Share")
        }
        Spacer(modifier = Modifier.width(2.dp))
        OutlinedButton(onClick = onClear) {
            Text("Clear")
        }
    }
}
