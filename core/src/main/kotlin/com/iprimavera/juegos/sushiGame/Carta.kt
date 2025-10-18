package com.iprimavera.juegos.sushiGame

import com.badlogic.gdx.graphics.Texture
import kotlinx.serialization.Serializable

@Serializable
enum class Carta(val texturaPath: String) {
    SALMON_NIGIRI("cartas/salmon.png"), // 2
    SQUID_NIGIRI("cartas/squid.png"), // 3
    EGG_NIGIRI("cartas/egg.png"), // 1
    WASABI("cartas/wasabi.png"), // SIGUIENTE NIGIRI X3
    MAKI_ROLL_1("cartas/maki1.png"), // EL QUE MAS 6 EL OTRO 3
    MAKI_ROLL_2("cartas/maki2.png"), // EL QUE MAS 6 EL OTRO 3
    MAKI_ROLL_3("cartas/maki3.png"), // EL QUE MAS 6 EL OTRO 3
    SASHIMI("cartas/sashimi.png"), // TENER 3 ES 10
    TEMPURA("cartas/tempura.png"), // TENER 2 ES 5
    DUMPLING("cartas/dumpling.png"), // 1 3 6 10 15
    PUDDING("cartas/pudding.png"); // ELL QUE MAS TENGA 6 EL QUE MENOS -6

    private var _textura: Texture? = null
    val textura: Texture
        get() {
            if (_textura == null) {
                _textura = Texture(texturaPath)
            }
            return _textura!!
        }

    companion object {
        fun disposeAll() {
            entries.forEach { carta ->
                carta._textura?.dispose()
                carta._textura = null
            }
        }
    }
}
