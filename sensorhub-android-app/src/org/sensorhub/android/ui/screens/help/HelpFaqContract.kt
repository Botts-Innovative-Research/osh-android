package org.sensorhub.android.ui.screens.help

sealed class FaqItem {
    data class Header(val title: String) : FaqItem()
    data class Entry(val question: String, val answer: String) : FaqItem()
}