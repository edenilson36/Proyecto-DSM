
package com.udb.minikuventas.datos

import com.google.firebase.database.Exclude


data class Producto(
    val nombre: String? = null,
    val descripcion: String? = null,
    val precioUnitario: Double? = 0.0,
    val stock: Int? = 0, //

    @get:Exclude
    var key: String? = null
) {
    // Constructor vacío requerido por Firebase
    constructor() : this(null, null, 0.0, 0)

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "nombre" to nombre,
            "descripcion" to descripcion,
            "precioUnitario" to precioUnitario,
            "stock" to stock
        )
    }
}