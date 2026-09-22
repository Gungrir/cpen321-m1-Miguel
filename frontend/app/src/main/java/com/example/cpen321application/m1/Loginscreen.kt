package com.example.cpen321application.m1

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class ServerInfo(
    val serverIp: String,
    val clientIp: String,
    val serverTime: String,
    val clientTime: String,
    val myName: String,
    val userName: String
)

@Composable
fun LoginScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var userName by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<ServerInfo?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun run(block: suspend () -> Unit) {
        scope.launch {
            loading = true
            error = null
            try {
                block()
            } catch (e: GetCredentialCancellationException) {
                error = "Sign-in was cancelled. Tap Sign in with Google to try again."
            } catch (e: Exception) {
                error = "Couldn't finish: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                loading = false
            }
        }
    }

    val signIn = {
        run {
            val name = signInWithGoogle(context)
            userName = name
            info = loadServerInfo(name)
        }
    }

    val signedIn = info != null
    LaunchedEffect(signedIn) {
        if (!signedIn) return@LaunchedEffect
        while (isActive) {
            val serverTime = runCatching {
                ApiClient.getJson("/api/server-time").getString("time")
            }.getOrNull()
            info = info?.let { current ->
                current.copy(
                    serverTime = serverTime ?: current.serverTime,
                    clientTime = clientTimeNow()
                )
            }
            delay(1000)
        }
    }

    ScreenScaffold(title = "Sign in and server info", onBack = onBack) {
        when {
            loading -> {
                Spacer(Modifier.height(48.dp))
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Contacting your server…")
            }

            info != null -> {
                InfoCard(info!!)
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = { run { info = loadServerInfo(userName!!) } }) {
                    Text("Reload server info")
                }
            }

            else -> {
                Spacer(Modifier.height(48.dp))
                Text(
                    "Sign in with your Google account to connect to the server.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = signIn) { Text("Sign in with Google") }
            }
        }

        error?.let {
            Spacer(Modifier.height(20.dp))
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}

private suspend fun signInWithGoogle(context: Context): String {
    val option = GetSignInWithGoogleOption.Builder(M1Config.GOOGLE_WEB_CLIENT_ID).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = CredentialManager.create(context).getCredential(context, request).credential

    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        val fullName = "${google.givenName.orEmpty()} ${google.familyName.orEmpty()}".trim()
        return fullName.ifEmpty { google.displayName ?: google.id }
    }
    throw IllegalStateException("Google didn't return an ID token credential")
}


private suspend fun loadServerInfo(userName: String): ServerInfo = coroutineScope {
    val serverIp = async { ApiClient.getJson("/api/server-ip").getString("ip") }
    val clientIp = async { ApiClient.getJson("/api/client-ip").getString("ip") }
    val serverTime = async { ApiClient.getJson("/api/server-time").getString("time") }
    val myName = async {
        val json = ApiClient.getJson("/api/name")
        "${json.getString("firstName")} ${json.getString("lastName")}"
    }
    val clientTime = clientTimeNow()

    ServerInfo(
        serverIp = serverIp.await(),
        clientIp = clientIp.await(),
        serverTime = serverTime.await(),
        clientTime = clientTime,
        myName = myName.await(),
        userName = userName
    )
}


fun clientTimeNow(): String =
    ZonedDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss 'GMT'xxx"))

@Composable
private fun InfoCard(info: ServerInfo) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                "Signed in as ${info.userName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            val rows = listOf(
                "Server IP" to info.serverIp,
                "Client IP" to info.clientIp,
                "Server time" to info.serverTime,
                "Client time" to info.clientTime,
                "Developer name" to info.myName,
                "Signed-in user" to info.userName
            )
            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider()
                InfoRow(label, value)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(value, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
    }
}