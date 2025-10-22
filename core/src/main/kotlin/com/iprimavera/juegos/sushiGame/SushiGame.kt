package com.iprimavera.juegos.sushiGame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.iprimavera.juegos.NetworkSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ktx.actors.onClick
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.scene2d.*
import ktx.assets.*
import ktx.tiled.*

data class Jugador(var mano: Mano, val mesa: Mesa)

class SushiGame(
    private val game: KtxGame<KtxScreen>,
    private val session: NetworkSession,
    private val isHost: Boolean
) : KtxScreen {

    private lateinit var usuario: Jugador
    private lateinit var enemigo: Jugador
    private var usuarioElegida: Int? = null
    private var enemigoElegida: Int? = null

    private lateinit var stage: Stage

    private val turnoManager = TurnoManager()

    private var partidaEmpezada = false

    private val assets = AssetManager().apply {
        setLoader(TiledMap::class.java, TmxMapLoader())
        load("mapas/SushiGo.tmx", TiledMap::class.java)
        finishLoading()
    }

    private var map: TiledMap = assets.get("mapas/SushiGo.tmx", TiledMap::class.java)
    private val capaObjetos = map.layers.get("zonas")

    @Serializable
    data class DatosIniciales(
        val cartasJugador: MutableList<Carta>,
        val cartasEnemigo: MutableList<Carta>
    )

    override fun show() {
        val viewport = FitViewport(1280f, 720f)
        stage = Stage(viewport)
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
                Gdx.app.postRunnable {
                    println("Número recibido: $numero")
                    enemigoElegida = numero
                }
            } else if (!isHost) {
                try {
                    val datos = Json.decodeFromString<DatosIniciales>(msg)
                    Gdx.app.postRunnable {
                        println("Cartas recibidas del servidor:")
                        println("Jugador: ${datos.cartasJugador}")
                        println("Enemigo: ${datos.cartasEnemigo}")
                        usuario.mano.cartas = datos.cartasEnemigo
                        enemigo.mano.cartas = datos.cartasJugador
                        actualizarBotones()
                        partidaEmpezada = true
                    }
                } catch (e: Exception) {
                    println("Mensaje desconocido o no válido: $msg")
                }
            }
        }

        actualizarBotones()

    }

    private fun actualizarBotones() {

        stage.clear()

//        enemigo.mesa.cartas.forEachIndexed { index, carta ->
//            val objeto = capaObjetos.objects.get("enemigo")
//            //val posicion =
//
//            val drawable = TextureRegionDrawable(TextureRegion(carta.textura))
//            val boton = ImageButton(drawable)
//            boton.setPosition(1f,objeto.y)
//            boton.setScale(2f)
//            stage.addActor(boton)
//        }
//
//        usuario.mesa.cartas.forEach { carta ->
//            val drawable = TextureRegionDrawable(TextureRegion(carta.textura))
//            val boton = ImageButton(drawable)
//            filaMesa.add(boton).size(anchoCarta).pad(2f).padTop(10f)
//        }

        fun clickable(boton: ImageButton, index: Int) {
            if (usuarioElegida == index) {
                boton.isTransform = true
                boton.setOrigin(boton.width/2f,boton.height/2f)
                boton.setScale(1.2f)
            } else {
                boton.setScale(1f)
            }

            boton.onClick {
                usuarioElegida = index
                session.send(index.toString())
                actualizarBotones()
            }
        }

        val elegir = capaObjetos.objects.get("elegir")
        val maxAncho = elegir.width
        val espacio = 40f
        val totalOriginal = usuario.mano.cartas.sumOf { it.textura.width.toDouble() }.toFloat() + espacio * (usuario.mano.cartas.size - 1)
        val escala: Float = maxAncho / totalOriginal

        usuario.mano.cartas.forEachIndexed { index, carta ->
            val drawable = TextureRegionDrawable(TextureRegion(carta.textura))
            val boton = ImageButton(drawable)

            boton.setPosition(elegir.x+elegir.width/usuario.mano.cartas.count()*index,elegir.y)
            boton.setSize(carta.textura.width.toFloat()*escala,elegir.height)

            clickable(boton,index)

            stage.addActor(boton)
        }

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

            actualizarBotones()

        }

        if (!usuario.mano.tieneCartas() && !enemigo.mano.tieneCartas() && partidaEmpezada) {
            ganar()
        }
    }

    private fun ganar() {
        val uspuntos = usuario.mesa.contarPuntos(enemigo.mesa)
        val enpuntos = enemigo.mesa.contarPuntos(usuario.mesa)

        game.addScreen(
            FinPartida(
                game = game,
                resultado = uspuntos > enpuntos,
                puntosJugador = uspuntos,
                puntosEnemigo = enpuntos,
                siguientePantalla = { game.setScreen<SushiGame>() },
                segundosEspera = 4f
            )
        )
        game.setScreen<FinPartida>()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        Carta.disposeAll()
    }
}
