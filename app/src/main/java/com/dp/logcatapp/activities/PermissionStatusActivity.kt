package com.dp.logcatapp.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.dp.logcatapp.ui.screens.PermissionStatusScreen
import com.dp.logcatapp.ui.theme.LogcatReaderTheme

class PermissionStatusActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LogcatReaderTheme {
                PermissionStatusScreen(
                    modifier = Modifier.fillMaxSize(),
                    onBack = { finish() },
                )
            }
        }
    }
}
