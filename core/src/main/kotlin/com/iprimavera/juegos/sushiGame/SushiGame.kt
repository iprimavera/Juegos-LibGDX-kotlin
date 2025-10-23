package com.iprimavera.juegos.sushiGame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.iprimavera.juegos.ConnectionScreen
import com.iprimavera.juegos.NetworkSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ktx.actors.onClick
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use
import ktx.scene2d.*
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
    private var partidaEmpezada = false
    private val shape: ShapeRenderer = ShapeRenderer()

    private lateinit var stage: Stage
    private val turnoManager = TurnoManager()

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

        // barajar cartas, repartir y enviar al cliente
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
                } catch (_: Exception) {
                    println("Mensaje desconocido o no válido: $msg")
                }
            }
        }

        actualizarBotones()

    }

    private fun actualizarBotones() {

        stage.clear()

        fun clickable(boton: ImageButton, index: Int) {
            if (usuarioElegida == index) {
                boton.setScale(1.2f)
            } else boton.setScale(1f)

            boton.onClick {
                usuarioElegida = index
                session.send(index.toString())
                actualizarBotones()
            }
        }

        fun addTabla(objeto: String, paquete: Paquete) {

            val rect = capaObjetos.objects.get(objeto)
            val maxAncho = rect.width
            val espacio = 40f
            val totalOriginal = paquete.cartas.sumOf { it.textura.width.toDouble() }.toFloat() + espacio * (paquete.cartas.size - 1)
            val escala: Float = maxAncho / totalOriginal

            paquete.cartas.forEachIndexed { index, carta ->
                val drawable = TextureRegionDrawable(TextureRegion(carta.textura))
                val boton = ImageButton(drawable)
                boton.isTransform = true

                boton.setPosition(rect.x+rect.width/paquete.cartas.count()*index,rect.y)
                boton.setSize(carta.textura.width.toFloat()*escala,rect.height)

                boton.setOrigin(boton.width/2f,boton.height/2f)
//                if (rect.name == "enemigo") boton.rotateBy(180f)

                if (paquete is Mano) clickable(boton, index)

                stage.addActor(boton)
            }
        }

        addTabla("enemigo", enemigo.mesa)
        addTabla("usuario", usuario.mesa)
        addTabla("elegir", usuario.mano)
    }


    override fun render(delta: Float) {
        clearScreen(0.18f, 0.18f, 0.18f, 1f)

        shape.projectionMatrix = stage.camera.combined
        shape.use(ShapeRenderer.ShapeType.Filled) {
            val arriba = capaObjetos.objects.get("enemigo")
            val abajo = capaObjetos.objects.get("usuario")
            val offset = 20f
            shape.setColor(0.803f, 0.521f, 0.247f, 1f)
            shape.rect(abajo.x-offset,abajo.y-offset,abajo.width+offset*2,arriba.y-abajo.y+arriba.height+offset*2)
        }

        stage.act(delta)
        stage.draw()

        if (!session.running) {
            Gdx.app.postRunnable {
                game.removeScreen<ConnectionScreen>()
                game.removeScreen<SushiGame>()

                game.addScreen(ConnectionScreen(game, { session, isHost ->
                    game.addScreen(SushiGame(game, session, isHost))
                    game.setScreen<SushiGame>()
                }))
                game.setScreen<ConnectionScreen>()
            }
        }

        if (usuarioElegida != null && enemigoElegida != null) {
            turnoManager.usarCartas(usuario, usuarioElegida!!)
            turnoManager.usarCartas(enemigo, enemigoElegida!!)
            usuarioElegida = null
            enemigoElegida = null
            turnoManager.swapManos(usuario, enemigo)
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
        shape.dispose()
    }
}
