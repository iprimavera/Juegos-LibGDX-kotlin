package com.iprimavera.juegos.sushiGame

interface Paquete {var cartas: MutableList<Carta>}

class Mano: Paquete {

    override var cartas: MutableList<Carta> = mutableListOf()

    fun tieneCartas(): Boolean {
        return cartas.isNotEmpty()
    }

    fun addCarta(carta: Carta) {
        cartas.add(carta)
        cartas.sort()
    }
}
