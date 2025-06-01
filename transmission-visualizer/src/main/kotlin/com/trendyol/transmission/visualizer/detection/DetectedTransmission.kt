package com.trendyol.transmission.visualizer.detection

data class DetectedTransmission(
    val name: String,
    val lineNumber: Int,
    val type: TransmissionType,
)
