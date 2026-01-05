package com.hermanns.hermannsgerenciador.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hermanns.hermannsgerenciador.ui.navigation.AppWithDrawer
import com.hermanns.hermannsgerenciador.ui.theme.HermannsGerenciadorTheme
import com.hermanns.hermannsgerenciador.util.NotificationHelper

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createChannel(this)

        setContent {
            HermannsGerenciadorTheme {
                AppWithDrawer()
            }
        }
    }
}