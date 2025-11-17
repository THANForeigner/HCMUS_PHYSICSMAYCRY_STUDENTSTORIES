package com.example.afinal

import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import com.example.afinal.ui.theme.FINALTheme
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(saveInstanceState: Bundle?){
        super.onCreate(saveInstanceState)
        setContent{
            val viewModel : LocationViewModel = viewModel()
            FINALTheme{
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ){
                    MyLocationApp(viewModel)
                }
            }
        }

    }
}

