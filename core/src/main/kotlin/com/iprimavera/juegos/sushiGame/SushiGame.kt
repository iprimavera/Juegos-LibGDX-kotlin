package com.iprimavera.juegos.sushiGame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.iprimavera.juegos.NetworkSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ktx.actors.onClick
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.scene2d.*

data class Jugador(var mano: Mano, val mesa: Mesa)

class SushiGame(
    private val game: KtxGame<KtxScreen>,
    private val session: NetworkSession,
    private val isHost: Boolean
) : KtxScreen {

    private lateinit var usuario: Jugador
    private lateinit var enemigo: Jugador

    private lateinit var stage: Stage

    private val turnoManager = TurnoManager()
    private var usuarioElegida: Int? = null
    private var enemigoElegida: Int? = null

    private var partidaEmpezada = false

    @Serializable
    data class DatosIniciales(
        val cartasJugador: MutableList<Carta>,
        val cartasEnemigo: MutableList<Carta>
    )

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage
        Scene2DSkin.defaultSkin = Skin(Gdx.files.internal("uiskin.json"))

        usuario = Jugador(Mano(), Mesa())
        enemigo = Jugador(Mano(), Mesa())

        partidaEmpezada = false

        if (isHost) {
            val deck = Deck()
            // repartir cartas a jugadores
            repeat(10) {
                deck.darCarta(usuario.mano)
                deck.darCarta(enemigo.mano)
            }

            val datos = DatosIniciales(
                cartasJugador = usuario.mano.cartas,
                cartasEnemigo = enemigo.mano.cartas
            )
            val json = Json.encodeToString(datos)
            session.send(json)
            partidaEmpezada = true
        }

        session.onMessage = { msg ->
            val numero = msg.toIntOrNull()

            if (numero != null) {
                // 🟢 Es un número
                Gdx.app.postRunnable {
                    println("Número recibido: $numero")
                    enemigoElegida = numero
                    // Aquí actualizas tu lógica con el número recibido
                }
            } else if (!isHost) {
                // 🔵 Intentar decodificar JSON de las cartas
                try {
                    val datos = Json.decodeFromString<DatosIniciales>(msg)
                    Gdx.app.postRunnable {
                        println("Cartas recibidas del servidor:")
                        println("Jugador: ${datos.cartasJugador}")
                        println("Enemigo: ${datos.cartasEnemigo}")
                        // Aquí puedes asignarlas a tus variables locales
                        usuario.mano.cartas = datos.cartasEnemigo
                        enemigo.mano.cartas = datos.cartasJugador
                        actualizarBotones(stage.actors.find { it is Table } as Table)
                        partidaEmpezada = true
                    }
                } catch (e: Exception) {
                    println("Mensaje desconocido o no válido: $msg")
                }
            }
        }

        stage.actors {
            table {
                setFillParent(true)
                center()
                pad(20f)

                actualizarBotones(this)
            }
        }

    }

    private fun actualizarBotones(tabla: Table) {
        tabla.clearChildren()

        val anchoPantalla = stage.viewport.worldWidth
        val anchoCarta = anchoPantalla / 10f - 13

        // --- 🔴 Línea 1: Cartas del enemigo (giradas 180°) ---
        val filaEnemigo = Table()
        filaEnemigo.isTransform = true
        enemigo.mesa.cartas.forEach { carta ->
            val drawable = TextureRegionDrawable(TextureRegion(carta.textura))
            val boton = ImageButton(drawable)
            filaEnemigo.add(boton).size(anchoCarta).pad(5f)
        }

        // --- 🟡 Línea 2: Cartas de la mesa (por ejemplo, descartes o visibles) ---
        val filaMesa = Table()
        usuario.mesa.cartas.forEach { carta ->
            val drawable = TextureRegionDrawable(TextureRegion(carta.textura))
            val boton = ImageButton(drawable)
            filaMesa.add(boton).size(anchoCarta).pad(5f)
        }

        // --- 🟢 Línea 3: Cartas del jugador (clicables) ---
        val filaJugador = Table()
        usuario.mano.cartas.forEachIndexed { index, carta ->
            val drawable = TextureRegionDrawable(TextureRegion(carta.textura))
            val boton = ImageButton(drawable)

            boton.onClick {
                usuarioElegida = index
                session.send(index.toString())
                actualizarBotones(tabla)
            }

            filaJugador.add(boton).size(anchoCarta).pad(5f)
        }

        // Añadir las 3 filas en orden a la tabla principal
        tabla.add(filaEnemigo).row()
        tabla.add(filaMesa).row()
        tabla.add(filaJugador).row()
    }


    override fun render(delta: Float) {
        stage.act(delta)
        stage.draw()

        val usel = usuarioElegida
        val enel = enemigoElegida
        if (usel != null && enel != null) {

            turnoManager.usarCartas(usuario, usel)
            turnoManager.usarCartas(enemigo, enel)

            usuarioElegida = null
            enemigoElegida = null

            turnoManager.swapManos(usuario,enemigo)

            actualizarBotones(stage.actors.find { it is Table } as Table)

        }

        if (!usuario.mano.tieneCartas() && !enemigo.mano.tieneCartas() && partidaEmpezada) {
            val uspuntos = usuario.mesa.contarPuntos(enemigo.mesa)
            val enpuntos = enemigo.mesa.contarPuntos(usuario.mesa)
            println("Tu puntuacion: $uspuntos")
            println("La puntuacion de tu enemigo: $enpuntos")

            if (uspuntos > enpuntos) {
                println("Has ganado!")
            } else if (uspuntos < enpuntos) {
                println("Has perdido :(")
            } else {
                println("Empate!")
            }
            game.addScreen(
                FinPartida(
                    game = game,
                    resultado = uspuntos > enpuntos,
                    puntosJugador = uspuntos,
                    puntosEnemigo = enpuntos,
                    siguientePantalla = { game.setScreen<SushiGame>() }, // lo que hará al acabar
                    segundosEspera = 4f // tiempo antes de volver
                )
            )
            game.setScreen<FinPartida>()

        }
    }

    override fun dispose() {
        stage.dispose()
        Carta.disposeAll()
    }
}
