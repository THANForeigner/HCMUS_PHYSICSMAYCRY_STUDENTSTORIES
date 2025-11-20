package com.example.afinal.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.afinal.SensorViewModel

@Composable
fun BarometerScreen() {
    val sensorViewModel: SensorViewModel = viewModel()
    val pressure by sensorViewModel.pressure.observeAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        pressure?.let {
            Text(text = "Pressure: $it hPa")
        }
    }
}
