package edu.algosteps

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import edu.algosteps.data.AppContainer
import edu.algosteps.ui.AlgoStepsTheme

class AlgoStepsApp : Application() {
    val container by lazy { AppContainer(this) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as AlgoStepsApp).container.repository
        setContent {
            AlgoStepsTheme { AlgoNavigation(repository) }
        }
    }
}
