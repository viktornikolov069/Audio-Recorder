package com.example.audiorecorder.adapter

/* This interface connects the onClick functionality inside the rv adapter with GalleryActivity.kt.
   The idea is to provide a clean way of using intents by writing them in the GalleryActivity.kt
   and not in the adapter as that is not something it should be responsible for. */

interface OnItemClickListener {
    fun onItemClickListener(position: Int)
    fun onItemLongClickListener(position: Int)
}