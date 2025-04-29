//
//  ContentView.swift
//  CounterIOSApp
//
//  Created by Yiğit Özgümüş on 2025-04-13.
//

import SwiftUI
import Counter

struct ComposeView: UIViewControllerRepresentable {
    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {
    }
    
    func makeUIViewController(context: Context) -> some UIViewController {
        MainViewControllerKt.MainViewController()
    }
}


struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea(.keyboard)
    }
}

#Preview {
    ContentView()
}
