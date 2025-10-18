package com.iprimavera.juegos.sushiGame

class TurnoManager {

    fun usarCartas(jugador: Jugador, index: Int) {
        jugador.mesa.addCarta(jugador.mano.cartas.removeAt(index))
    }

    fun swapManos(usuario: Jugador, enemigo: Jugador) {
        val usuarioCartas = usuario.mano.cartas.toMutableList()
        val enemigoCartas = enemigo.mano.cartas.toMutableList()

        usuario.mano.cartas.clear()
        usuario.mano.cartas.addAll(enemigoCartas)

        enemigo.mano.cartas.clear()
        enemigo.mano.cartas.addAll(usuarioCartas)
    }

}
