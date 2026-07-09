package org.sensorhub.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sensorhub.android.ui.components.OSHButton
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHInputField
import org.sensorhub.android.ui.components.OSHSegmentedButton
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun ServerFormScreen(
    onBackClick: () -> Unit
) {

    var id by rememberSaveable { mutableStateOf("") }
    var serverName by rememberSaveable { mutableStateOf("") }
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("") }
    var endpointPath by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var enableTls by rememberSaveable { mutableStateOf("") }
    var disableSslCheck by rememberSaveable { mutableStateOf("") }
    var useConSysClient by rememberSaveable { mutableStateOf("") }
    var oAuthEnabled by rememberSaveable { mutableStateOf("") }
    var enabled by rememberSaveable { mutableStateOf("") }
    var clientId by rememberSaveable { mutableStateOf("") }
    var clientSecret by rememberSaveable { mutableStateOf("") }
    var tokenEndpoint by rememberSaveable { mutableStateOf("") }

    // validation
    var portError by rememberSaveable { mutableStateOf<String?>(null) }


    var isPasswordVisible by remember { mutableStateOf(false) }
    var isClientSecretVisible by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(
                title = "Server Form",
                onBackClick = onBackClick
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OSHCard(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                OSHSegmentedButton(
                    options = listOf("CS API CLient", "SOS-T Client")
                )
            }

            OSHCard(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                OSHInputField(
                    value = serverName,
                    onValueChange = {  serverName = it },
                    label = "Server Name",
                )
                OSHInputField(
                    value = host,
                    onValueChange = {  host = it },
                    label = "Host/ IP Address",
                )
                OSHInputField(
                    value = endpointPath,
                    onValueChange = {  endpointPath = it },
                    label = "Endpoint Path",
                )
            }


            OSHInputField(
                value = username,
                onValueChange = {  username = it },
                label = "Username",
            )
            OSHInputField(
                value = password,
                onValueChange = {  password = it },
                label = "Password",
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )


            OSHButton(
                onClick = {},
                text = "Add Server",
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }
}


data class ServerProfile(
    val id: String = "",
    val name: String = "",
    val host: String = "",
    val port: Int = 8181,
    val endpointPath: String = "/sensorhub/api",
    val username: String = "",
    val password: String = "",
    val enableTls: Boolean = false,
    val disableSslCheck: Boolean = false,
    val useConSysClient: Boolean = false,
    val oAuthEnabled: Boolean = false,
    val enabled: Boolean = false,
    val clientId: String = "",
    val clientSecret: String = "",
    val tokenEndpoint: String = ""
) {
    val isValid: Boolean get() = name.isNotBlank() && host.isNotBlank() && ( port >= 0 && port <= 65513) //name == null && host == null && port == null &&
}


@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ServerProfilesScreenPreview() {
    OSHTheme {
        ServerFormScreen(
            onBackClick = {},
        )
    }
}
