package de.projektastra.app.hardware

/**
 * Palette colors for pure OLED true black mode vs standard dark theme.
 */
data class OledThemeColors(
    val backgroundColorHex: String,
    val surfaceColorHex: String,
    val cardBackgroundHex: String,
    val isPureBlack: Boolean
)

/**
 * Manages pure OLED True Black (#000000) styling to ensure complete pixel switch-off
 * and maximal dark adaptation on AMOLED/OLED displays.
 */
object OledThemeManager {
    const val PURE_BLACK_HEX = "#000000"
    const val DEFAULT_DARK_SURFACE_HEX = "#121212"

    fun getThemeColors(oledModeEnabled: Boolean): OledThemeColors {
        return if (oledModeEnabled) {
            OledThemeColors(
                backgroundColorHex = PURE_BLACK_HEX,
                surfaceColorHex = PURE_BLACK_HEX,
                cardBackgroundHex = PURE_BLACK_HEX,
                isPureBlack = true
            )
        } else {
            OledThemeColors(
                backgroundColorHex = DEFAULT_DARK_SURFACE_HEX,
                surfaceColorHex = "#1E1E1E",
                cardBackgroundHex = "#2C2C2C",
                isPureBlack = false
            )
        }
    }
}
