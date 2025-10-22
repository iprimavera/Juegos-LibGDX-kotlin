package com.iprimavera.juegos.sushiGame

class Mesa: Paquete {

    override var cartas: MutableList<Carta> = mutableListOf()

    fun addCarta(carta: Carta) {
        cartas.add(carta)
        // cartas.sort()
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
        for (i in 1..tempura/2) { puntos += 5 }

        if (pudding > mesaEnemiga.getPuddings()) {
            puntos += 6
        } else if (pudding < mesaEnemiga.getPuddings() && pudding > 0) {
            puntos -= 6
        }

        puntos += when (dumpling) {
            0 -> 0
            1 -> 1
            2 -> 3
            3 -> 6
            4 -> 10
            5 -> 15
            else -> 15
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
