//
//  ChatTopPanelMinHeight.swift
//  YoPt
//
//  Created by Dolin Sergey on 3. 6. 2026..
//
import SwiftUI

struct ChatTopPanelMinHeight: PreferenceKey {

    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value += nextValue()
    }

}
