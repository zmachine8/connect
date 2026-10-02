package ee.ut.connect.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ee.ut.connect.R

@Composable
fun FantasyBackdrop(login: Boolean = false, chat: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(if (login) R.drawable.infernal_gate else R.drawable.ember_hall),
            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            if (login) listOf(Color.Black.copy(alpha = .12f), Color.Black.copy(alpha = .35f), Color.Black.copy(alpha = .85f))
            else if (chat) listOf(Color.Black.copy(alpha = .92f), Color.Black.copy(alpha = .85f), Color.Black.copy(alpha = .78f))
            else listOf(Color.Black.copy(alpha = .90f), Color.Black.copy(alpha = .80f), Color.Black.copy(alpha = .55f)))))
        content()
    }
}

@Composable
fun InitialAvatar(name: String, modifier: Modifier = Modifier) {
    val accents = listOf(Color(0xFF65311F), Color(0xFF293C45), Color(0xFF304335), Color(0xFF594022))
    val index = (name.fold(0) { sum, char -> sum + char.code } and Int.MAX_VALUE) % accents.size
    Box(modifier.size(52.dp).border(1.dp, Color(0xFFE0A363), CircleShape).padding(3.dp)
        .border(1.dp, Color(0xFF735538), CircleShape).clip(CircleShape)
        .background(Brush.radialGradient(listOf(accents[index], Color(0xFF171615))))) {
        Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge,
            color = Color(0xFFFFDFC0), modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun fantasyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xF21D1C1A),
    unfocusedContainerColor = Color(0xEB171716),
    disabledContainerColor = Color(0xEB171716),
    focusedBorderColor = Color(0xFFE9AF68),
    unfocusedBorderColor = Color(0xFF806347),
)

@Composable
fun EmberButton(onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier,
                content: @Composable RowScope.() -> Unit) {
    val shape = CutCornerShape(8.dp)
    val glow = if (enabled) Color(0xFFEBA252) else Color(0xFF514436)
    Button(onClick = onClick, enabled = enabled, shape = shape,
        border = BorderStroke(1.dp, glow),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
            contentColor = Color(0xFFFFE9CF), disabledContainerColor = Color.Transparent,
            disabledContentColor = Color(0xFF938777)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier.heightIn(min = 52.dp).drawBehind {
            drawRoundRect(glow.copy(alpha = .06f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()))
        }.clip(shape).background(Brush.verticalGradient(
            if (enabled) listOf(Color(0xFF9C5529), Color(0xFF512A18), Color(0xFF703B20))
            else listOf(Color(0xFF302B25), Color(0xFF24211D)))) , content = content)
}

@Composable
fun CopperDivider(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFF775333))
        Text("◇", color = Color(0xFFE9AF68))
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFF775333))
    }
}
