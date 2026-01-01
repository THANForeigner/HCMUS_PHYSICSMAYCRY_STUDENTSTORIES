package com.example.afinal.data.model

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.example.afinal.data.LocationData

class LocationViewModel: ViewModel(){
    private val _location = mutableStateOf<LocationData?>(null)
    val location: State<LocationData?> = _location
    fun updateLocation(newLocation: LocationData) {
        _location.value = newLocation
    }
}