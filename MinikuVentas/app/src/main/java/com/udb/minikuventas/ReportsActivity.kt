package com.udb.minikuventas

import android.content.Intent
import android.os.Bundle
import android.util.Log // Importa Log para depuración
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.udb.minikuventas.datos.Producto
import com.udb.minikuventas.datos.Venta
import com.udb.minikuventas.databinding.ActivityReportsBinding
import com.google.firebase.database.*
import java.util.Calendar
import java.util.TimeZone

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private lateinit var refVentas: DatabaseReference
    private lateinit var refProductos: DatabaseReference

    private var todasLasVentas: MutableList<Venta> = mutableListOf()
    private var todosLosProductos: MutableList<Producto> = mutableListOf()
    private var ventasMostradas: MutableList<Venta> = mutableListOf()
    private var listaClientesUnicos: MutableList<String> = mutableListOf()

    private lateinit var adaptadorVenta: AdaptadorVenta
    private lateinit var adaptadorClientes: ArrayAdapter<String>

    private var filtroFechaActual = R.id.btnMensual
    private var chipSeleccionadoActual = R.id.chipVentas
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        refVentas = FirebaseDatabase.getInstance().getReference("ventas")
        refProductos = FirebaseDatabase.getInstance().getReference("productos")

        adaptadorVenta = AdaptadorVenta(this, ventasMostradas)
        adaptadorClientes = ArrayAdapter(this, android.R.layout.simple_list_item_1, listaClientesUnicos)
        binding.lvDetalleVentas.adapter = adaptadorVenta

        setupDateFilterListener()
        setupCategoryChipListener()
        escucharCambiosFirebase()
        setupTopNavigation()
    }

    private fun setupDateFilterListener() {
        binding.toggleGroupFechas.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked && checkedId != filtroFechaActual) {
                filtroFechaActual = checkedId
                actualizarVistaSegunSeleccion()
            }
        }
    }

    private fun setupCategoryChipListener() {
        binding.chipGroupCategorias.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val nuevoChipSeleccionado = checkedIds.first()
                if (nuevoChipSeleccionado != chipSeleccionadoActual) {
                    chipSeleccionadoActual = nuevoChipSeleccionado
                    actualizarVistaSegunSeleccion()
                }
            }
        }
    }

    private fun escucharCambiosFirebase() {
        // Listener Ventas
        refVentas.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                todasLasVentas.clear()
                for (ventaSnapshot in snapshot.children) {
                    ventaSnapshot.getValue(Venta::class.java)?.let { todasLasVentas.add(it) }
                }
                if (chipSeleccionadoActual == R.id.chipVentas || chipSeleccionadoActual == R.id.chipFinanzas || chipSeleccionadoActual == R.id.chipClientes) {
                    actualizarVistaSegunSeleccion()
                }
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })

        // Listener Productos
        refProductos.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                todosLosProductos.clear()
                for (prodSnapshot in snapshot.children) {
                    prodSnapshot.getValue(Producto::class.java)?.let { todosLosProductos.add(it) }
                }
                if (chipSeleccionadoActual == R.id.chipInventario) {
                    actualizarVistaSegunSeleccion()
                }
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun actualizarVistaSegunSeleccion() {
        if (isDestroyed || isFinishing) return
        when (chipSeleccionadoActual) {
            R.id.chipVentas -> mostrarReporteVentas()
            R.id.chipInventario -> mostrarReporteInventario()
            R.id.chipClientes -> mostrarReporteClientes()
            R.id.chipFinanzas -> mostrarReporteFinanzas()
        }
    }



    private fun mostrarReporteVentas() {
        binding.lvDetalleVentas.adapter = adaptadorVenta

        val (inicio, fin) = obtenerRangoFechas(filtroFechaActual) // Usamos destructuring con Pair
        ventasMostradas.clear()
        var ventasTotales = 0.0
        var productosVendidos = 0


        todasLasVentas.forEach { venta -> // Usamos forEach en lugar de filter
            if (venta.fecha != null && venta.fecha >= inicio && venta.fecha <= fin) {
                ventasMostradas.add(venta)
                ventasTotales += venta.total ?: 0.0
                venta.items?.forEach { item -> productosVendidos += item.cantidad ?: 0 }
            }
        }
        // -------------------------
        actualizarTarjetasVentas(ventasTotales, productosVendidos, ventasTotales)
        adaptadorVenta.notifyDataSetChanged()
        binding.tvDetalleTitulo.text = "Detalle de Ventas"
    }

    private fun mostrarReporteInventario() {
        binding.lvDetalleVentas.adapter = null
        ventasMostradas.clear()

        var valorTotalStock = 0.0
        var cantidadTotalItems = 0
        todosLosProductos.forEach { producto ->
            val stock = producto.stock ?: 0
            cantidadTotalItems += stock
            valorTotalStock += stock * (producto.precioUnitario ?: 0.0)
        }
        actualizarTarjetasInventario(todosLosProductos.size, cantidadTotalItems, valorTotalStock)
        binding.tvDetalleTitulo.text = "Resumen de Inventario"
    }

    private fun mostrarReporteClientes() {
        binding.lvDetalleVentas.adapter = adaptadorClientes
        // --- CORRECCIÓN AQUÍ ---
        val (inicio, fin) = obtenerRangoFechas(filtroFechaActual)
        val clientesPeriodo = mutableMapOf<String, Double>()
        var numeroVentasPeriodo = 0

        // --- CORRECCIÓN AQUÍ ---
        todasLasVentas.forEach { venta -> // Usamos forEach
            if (venta.fecha != null && venta.fecha >= inicio && venta.fecha <= fin) {
                numeroVentasPeriodo++
                val nombreCliente = venta.cliente?.trim()?.takeIf { it.isNotEmpty() } ?: "Desconocido"
                clientesPeriodo[nombreCliente] = (clientesPeriodo[nombreCliente] ?: 0.0) + (venta.total ?: 0.0)
            }
        }
        // -------------------------

        val clientesUnicos = clientesPeriodo.filterKeys { it != "Desconocido" }.size
        val totalGastado = clientesPeriodo.values.sum()
        val ventaPromedioCliente = if (clientesUnicos > 0) totalGastado / clientesUnicos else 0.0

        listaClientesUnicos.clear()
        clientesPeriodo.entries.sortedByDescending { it.value }.forEach { entry ->
            listaClientesUnicos.add("${entry.key} - $${String.format("%.2f", entry.value)}")
        }

        actualizarTarjetasClientes(clientesUnicos, numeroVentasPeriodo, ventaPromedioCliente)
        adaptadorClientes.notifyDataSetChanged()
        binding.tvDetalleTitulo.text = "Clientes del Periodo"
    }

    private fun mostrarReporteFinanzas() {
        binding.lvDetalleVentas.adapter = adaptadorVenta

        val (inicio, fin) = obtenerRangoFechas(filtroFechaActual) // Usamos destructuring con Pair
        ventasMostradas.clear()
        var ingresosTotales = 0.0
        var numeroVentas = 0


        todasLasVentas.forEach { venta -> // Usamos forEach
            if (venta.fecha != null && venta.fecha >= inicio && venta.fecha <= fin) { // Comparamos con inicio y fin
                ventasMostradas.add(venta)
                ingresosTotales += venta.total ?: 0.0
                numeroVentas++
            }
        }


        val ticketPromedio = if (numeroVentas > 0) ingresosTotales / numeroVentas else 0.0
        actualizarTarjetasFinanzas(ingresosTotales, numeroVentas, ticketPromedio)
        adaptadorVenta.notifyDataSetChanged()
        binding.tvDetalleTitulo.text = "Detalle de Ingresos"
    }




    private fun obtenerRangoFechas(filtroId: Int): Pair<Long, Long> {
        val tz = TimeZone.getTimeZone("America/El_Salvador")
        val calendario = Calendar.getInstance(tz)
        calendario.set(Calendar.HOUR_OF_DAY, 0); calendario.set(Calendar.MINUTE, 0); calendario.set(Calendar.SECOND, 0); calendario.set(Calendar.MILLISECOND, 0)
        val inicio: Long; val fin: Long
        when (filtroId) {
            R.id.btnDiario -> {
                inicio = calendario.timeInMillis
                calendario.add(Calendar.DAY_OF_YEAR, 1); calendario.add(Calendar.MILLISECOND, -1)
                fin = calendario.timeInMillis
            }
            R.id.btnSemanal -> {
                calendario.firstDayOfWeek = Calendar.MONDAY
                calendario.set(Calendar.DAY_OF_WEEK, calendario.firstDayOfWeek)
                inicio = calendario.timeInMillis
                calendario.add(Calendar.WEEK_OF_YEAR, 1); calendario.add(Calendar.MILLISECOND, -1)
                fin = calendario.timeInMillis
            }
            R.id.btnMensual -> {
                calendario.set(Calendar.DAY_OF_MONTH, 1)
                inicio = calendario.timeInMillis
                calendario.add(Calendar.MONTH, 1); calendario.add(Calendar.MILLISECOND, -1)
                fin = calendario.timeInMillis
            }
            R.id.btnPersonalizado -> { inicio = 0L; fin = Long.MAX_VALUE; Toast.makeText(this, "Mostrando todo", Toast.LENGTH_SHORT).show() }
            else -> { inicio = 0L; fin = Long.MAX_VALUE }
        }
        return Pair(inicio, fin) // Retorna Pair
    }
    // ----------------------------------------------------


    private fun actualizarTarjetasVentas(total: Double, cantidadProd: Int, ingresos: Double) {
        binding.tvCard1Title.text = "Ventas Totales"
        binding.tvCard1Value.text = "$${String.format("%.2f", total)}"
        binding.tvCard2Title.text = "Productos Vendidos"
        binding.tvCard2Value.text = cantidadProd.toString()
        binding.tvCard3Title.text = "Ingresos"
        binding.tvCard3Value.text = "$${String.format("%.2f", ingresos)}"
    }

    private fun actualizarTarjetasInventario(prodUnicos: Int, itemsTotales: Int, valorStock: Double) {
        binding.tvCard1Title.text = "Productos Únicos"
        binding.tvCard1Value.text = prodUnicos.toString()
        binding.tvCard2Title.text = "Items en Stock"
        binding.tvCard2Value.text = itemsTotales.toString()
        binding.tvCard3Title.text = "Valor del Stock"
        binding.tvCard3Value.text = "$${String.format("%.2f", valorStock)}"
    }

    private fun actualizarTarjetasClientes(clientesUnicos: Int, numVentas: Int, ventaPromedio: Double) {
        binding.tvCard1Title.text = "Clientes Únicos"
        binding.tvCard1Value.text = clientesUnicos.toString()
        binding.tvCard2Title.text = "Nº Ventas Periodo"
        binding.tvCard2Value.text = numVentas.toString()
        binding.tvCard3Title.text = "Gasto Prom./Cliente"
        binding.tvCard3Value.text = "$${String.format("%.2f", ventaPromedio)}"
    }

    private fun actualizarTarjetasFinanzas(ingresos: Double, numVentas: Int, ticketPromedio: Double) {
        binding.tvCard1Title.text = "Ingresos Totales"
        binding.tvCard1Value.text = "$${String.format("%.2f", ingresos)}"
        binding.tvCard2Title.text = "Nº Ventas"
        binding.tvCard2Value.text = numVentas.toString()
        binding.tvCard3Title.text = "Ticket Promedio"
        binding.tvCard3Value.text = "$${String.format("%.2f", ticketPromedio)}"
    }


    private fun setupTopNavigation() {
        if (binding.tabLayout.tabCount == 0) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.caja).setText("Inventario"))
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.campana).setText("Reportes"))
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.carrito).setText("Nueva Venta"))
        }
        val reportsTab = binding.tabLayout.getTabAt(1)
        binding.tabLayout.selectTab(reportsTab, false)

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (!isNavigating) {
                    when (tab?.position) {
                        0 -> navigateTo(InventoryActivity::class.java)
                        1 -> { /* Ya estamos aquí */ }
                        2 -> navigateTo(NewSaleActivity::class.java)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    private fun navigateTo(activityClass: Class<*>) {
        if (isNavigating) return
        isNavigating = true
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }
    override fun onResume() {
        super.onResume()
        isNavigating = false
        val reportsTab = binding.tabLayout.getTabAt(1)
        binding.tabLayout.selectTab(reportsTab, false)
        actualizarVistaSegunSeleccion()
    }
}