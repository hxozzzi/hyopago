package com.hxozzi.hyopago

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.hxozzi.hyopago.MainScreen
import com.hxozzi.hyopago.ui.theme.HyopagoTheme

class MEMOActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HyopagoTheme {
                MEMOScreen()
            }
        }
    }
}

@Composable
fun MEMOScreen(){

}