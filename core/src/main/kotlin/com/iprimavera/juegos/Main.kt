package com.iprimavera.juegos

import com.iprimavera.juegos.sushiGame.SushiGame
import ktx.app.KtxGame
import ktx.app.KtxScreen

class Main : KtxGame<KtxScreen>() {
    override fun create() {
        addScreen(ConnectionScreen(this, { session, isHost ->
            addScreen(SushiGame(this, session, isHost))
            setScreen<SushiGame>()
        }))

        setScreen<ConnectionScreen>()
    }
}
