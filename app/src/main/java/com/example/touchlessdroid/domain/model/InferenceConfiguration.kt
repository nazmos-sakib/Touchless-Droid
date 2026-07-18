package com.example.touchlessdroid.domain.model

import com.example.touchlessdroid.utils.DelegateOption
import com.example.touchlessdroid.utils.PrecisionOption
import com.example.touchlessdroid.utils.RuntimeOption

data class InferenceConfiguration(
    val runtime: RuntimeOption,
    val delegate: DelegateOption,
    val precision: PrecisionOption
)