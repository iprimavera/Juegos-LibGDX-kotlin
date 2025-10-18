package com.iprimavera.juegos.sushiGame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Timer
import com.badlogic.gdx.utils.viewport.FitViewport
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.app.clearScreen

class FinPartida(
    private val game: KtxGame<KtxScreen>,
    private val resultado: Boolean, // true = ganar, false = perder
    private val puntosJugador: Int,
    private val puntosEnemigo: Int,
    private val siguientePantalla: () -> Unit, // lo que se ejecuta al volver
    private val segundosEspera: Float = 3f      // segundos antes de volver
) : KtxScreen {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(800f, 480f, camera)
    private val stage = Stage(viewport)
    private val skin = Skin(Gdx.files.internal("uiskin.json"))

    private var tiempo = 0f

    override fun show() {
        Gdx.input.inputProcessor = stage

        val tituloTexto = if (resultado) "¡HAS GANADO!" else "HAS PERDIDO"
        val colorTitulo = if (resultado) Color.GREEN else Color.RED

        val tituloLabel = Label(tituloTexto, skin, "default").apply {
            setFontScale(2.5f)
            color = colorTitulo
        }

        val puntuacionLabel = Label("Tu puntuacion: $puntosJugador", skin).apply {
            setFontScale(1.5f)
            color = Color.WHITE
        }

        val enemigoLabel = Label("Puntuacion del enemigo: $puntosEnemigo", skin).apply {
            setFontScale(1.5f)
            color = Color.LIGHT_GRAY
        }

        val tabla = Table()
        tabla.setFillParent(true)
        tabla.defaults().pad(20f)
        tabla.add(tituloLabel).row()
        tabla.add(puntuacionLabel).row()
        tabla.add(enemigoLabel).row()
        stage.addActor(tabla)

        // ⏰ Programar el cambio automático
        Timer.schedule(object : Timer.Task() {
            override fun run() {
                siguientePantalla()
            }
        }, segundosEspera)
    }

    override fun render(delta: Float) {
        clearScreen(0.05f, 0.07f, 0.1f)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
    }

    override fun hide() {
        super.hide()
        game.removeScreen<FinPartida>()
        dispose()
    }

}
