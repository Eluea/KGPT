/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt

import org.junit.Test
import io.github.libxposed.api.XposedInterface

class ExampleUnitTest {
    @Test
    fun checkChainMethods() {
        for (m in XposedInterface.Chain::class.java.methods) {
            println("Chain: ${m.name}(${m.parameterTypes.map { it.simpleName }.joinToString()}) -> ${m.returnType.simpleName}")
        }
    }
}
