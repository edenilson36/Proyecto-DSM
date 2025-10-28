
package com.udb.minikuventas.datos


data class Venta(
    val numeroFactura: String? = null,
    val fecha: Long? = 0L,
    val cliente: String? = null,
    val total: Double? = 0.0,
    val items: List<DetalleVenta>? = null // ¡Una lista de los productos vendidos!
) {

    constructor() : this(null, 0L, null, 0.0, null)
}