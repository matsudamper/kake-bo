package net.matsudamper.money.frontend.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

@Composable
public expect fun rememberCustomFontFamily(): FontFamily

@Composable
public expect fun rememberFontFamilyResolver(): FontFamily.Resolver
