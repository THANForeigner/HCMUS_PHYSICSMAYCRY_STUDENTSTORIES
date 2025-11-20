package com.example.afinal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SensorViewModel(application: Application) : AndroidViewModel(application) {
    private val barometer = Barometer(application)

    private val _pressure = MutableLiveData<Float>()
    val pressure: LiveData<Float> = _pressure

    init {
        barometer.observePressure()
            .onEach { pressureValue ->
                _pressure.value = pressureValue
            }
            .catch { e ->
                // Handle error
            }
            .launchIn(viewModelScope)
    }
}