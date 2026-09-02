package ca.northstarappworks.cleaning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ca.northstarappworks.cleaning.ui.OurHomeApp
import ca.northstarappworks.cleaning.ui.OurHomeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OurHomeTheme { OurHomeApp() } }
    }
}
