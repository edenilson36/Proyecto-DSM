

package com.udb.minikuventas

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.udb.minikuventas.datos.Producto // <-- ¡Import corregido!


class AdaptadorProducto(
    private val context: Activity,
    var productos: List<Producto>
) : ArrayAdapter<Producto?>(context, R.layout.producto_item, productos) {


    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val layoutInflater = context.layoutInflater
        var rowview: View? = view


        rowview = rowview ?: layoutInflater.inflate(R.layout.producto_item, null)



        val tvNombre = rowview!!.findViewById<TextView>(R.id.tvNombre)
        val tvStock = rowview.findViewById<TextView>(R.id.tvStock)
        val tvPrecio = rowview.findViewById<TextView>(R.id.tvPrecio)


        val producto = productos[position]


        tvNombre.text = producto.nombre
        tvStock.text = "Cantidad: ${producto.stock}"
        // Formateamos el precio para que se vea bien (ej: $10.00)
        tvPrecio.text = "Precio: $${String.format("%.2f", producto.precioUnitario)}"

        return rowview
    }
}