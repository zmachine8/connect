package ee.ut.connect.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginRoute(onContinue: () -> Unit) {
    LoginScreen(onContinue)
}

@Composable
fun LoginScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Connect", style = MaterialTheme.typography.displaySmall)
        Text("Private conversations with people you know", modifier = Modifier.padding(vertical = 16.dp))
        Button(onClick = onContinue) { Text("Continue to prototype") }
        Text(
            "Authentication will be added in the Firebase stage.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
