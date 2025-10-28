

package com.udb.minikuventas.datos


data class DetalleVenta(
    val nombreProducto: String? = null,
    val cantidad: Int? = 0,
    val precioUnitario: Double? = 0.0
) {
    constructor() : this(null, 0, 0.0)
}