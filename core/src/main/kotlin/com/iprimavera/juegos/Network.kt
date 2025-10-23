package com.iprimavera.juegos

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.FitViewport
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.app.clearScreen
import kotlin.concurrent.thread
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

class ConnectionScreen(
    private val game: KtxGame<KtxScreen>,
    private val onConnected: (session: NetworkSession, isHost: Boolean) -> Unit,
) : KtxScreen {

    private val title: String = "Conexion LAN"
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(800f, 480f, camera)
    private val stage = Stage(viewport)
    private val font = BitmapFont()
    private val skin = Skin(Gdx.files.internal("uiskin.json"))

    private var statusLabel: Label
    private var ipLabel: Label

    private var ips: List<String> = listLocalIPv4()
    private var connecting = false

    init {
        Gdx.input.inputProcessor = stage

        val table = Table()
        table.setFillParent(true)
        stage.addActor(table)

        val titleLabel = Label(title, skin, "default")
        titleLabel.setFontScale(1.3f)

        ipLabel = Label("IP Local: ${ips.joinToString(", ")}", skin)
        statusLabel = Label("Selecciona si quieres ser Host o Cliente", skin)

        val hostButton = TextButton("CREAR PARTIDA", skin)
        val clientButton = TextButton("UNIRSE A PARTIDA", skin)

        hostButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                startAsHost()
            }
        })

        clientButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                autoConnectToHost()
            }
        })

        table.add(titleLabel).padBottom(30f).row()
        table.add(ipLabel).padBottom(10f).row()
        table.add(statusLabel).padBottom(25f).row()
        table.add(hostButton).width(250f).height(60f).pad(10f)
        table.add(clientButton).width(250f).height(60f).pad(10f).row()
    }

    override fun render(delta: Float) {
        clearScreen(0.05f, 0.07f, 0.1f)
        stage.act(delta)
        stage.draw()
    }

    private fun autoConnectToHost() {
        if (connecting) return
        connecting = true

        val baseIp = ips.firstOrNull()?.substringBeforeLast('.') ?: return
        statusLabel.setText("Buscando host...")

        thread {
            for (i in 1..254) {
                val testIp = "$baseIp.$i"
                try {
                    val socket = Socket()
                    socket.connect(java.net.InetSocketAddress(testIp, NetworkSession.PORT), 200)
                    val session = NetworkSession(socket)
                    session.startReading()
                    Gdx.app.postRunnable {
                        statusLabel.setText("Conectado a $testIp")
                        onConnected(session, false)
                    }
                    return@thread
                } catch (_: Exception) {
                    // no host
                }
            }
            Gdx.app.postRunnable {
                statusLabel.setText("No se encontró ningún host.")
                connecting = false
            }
        }
    }

    private fun startAsHost() {
        connecting = true
        statusLabel.setText("Esperando cliente...")
        NetworkSession.startServer(onConnected = { sess ->
            Gdx.app.postRunnable {
                statusLabel.setText("Cliente conectado: ${sess.remoteAddress}")
                onConnected(sess, true)
            }
        }, onError = { e ->
            Gdx.app.postRunnable {
                statusLabel.setText("Error servidor: ${e.message}")
                connecting = false
            }
        })
    }

    private fun listLocalIPv4(): List<String> = try {
        val result = mutableListOf<String>()
        val ifaces = NetworkInterface.getNetworkInterfaces()
        for (ni in ifaces) {
            if (!ni.isUp || ni.isLoopback) continue
            val addrs = ni.inetAddresses
            for (ia in addrs) if (ia is Inet4Address && !ia.isLoopbackAddress && ia.isSiteLocalAddress) {
                result += ia.hostAddress
            }
        }
        if (result.isEmpty()) listOf("0.0.0.0") else result
    } catch (_: Exception) { listOf("0.0.0.0") }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        font.dispose()
    }
}

class NetworkSession(private val socket: Socket) {
    private val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    private val writer = PrintWriter(socket.getOutputStream(), true)

    @Volatile var running = true
    var onMessage: ((String) -> Unit)? = null

    val remoteAddress: String get() = socket.inetAddress.hostAddress

    fun startReading() {
        thread(name = "net-reader") {
            try {
                while (running) {
                    val line = reader.readLine() ?: break
                    onMessage?.invoke(line)
                }
            } catch (_: Exception) {
            } finally { close() }
        }
    }

    fun send(msg: String) = writer.println(msg)

    fun close() {
        running = false
        try { reader.close() } catch (_: Exception) {}
        try { writer.close() } catch (_: Exception) {}
        try { socket.close() } catch (_: Exception) {}
    }

    companion object {
        const val PORT = 5874

        fun startServer(onConnected: (NetworkSession) -> Unit, onError: (Exception) -> Unit) = thread(name = "net-server") {
            var server: ServerSocket? = null
            try {
                server = ServerSocket(PORT)
                val client = server.accept()
                val session = NetworkSession(client)
                session.startReading()
                onConnected(session)
            } catch (e: Exception) {
                onError(e)
            } finally { try { server?.close() } catch (_: Exception) {} }
        }

//        fun connectTo(ip: String, onConnected: (NetworkSession) -> Unit, onError: (Exception) -> Unit) = thread(name = "net-client") {
//            try {
//                val socket = Socket(ip, PORT)
//                val session = NetworkSession(socket)
//                session.startReading()
//                onConnected(session)
//            } catch (e: Exception) { onError(e) }
//        }
    }
}
