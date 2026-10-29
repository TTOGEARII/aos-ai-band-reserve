package com.enezzang.yatte

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.enezzang.yatte.ui.EnezzangYatteScreen
import com.enezzang.yatte.ui.theme.Enezzang_yatteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Enezzang_yatteTheme {
                EnezzangYatteScreen()
            }
        }
    }
}
