package com.yigitozgumus.transmissiontesting

import com.trendyol.transmission.Transmission

inline fun<reified T: Transmission.Data?> List<Transmission.Data>.lastInstanceOf(): T? {
    return this.filterIsInstance<T>().last()
}

inline fun<reified T: Transmission.Effect?> List<Transmission.Effect>.lastInstanceOf(): T? {
    return this.filterIsInstance<T>().last()
}
