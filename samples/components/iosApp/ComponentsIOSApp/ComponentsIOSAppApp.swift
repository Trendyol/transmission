//
//  ComponentsIOSAppApp.swift
//  ComponentsIOSApp
//
//  Created by Yiğit Özgümüş on 2025-04-13.
//

import SwiftUI
import Components

@main
struct ComponentsIOSAppApp: App {
    init() {
        HelperKt.doInitKoin()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
