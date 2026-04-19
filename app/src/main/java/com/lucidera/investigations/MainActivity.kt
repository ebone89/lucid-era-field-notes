package com.lucidera.investigations

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lucidera.investigations.data.AppContainer
import com.lucidera.investigations.ui.LucidEraApp
import com.lucidera.investigations.ui.theme.LucidEraTheme

class MainActivity : ComponentActivity() {

    private val container by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LucidEraTheme {
                LucidEraApp(container = container)
            }
        }
    }
}
