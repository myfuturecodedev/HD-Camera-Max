package com.futurecode.hdcameramax.model

data class Language(
        val name: String,
        val code: String,
        val isDefault: Boolean = false,
        var isSelected: Boolean = false
    )