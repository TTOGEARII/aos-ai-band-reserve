package com.kanenashitogeari.bandroom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kanenashitogeari.bandroom.ui.BandRoomScreen
import com.kanenashitogeari.bandroom.ui.theme.Band_roomTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Band_roomTheme {
                BandRoomScreen()
            }
        }
    }
}
