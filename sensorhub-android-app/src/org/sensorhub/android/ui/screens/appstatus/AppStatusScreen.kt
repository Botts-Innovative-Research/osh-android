package org.sensorhub.android.ui.screens.appstatus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sensorhub.android.R
import org.sensorhub.android.ui.components.OSHCard
import org.sensorhub.android.ui.components.OSHStatusRow
import org.sensorhub.android.ui.components.OSHTopAppBarWithBack
import org.sensorhub.android.ui.theme.Background
import org.sensorhub.android.ui.theme.OSHTheme

@Composable
fun AppStatusScreen(
    onBackClick: () -> Unit,
    viewModel: AppStatusViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            OSHTopAppBarWithBack(
                title = stringResource(R.string.app_status_main_fragment),
                onBackClick = onBackClick
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {

            OSHCard {
                OSHStatusRow(
                    title = stringResource(R.string.httpServerStatusLabel),
                    subtitle = state.httpStatus
                )
                HorizontalDivider(

                )
                OSHStatusRow(
                    title = stringResource(R.string.sosServiceStatusLabel),
                    subtitle = state.sosStatus
                )
                HorizontalDivider(

                )
                OSHStatusRow(
                    title = stringResource(R.string.conSysServiceStatusLabel),
                    subtitle = state.conSysStatus
                )
                HorizontalDivider(

                )
                OSHStatusRow(
                    title = stringResource(R.string.discoveryServiceStatusLabel),
                    subtitle = state.discoveryStatus
                )
                HorizontalDivider(

                )
                OSHStatusRow(
                    title = stringResource(R.string.sensorServiceStatusLabel),
                    subtitle = state.sensorStatus
                )
                HorizontalDivider(

                )
                OSHStatusRow(
                    title = stringResource(R.string.storageServiceStatusLabel),
                    subtitle = state.storageStatus
                )
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppStatusScreenPreview() {
    OSHTheme {
        AppStatusScreen(
            onBackClick = {}
        )
    }
}