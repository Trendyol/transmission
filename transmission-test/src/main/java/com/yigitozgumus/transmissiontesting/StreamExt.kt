package com.yigitozgumus.transmissiontesting

import com.trendyol.transmission.Transmission

inline fun<reified T: Transmission.Data?> List<Transmission.Data>.lastVersionOf(): T? {
    return this.filterIsInstance<T>().takeIf { it.isNotEmpty() }?.last()
}

inline fun<reified T: Transmission.Effect?> List<Transmission.Effect>.lastVersionOf(): T? {
    return this.filterIsInstance<T>().takeIf { it.isNotEmpty() }?.last()
}
