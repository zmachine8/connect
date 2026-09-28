package ee.ut.connect.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.google.firebase.auth.FirebaseAuth
import ee.ut.connect.ui.chat.ChatRoute
import ee.ut.connect.ui.login.LoginRoute
import ee.ut.connect.ui.profile.ProfileRoute
import ee.ut.connect.ui.users.UsersRoute
import kotlinx.serialization.Serializable

@Serializable data object LoginDestination
@Serializable data object UsersDestination
@Serializable data object ProfileDestination
@Serializable data class ChatDestination(val userId: String, val displayName: String)

@Composable
fun ConnectApp() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val startDestination = if (auth.currentUser == null) LoginDestination else UsersDestination
    NavHost(navController = navController, startDestination = startDestination) {
        composable<LoginDestination> {
            LoginRoute {
                navController.navigate(UsersDestination) {
                    popUpTo(LoginDestination) { inclusive = true }
                }
            }
        }
        composable<UsersDestination> {
            UsersRoute(
                onOpenProfile = { navController.navigate(ProfileDestination) },
                onSignOut = {
                    auth.signOut()
                    navController.navigate(LoginDestination) {
                        popUpTo(UsersDestination) { inclusive = true }
                    }
                },
                onUserSelected = { user ->
                    navController.navigate(
                        ChatDestination(
                            userId = user.id,
                            displayName = user.displayName,
                        )
                    )
                },
            )
        }
        composable<ProfileDestination> {
            ProfileRoute(onBack = navController::navigateUp)
        }
        composable<ChatDestination> { entry ->
            val destination = entry.toRoute<ChatDestination>()
            ChatRoute(destination.userId, destination.displayName, navController::navigateUp)
        }
    }
}
