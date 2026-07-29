package org.sensorhub.android.ui.screens.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sensorhub.android.R
import org.sensorhub.android.server.ServerProfile
import org.sensorhub.android.server.ServerProfileRepository
import org.sensorhub.android.ui.components.OSHButton
import org.sensorhub.android.ui.components.OSHInputField
import org.sensorhub.android.ui.components.OSHSegmentedButton
import org.sensorhub.android.ui.components.OSHSwitchRow
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun ServerFormScreen(
    onBackClick: () -> Unit,
    profileId: String? = null
) {
    val context = LocalContext.current
    val repo = remember { ServerProfileRepository.getInstance(context) }
    val isEdit = profileId != null && profileId != "new"
    val existingProfile = remember { if (isEdit) repo.getById(profileId) else null }

    var serverName by rememberSaveable { mutableStateOf(existingProfile?.name ?: "") }
    var host by rememberSaveable { mutableStateOf(existingProfile?.host ?: "") }
    var port by rememberSaveable { mutableStateOf(existingProfile?.port?.toString() ?: "") }
    var endpointPath by rememberSaveable { mutableStateOf(existingProfile?.endpointPath ?: "/sensorhub/api") }
    var username by rememberSaveable { mutableStateOf(existingProfile?.username ?: "") }
    var password by rememberSaveable {
        mutableStateOf(if (isEdit && existingProfile != null) repo.getPassword(existingProfile.id) else "")
    }
    var enableTls by rememberSaveable { mutableStateOf(existingProfile?.enableTls ?: false) }
    var disableSslCheck by rememberSaveable { mutableStateOf(existingProfile?.disableSslCheck ?: false) }
    var enableOAuth by rememberSaveable { mutableStateOf(existingProfile?.oAuthEnabled ?: false) }
    var clientId by rememberSaveable {
        mutableStateOf(if (isEdit && existingProfile != null) repo.getOAuthClientId(existingProfile.id) else "")
    }
    var clientSecret by rememberSaveable {
        mutableStateOf(if (isEdit && existingProfile != null) repo.getOAuthClientSecret(existingProfile.id) else "")
    }
    var tokenEndpoint by rememberSaveable {
        mutableStateOf(if (isEdit && existingProfile != null) repo.getOAuthTokenEndpoint(existingProfile.id) else "")
    }

    var clientTypeIndex by rememberSaveable {
        mutableStateOf(if (existingProfile?.useConSysClient == false) 1 else 0)
    }

    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var hostError by rememberSaveable { mutableStateOf<String?>(null) }
    var portError by rememberSaveable { mutableStateOf<String?>(null) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isClientSecretVisible by remember { mutableStateOf(false) }

    val msgRequired = stringResource(R.string.msg_name_host_port_required)
    val msgNoProtocol = stringResource(R.string.msg_no_protocol)
    val msgPortNumber = stringResource(R.string.msg_port_number)
    val msgPortRange = stringResource(R.string.msg_port_range)

    fun validate(): Boolean {
        var valid = true
        nameError = if (serverName.isBlank()) { valid = false; msgRequired } else null
        hostError = if (host.isBlank()) {
            valid = false; msgRequired
        } else if (host.contains(" ") || host.contains("://")) {
            valid = false; msgNoProtocol
        } else null
        portError = if (port.isBlank()) {
            valid = false; msgRequired
        } else {
            val portNum = port.toIntOrNull()
            if (portNum == null) { valid = false; msgPortNumber }
            else if (portNum < 1 || portNum > 65535) { valid = false; msgPortRange }
            else null
        }
        return valid
    }

    fun saveProfile() {
        if (!validate()) return

        val profile = existingProfile ?: ServerProfile()
        profile.name = serverName.trim()
        profile.host = host.trim()
        profile.port = port.toInt()
        var ep = endpointPath.trim()
        if (ep.isNotEmpty() && !ep.startsWith("/")) ep = "/$ep"
        profile.endpointPath = ep
        profile.useConSysClient = clientTypeIndex == 0
        profile.enableTls = enableTls
        profile.disableSslCheck = disableSslCheck
        profile.oAuthEnabled = enableOAuth
        profile.username = if (!enableOAuth) username.trim() else ""

        repo.save(profile)

        val pwd = if (!enableOAuth) password.trim() else ""
        repo.setPassword(profile.id, pwd)

        if (enableOAuth) {
            repo.setOAuthClientId(profile.id, clientId.trim())
            repo.setOAuthClientSecret(profile.id, clientSecret.trim())
            repo.setOAuthTokenEndpoint(profile.id, tokenEndpoint.trim())
        }

        onBackClick()
    }

    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(
                title = stringResource(if (isEdit) R.string.title_edit_server else R.string.add_server),
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
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            OSHSegmentedButton(
                options = listOf("CS API Client", "SOS-T Client"),
                selectedIndex = clientTypeIndex,
                onOptionSelected = { index ->
                    clientTypeIndex = index
                    endpointPath = if (index == 0) "/sensorhub/api" else "/sensorhub/sos"
                }
            )

            OSHInputField(
                value = serverName,
                onValueChange = { serverName = it; nameError = null },
                label = "Server name",
                error = nameError
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OSHInputField(
                    value = host,
                    onValueChange = { host = it; hostError = null },
                    label = "Host / IP",
                    modifier = Modifier.weight(1f),
                    error = hostError
                )
                OSHInputField(
                    value = port,
                    onValueChange = { port = it; portError = null },
                    label = "Port",
                    modifier = Modifier.weight(0.4f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    error = portError
                )
            }
            OSHInputField(
                value = endpointPath,
                onValueChange = { endpointPath = it },
                label = "Endpoint path",
            )
            OSHSwitchRow(
                title = "Enable TLS",
                checked = enableTls,
                onCheckedChange = {
                    enableTls = it
                    if (!it) disableSslCheck = false
                },
            )

            if (enableTls) {
                OSHSwitchRow(
                    title = "Disable SSL Check",
                    checked = disableSslCheck,
                    onCheckedChange = { disableSslCheck = it },
                )
            }

            OSHSwitchRow(
                title = "Enable OAuth",
                checked = enableOAuth,
                onCheckedChange = { enableOAuth = it },
            )

            if (enableOAuth) {
                OSHInputField(
                    value = tokenEndpoint,
                    onValueChange = { tokenEndpoint = it },
                    label = "Token Endpoint",
                )
                OSHInputField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = "Client ID",
                )
                OSHInputField(
                    value = clientSecret,
                    onValueChange = { clientSecret = it },
                    label = "Client Secret",
                    visualTransformation = if (isClientSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isClientSecretVisible = !isClientSecretVisible }) {
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
            } else {
                OSHInputField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                )
                OSHInputField(
                    value = password,
                    onValueChange = { password = it },
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            OSHButton(
                onClick = { saveProfile() },
                text = stringResource(if (isEdit) R.string.title_edit_server else R.string.btn_add_server),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ServerFormScreenPreview() {
    OSHTheme {
        ServerFormScreen(
            onBackClick = {},
        )
    }
}
