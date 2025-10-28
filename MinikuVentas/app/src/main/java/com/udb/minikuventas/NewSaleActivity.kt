package com.udb.minikuventas

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.udb.minikuventas.datos.DetalleVenta
import com.udb.minikuventas.datos.Producto // Asegúrate de tener este import
import com.udb.minikuventas.datos.Venta
import com.udb.minikuventas.databinding.ActivityNewSaleBinding
import com.google.firebase.database.DataSnapshot // Asegúrate de tener este import
import com.google.firebase.database.DatabaseError // Asegúrate de tener este import
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener // Asegúrate de tener este import

class NewSaleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewSaleBinding
    private lateinit var refVentas: DatabaseReference
    private lateinit var refProductos: DatabaseReference // Referencia a productos
    private val listaItems = mutableListOf<DetalleVenta>()
    private var totalVenta = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewSaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        refVentas = FirebaseDatabase.getInstance().getReference("ventas")
        refProductos = FirebaseDatabase.getInstance().getReference("productos") // Inicializar refProductos

        binding.btnAgregarProducto.setOnClickListener {
            agregarProducto()
        }

        binding.btnGenerarFactura.setOnClickListener {
            generarFactura()
        }

        setupTopNavigation()
    }

    private fun setupTopNavigation() {

        if (binding.tabLayout.tabCount == 0) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.caja).setText("Inventario"))
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.campana).setText("Reportes"))
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.carrito).setText("Nueva Venta"))
        }

        // Seleccionar la pestaña correcta
        val saleTab = binding.tabLayout.getTabAt(2) // Índice 2 para Nueva Venta
        if (saleTab?.isSelected == false) {
            binding.tabLayout.post { saleTab.select() }
        }


        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val currentActivityClass = this@NewSaleActivity::class.java
                when (tab?.position) {
                    0 -> if (currentActivityClass != InventoryActivity::class.java) navigateTo(InventoryActivity::class.java)
                    1 -> if (currentActivityClass != ReportsActivity::class.java) navigateTo(ReportsActivity::class.java)
                    2 -> {

                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
    }

    private fun agregarProducto() {
        val nombre = binding.txtNombreProducto.text.toString().trim() // Usar trim para quitar espacios
        val cantidad = binding.txtCantidad.text.toString().toIntOrNull() ?: 0
        val precio = binding.txtPrecio.text.toString().toDoubleOrNull() ?: 0.0

        if (nombre.isEmpty() || cantidad <= 0 || precio <= 0.0) {
            Toast.makeText(this, "Datos del producto incorrectos", Toast.LENGTH_SHORT).show()
            return
        }



        val item = DetalleVenta(nombre, cantidad, precio)
        listaItems.add(item)
        totalVenta += (cantidad * precio)
        actualizarResumenUI()

        binding.txtNombreProducto.text.clear()
        binding.txtCantidad.text.clear()
        binding.txtPrecio.text.clear()
    }

    private fun actualizarResumenUI() {
        var resumen = ""
        if (listaItems.isEmpty()) {
            resumen = "Añade productos a la venta..."
        } else {
            for (item in listaItems) {
                val subtotal = (item.cantidad ?: 0) * (item.precioUnitario ?: 0.0)
                resumen += "${item.nombreProducto} (x${item.cantidad}) - $${String.format("%.2f", subtotal)}\n"
            }
            resumen += "\nTOTAL: $${String.format("%.2f", totalVenta)}"
        }
        binding.tvResumenVenta.text = resumen
    }

    private fun generarFactura() {
        // --- Generar número de factura único ---
        val timestamp = System.currentTimeMillis()
        val randomSuffix = (100..999).random()
        val facturaNum = "INV-${timestamp}-${randomSuffix}"
        // ----------------------------------------

        val cliente = binding.txtCliente.text.toString().trim() // Usar trim


        if (listaItems.isEmpty()) {
            Toast.makeText(this, "Se requiere al menos 1 producto para generar la factura", Toast.LENGTH_SHORT).show()
            return
        }
        // ----------------------------------

        val venta = Venta(
            numeroFactura = facturaNum,
            fecha = System.currentTimeMillis(), // Usamos timestamp actual para la fecha
            cliente = if (cliente.isNotEmpty()) cliente else "N/A", // Guardar N/A si está vacío
            total = totalVenta,
            items = listaItems
        )


        refVentas.push().setValue(venta)
            .addOnSuccessListener {
                Toast.makeText(this, "Factura Generada (${venta.numeroFactura})", Toast.LENGTH_LONG).show()


                actualizarStockProductos(venta.items)


                listaItems.clear()
                totalVenta = 0.0
                actualizarResumenUI()
                binding.txtCliente.text.clear()

            }
            .addOnFailureListener { e -> // Capturar excepción para más detalles
                Toast.makeText(this, "Error al generar factura: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun actualizarStockProductos(itemsVendidos: List<DetalleVenta>?) {
        if (itemsVendidos == null) return

        itemsVendidos.forEach { itemVendido ->
            val nombreProducto = itemVendido.nombreProducto?.trim() // Usar trim al buscar
            val cantidadVendida = itemVendido.cantidad ?: 0

            if (nombreProducto.isNullOrEmpty() || cantidadVendida <= 0) {

                return@forEach
            }


            val query = refProductos.orderByChild("nombre").equalTo(nombreProducto)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {

                        snapshot.children.firstOrNull()?.let { productoSnapshot ->
                            val productoKey = productoSnapshot.key
                            val productoActual = productoSnapshot.getValue(Producto::class.java)

                            if (productoKey != null && productoActual != null) {
                                val stockActual = productoActual.stock ?: 0
                                val nuevoStock = stockActual - cantidadVendida


                                refProductos.child(productoKey).child("stock").setValue(nuevoStock)
                                    .addOnSuccessListener {

                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this@NewSaleActivity, "Error al actualizar stock de '$nombreProducto': ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {

                            }
                        }
                    } else {
                        Toast.makeText(this@NewSaleActivity, "Producto '$nombreProducto' no encontrado para actualizar stock", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@NewSaleActivity, "Error al buscar '$nombreProducto': ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()

        val saleTab = binding.tabLayout.getTabAt(2)
        if (saleTab?.isSelected == false) {
            binding.tabLayout.post { saleTab.select() }
        }
    }
}