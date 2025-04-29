//
//  CounterIOSAppApp.swift
//  CounterIOSApp
//
//  Created by Yiğit Özgümüş on 2025-04-13.
//

import SwiftUI
import Counter

@main
struct CounterIOSAppApp: App {
    
    init() {
        HelperKt.doInitKoin()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
