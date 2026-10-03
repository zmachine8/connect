package ee.ut.connect.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Measures the usable window after system bars and the keyboard are excluded. */
@Composable
fun AdaptiveContent(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(compact: Boolean) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        content(maxHeight < 480.dp)
    }
}
