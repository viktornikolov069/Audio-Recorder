package com.example.audiorecorder.utils

interface ListPositioner {

    val recyclerScrollKey: String

    fun loadListPosition()

    fun saveListPosition()

    fun resetListPosition()
}