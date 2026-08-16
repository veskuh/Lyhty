package net.veskuh.lyhty.util

data class LyhtyError(
    val code: String,
    val title: String,
    val explanation: String,
    val actionableHint: String,
    val technicalDetails: String? = null
) {
    val displayMessage: String
        get() = "[$code] $title: $explanation\n👉 $actionableHint"
}
