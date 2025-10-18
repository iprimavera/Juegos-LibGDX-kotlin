package com.iprimavera.juegos.sushiGame

class Deck {

    private var cartas: MutableList<Carta> = mutableListOf()

    init {
        repeat(14) {cartas.add(Carta.TEMPURA)}
        repeat(14) {cartas.add(Carta.SASHIMI)}
        repeat(14) {cartas.add(Carta.DUMPLING)}
        repeat(10) {cartas.add(Carta.SALMON_NIGIRI)}
        repeat(5) {cartas.add(Carta.SQUID_NIGIRI)}
        repeat(5) {cartas.add(Carta.EGG_NIGIRI)}
        repeat(10) {cartas.add(Carta.PUDDING)}
        repeat(6) {cartas.add(Carta.WASABI)}
        repeat(6) {cartas.add(Carta.MAKI_ROLL_1)}
        repeat(12) {cartas.add(Carta.MAKI_ROLL_2)}
        repeat(8) {cartas.add(Carta.MAKI_ROLL_3)}

        cartas.shuffle()
    }

    fun darCarta(mano: Mano) {
        mano.addCarta(cartas.removeAt(0))
    }
}
