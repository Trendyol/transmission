package com.trendyol.transmissiontest

import com.trendyol.transmission.Transmission

/**
 * Finds the last occurrence of a specific data type in the list.
 * 
 * This extension function is useful for testing scenarios where you need to find the most recent
 * occurrence of a specific data type from a stream of captured data transmissions.
 * 
 * @param T The type of data to search for
 * @receiver List of data transmissions to search through
 * @return The last data transmission of type T, or null if none exists
 * 
 * Example usage:
 * ```kotlin
 * val dataStream = listOf(
 *     UserData.Loading,
 *     UserData.LoggedIn(user),
 *     UserData.ProfileUpdated(newProfile)
 * )
 * val lastUserData = dataStream.lastVersionOf<UserData.LoggedIn>()
 * assertNotNull(lastUserData)
 * ```
 */
inline fun<reified T: Transmission.Data?> List<Transmission.Data>.lastVersionOf(): T? {
    return this.filterIsInstance<T>().takeIf { it.isNotEmpty() }?.last()
}

/**
 * Finds the last occurrence of a specific effect type in the list.
 * 
 * This extension function is useful for testing scenarios where you need to find the most recent
 * occurrence of a specific effect type from a stream of captured effect transmissions.
 * 
 * @param T The type of effect to search for
 * @receiver List of effect transmissions to search through
 * @return The last effect transmission of type T, or null if none exists
 * 
 * Example usage:
 * ```kotlin
 * val effectStream = listOf(
 *     LoadingEffect.Show,
 *     NetworkEffect.Request(url),
 *     LoadingEffect.Hide,
 *     NavigationEffect.Navigate(screen)
 * )
 * val lastLoadingEffect = effectStream.lastVersionOf<LoadingEffect.Hide>()
 * assertNotNull(lastLoadingEffect)
 * ```
 */
inline fun<reified T: Transmission.Effect?> List<Transmission.Effect>.lastVersionOf(): T? {
    return this.filterIsInstance<T>().takeIf { it.isNotEmpty() }?.last()
}
