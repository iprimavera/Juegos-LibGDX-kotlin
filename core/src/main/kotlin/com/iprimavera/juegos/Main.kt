package com.iprimavera.juegos

import com.iprimavera.juegos.sushiGame.SushiGame
import ktx.app.KtxGame
import ktx.app.KtxScreen

class Main : KtxGame<KtxScreen>() {
    override fun create() {
        // Añade la pantalla de conexión LAN al juego
        addScreen(ConnectionScreen(this, { session, isHost ->
            // Aquí cambias a tu GameScreen cuando se conecten
            addScreen(SushiGame(this, session, isHost))
            setScreen<SushiGame>()
        }))

        // Establece la pantalla inicial
        setScreen<ConnectionScreen>()
    }
}
