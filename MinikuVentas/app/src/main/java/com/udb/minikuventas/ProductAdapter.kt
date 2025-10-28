package com.udb.minikuventas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.udb.minikuventas.datos.Producto

class ProductAdapter(
    private var productos: MutableList<Producto>,
    private val onItemClick: (Producto) -> Unit,
    private val onItemLongClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvProductDescription: TextView = itemView.findViewById(R.id.tvProductDescription)
        val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val tvProductStock: TextView = itemView.findViewById(R.id.tvProductStock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_card, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val producto = productos[position]
        holder.tvProductName.text = producto.nombre
        holder.tvProductDescription.text = producto.descripcion ?: "" // Muestra vacío si no hay descripción
        holder.tvProductPrice.text = "$${String.format("%.2f", producto.precioUnitario)}"
        holder.tvProductStock.text = "Stock: ${producto.stock}"

        holder.ivProductImage.setImageResource(R.drawable.caja) // O tu logo u otra imagen

        // Configurar clics
        holder.itemView.setOnClickListener { onItemClick(producto) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(producto)
            true
        }
    }

    override fun getItemCount(): Int = productos.size

    fun updateData(newProducts: List<Producto>) {
        productos.clear()
        productos.addAll(newProducts)
        notifyDataSetChanged()
    }
}