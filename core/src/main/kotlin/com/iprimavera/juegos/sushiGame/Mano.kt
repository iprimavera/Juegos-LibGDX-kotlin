package com.iprimavera.juegos.sushiGame

class Mano {

    var cartas: MutableList<Carta> = mutableListOf()

    fun tieneCartas(): Boolean {
        return cartas.isNotEmpty()
    }

    fun addCarta(carta: Carta) {
        cartas.add(carta)
    }
}
