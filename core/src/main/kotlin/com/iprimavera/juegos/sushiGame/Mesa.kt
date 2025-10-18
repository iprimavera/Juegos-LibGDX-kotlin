package com.iprimavera.juegos.sushiGame

class Mesa {

    var cartas: MutableList<Carta> = mutableListOf()

    fun addCarta(carta: Carta) {
        cartas.add(carta)
    }

    fun contarPuntos(mesaEnemiga: Mesa): Int {
        var puntos = 0

        var wasabi = false
        var makis = 0
        var sashimi = 0
        var tempura = 0
        var dumpling = 0
        var pudding = 0

        for (carta in cartas) {
            when (carta) {
                Carta.EGG_NIGIRI -> {
                    if (wasabi) {
                        wasabi = false
                        puntos += 1*3
                    } else puntos += 1
                }
                Carta.SALMON_NIGIRI -> {
                    if (wasabi) {
                        wasabi = false
                        puntos += 2*3
                    } else puntos += 2
                }
                Carta.SQUID_NIGIRI -> {
                    if (wasabi) {
                        wasabi = false
                        puntos += 3*3
                    } else puntos += 3
                }
                Carta.WASABI -> wasabi = true
                Carta.MAKI_ROLL_1 -> makis += 1
                Carta.MAKI_ROLL_2 -> makis += 2
                Carta.MAKI_ROLL_3 -> makis += 3
                Carta.SASHIMI -> sashimi += 1
                Carta.TEMPURA -> tempura += 1
                Carta.DUMPLING -> dumpling += 1
                Carta.PUDDING -> pudding += 1
            }
        }

        if (makis > mesaEnemiga.getMakis()) {
            puntos += 6
        } else if (makis > 0) puntos += 3

        for (i in 1..sashimi/3) { puntos += 10 }

        if (pudding > mesaEnemiga.getPuddings()) {
            puntos += 6
        } else if (pudding < mesaEnemiga.getPuddings() && pudding > 0) {
            puntos -= 6
        }

        return puntos
    }

    private fun getMakis(): Int {
        return cartas.count { it == Carta.MAKI_ROLL_1 } +
            cartas.count { it == Carta.MAKI_ROLL_2 } * 2 +
            cartas.count { it == Carta.MAKI_ROLL_3 } * 3
    }
    private fun getPuddings(): Int {
        return cartas.count { it == Carta.PUDDING }
    }
}
