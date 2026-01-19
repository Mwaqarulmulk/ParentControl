package com.myparentalcontrol.parent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.myparentalcontrol.parent.ui.navigation.ParentNavHost
import com.myparentalcontrol.parent.ui.theme.ParentalControlTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Parent app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ParentalControlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ParentNavHost()
                }
            }
        }
    }
}
