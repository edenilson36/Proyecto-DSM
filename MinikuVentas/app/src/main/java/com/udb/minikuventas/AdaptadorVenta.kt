
package com.udb.minikuventas

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.udb.minikuventas.datos.Venta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdaptadorVenta(
    private val context: Activity,
    var ventas: List<Venta>
) : ArrayAdapter<Venta?>(context, R.layout.item_venta_detalle, ventas) {

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val layoutInflater = context.layoutInflater
        var rowview: View? = view


        rowview = rowview ?: layoutInflater.inflate(R.layout.item_venta_detalle, null)


        val tvProductoNombre = rowview!!.findViewById<TextView>(R.id.tvProductoNombre)
        val tvProductoCantidad = rowview.findViewById<TextView>(R.id.tvProductoCantidad)
        val tvProductoPrecio = rowview.findViewById<TextView>(R.id.tvProductoPrecio)
        val tvProductoTotal = rowview.findViewById<TextView>(R.id.tvProductoTotal)
        val tvProductoFecha = rowview.findViewById<TextView>(R.id.tvProductoFecha)
        val tvProductoCliente = rowview.findViewById<TextView>(R.id.tvProductoCliente)


        val venta = ventas[position]




        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fecha = sdf.format(Date(venta.fecha ?: 0L))
        tvProductoFecha.text = fecha


        tvProductoCliente.text = venta.cliente ?: "N/A"


        tvProductoTotal.text = "$${String.format("%.2f", venta.total)}"


        val primerItem = venta.items?.firstOrNull()

        if (primerItem != null) {
            tvProductoNombre.text = primerItem.nombreProducto
            tvProductoCantidad.text = primerItem.cantidad.toString()
            tvProductoPrecio.text = "$${String.format("%.2f", primerItem.precioUnitario)}"
        } else {
            tvProductoNombre.text = "N/A"
            tvProductoCantidad.text = "N/A"
            tvProductoPrecio.text = "N/A"
        }

        return rowview
    }
}