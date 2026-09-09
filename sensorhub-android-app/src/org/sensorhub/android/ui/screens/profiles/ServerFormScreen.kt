package org.sensorhub.android.ui.screens.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sensorhub.android.R
import org.sensorhub.android.ui.components.OSHButton
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHInputField
import org.sensorhub.android.ui.components.OSHSwitchRow
import org.sensorhub.android.ui.components.OSHTonalButton
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.Error
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.android.ui.theme.Success
import org.sensorhub.android.ui.theme.TextSecondary

@Composable
fun ServerFormScreen(
    onBackClick: () -> Unit,
    profileId: String? = null,
    viewModel: ServerFormViewModel = viewModel()
) {
    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId)
    }

    val state = viewModel.state
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isClientSecretVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(
                title = stringResource(if (viewModel.isEdit) R.string.title_edit_server else R.string.add_server),
                onBackClick = onBackClick
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OSHInputField(
                value = state.serverName,
                onValueChange = { viewModel.updateServerName(it) },
                label = "Server name",
                error = viewModel.nameError
            )
            OSHCard {
                OSHInputField(
                    value = state.endpointUrl,
                    onValueChange = { viewModel.updateEndpointUrl(it) },
                    label = "Connection URL",
                    error = viewModel.endpointUrlError,
                    placeholder = { Text("https://ip:port/sensorhub/api", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                OSHInputField(
                    value = state.username,
                    onValueChange = { viewModel.updateUsername(it) },
                    label = "Username (Optional)",
                )
                OSHInputField(
                    value = state.password,
                    onValueChange = { viewModel.updatePassword(it) },
                    label = "Password (Optional)",
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
                OSHSwitchRow(
                    title = "Enable OAuth",
                    checked = state.enableOAuth,
                    onCheckedChange = { viewModel.updateEnableOAuth(it) },
                )
                if (state.enableOAuth) {
                    OSHInputField(
                        value = state.tokenEndpoint,
                        onValueChange = { viewModel.updateTokenEndpoint(it) },
                        label = "Token Endpoint",
                    )
                    OSHInputField(
                        value = state.clientId,
                        onValueChange = { viewModel.updateClientId(it) },
                        label = "Client ID",
                    )
                    OSHInputField(
                        value = state.clientSecret,
                        onValueChange = { viewModel.updateClientSecret(it) },
                        label = "Client Secret",
                        visualTransformation = if (isClientSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = {
                                isClientSecretVisible = !isClientSecretVisible
                            }) {
                                Icon(
                                    imageVector = if (isClientSecretVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle secret visibility"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OSHButton(
                onClick = {
                    if (viewModel.saveProfile()) onBackClick()
                },
                text = stringResource(if (viewModel.isEdit) R.string.title_edit_server else R.string.btn_add_server),
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isTestingConnection
            )

            Spacer(modifier = Modifier.height(16.dp))

            OSHTonalButton(
                onClick = { viewModel.testConnection() },
                text = if (viewModel.isTestingConnection) "Testing…"
                else stringResource(R.string.action_test_connection),
                modifier = Modifier.fillMaxWidth()
            )
            viewModel.connectionTestResult?.let { result ->
                ResultMessage(
                    label = result,
                    icon = if (result.startsWith("Connected")) Icons.Default.CheckCircle else Icons.Default.Error,
                    color = if (result.startsWith("Connected")) Success else Error
                )
            }

        }
    }
}

@Composable
private fun ResultMessage(
    label: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(modifier = Modifier.width(15.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = color
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ServerFormScreenPreview() {
    OSHTheme {
        ServerFormScreen(
            onBackClick = {},
            "0",
        )
    }
}
