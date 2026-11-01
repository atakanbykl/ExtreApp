package com.example.extreapp.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Nightlife
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object AppleFluidMotion {
    /**
     * Critically damped spring (dampingRatio = 1.0, response ~ 0.35s)
     * Default for UI transitions without overshoot.
     */
    fun <T> criticallyDampedSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 380f
    )

    /**
     * Momentum-aware spring (dampingRatio = 0.8, response ~ 0.35s)
     * For gesture-driven sheets, cards, and interactive chart reveals.
     */
    fun <T> momentumSpring() = spring<T>(
        dampingRatio = 0.8f,
        stiffness = 340f
    )
}

/**
 * Immediate input-down scale feedback (0.97f on touch-down) per Apple Design §1.
 */
fun Modifier.applePressScale(
    enabled: Boolean = true,
    activeScale: Float = 0.97f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) activeScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 650f
        ),
        label = "applePressScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

object Formatters {
    private val turkishLocale = Locale.forLanguageTag("tr-TR")
    private val currencySymbols = DecimalFormatSymbols(turkishLocale).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val currencyFormat = DecimalFormat("#,##0.00", currencySymbols)
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", turkishLocale)

    private val turkishMonthNames = arrayOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    )

    fun formatCurrency(
        amount: Double,
        currency: String = "TRY",
        showSign: Boolean = false,
        isMasked: Boolean = false
    ): String {
        if (isMasked) return "***"
        val symbol = when (currency.uppercase(Locale.ROOT)) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "TL"
        }
        val formatted = currencyFormat.format(kotlin.math.abs(amount))
        val prefix = when {
            showSign && amount < -0.001 -> "-"
            showSign && amount > 0.001 -> "+"
            amount < -0.001 -> "-"
            else -> ""
        }
        return "$prefix$formatted $symbol"
    }

    fun formatEpochDay(epochDay: Long): String {
        return runCatching {
            LocalDate.ofEpochDay(epochDay).format(dateFormatter)
        }.getOrDefault("")
    }

    fun formatYearMonthDisplay(yearMonth: String): String {
        return runCatching {
            val ym = YearMonth.parse(yearMonth)
            "${turkishMonthNames[ym.monthValue - 1]} ${ym.year}"
        }.getOrDefault(yearMonth)
    }

    fun formatYearMonthShort(yearMonth: String): String {
        return runCatching {
            val ym = YearMonth.parse(yearMonth)
            "${turkishMonthNames[ym.monthValue - 1].take(3)} '${ym.year.toString().takeLast(2)}"
        }.getOrDefault(yearMonth)
    }
}

@Composable
fun resolveCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "ShoppingCart" -> Icons.Outlined.ShoppingCart
        "Restaurant" -> Icons.Outlined.Restaurant
        "LocalGasStation" -> Icons.Outlined.LocalGasStation
        "ShoppingBag" -> Icons.Outlined.ShoppingBag
        "Flight" -> Icons.Outlined.Flight
        "Subscriptions" -> Icons.Outlined.Subscriptions
        "Home" -> Icons.Outlined.Home
        "FitnessCenter" -> Icons.Outlined.FitnessCenter
        "Nightlife" -> Icons.Outlined.Nightlife
        "ReceiptLong" -> Icons.AutoMirrored.Outlined.ReceiptLong
        "HelpOutline" -> Icons.AutoMirrored.Outlined.HelpOutline
        else -> Icons.Outlined.Category
    }
}

fun Color.Companion.fromHex(hex: Long): Color = Color(hex)

