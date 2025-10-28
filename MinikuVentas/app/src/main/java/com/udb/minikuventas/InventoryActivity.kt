
package com.udb.minikuventas

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.udb.minikuventas.datos.Producto
import com.udb.minikuventas.databinding.ActivityInventoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class InventoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInventoryBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var refProductos: DatabaseReference


    private var productosCompletos: MutableList<Producto> = mutableListOf()


    private var productosMostrados: MutableList<Producto> = mutableListOf()


    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        refProductos = database.getReference("productos")

        // --- Config RecyclerView ---
        // 1. Inicializa el adaptador con la lista (aún vacía) que se mostrará
        productAdapter = ProductAdapter(productosMostrados,
            onItemClick = { producto ->
                // Lógica Editar
                val intent = Intent(this, AddProductActivity::class.java).apply {
                    putExtra("accion", "e")
                    putExtra("key", producto.key)
                    putExtra("nombre", producto.nombre)
                    putExtra("descripcion", producto.descripcion)
                    putExtra("precioUnitario", producto.precioUnitario)
                    putExtra("stock", producto.stock)

                }
                startActivity(intent)
            },
            onItemLongClick = { producto ->
                // Lógica Borrar
                showDeleteConfirmationDialog(producto)
            }
        )

        binding.recyclerViewProductos.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewProductos.adapter = productAdapter



        binding.fabAgregar.setOnClickListener {
            val intent = Intent(this, AddProductActivity::class.java).apply {
                putExtra("accion", "a")
            }
            startActivity(intent)
        }



        binding.etSearchProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filtra usando la lista completa y actualiza la lista mostrada
                filterAndUpdateAdapter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })




        loadProductsFromFirebase()



        setupTopNavigation()



        binding.btnMenu.setOnClickListener { view ->
            showPopupMenu(view)
        }

    }

    private fun showDeleteConfirmationDialog(producto: Producto) {
        AlertDialog.Builder(this)
            .setTitle("Confirmación")
            .setMessage("¿Eliminar '${producto.nombre}'?")
            .setPositiveButton("Sí") { _, _ ->
                producto.key?.let { key ->
                    refProductos.child(key).removeValue()
                        .addOnSuccessListener { Toast.makeText(this, "Producto borrado", Toast.LENGTH_SHORT).show() }
                        .addOnFailureListener { Toast.makeText(this, "Error al borrar", Toast.LENGTH_SHORT).show() }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun loadProductsFromFirebase() {
        refProductos.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                productosCompletos.clear()

                for (dato in snapshot.children) {
                    val producto = dato.getValue(Producto::class.java)
                    if (producto != null) {
                        producto.key = dato.key
                        productosCompletos.add(producto)
                    }
                }

                filterAndUpdateAdapter(binding.etSearchProduct.text.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@InventoryActivity, "Error al leer datos: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }


    private fun filterAndUpdateAdapter(query: String) {

        productosMostrados.clear()


        if (query.isEmpty()) {

            productosMostrados.addAll(productosCompletos)
        } else {

            val lowerCaseQuery = query.lowercase().trim()
            productosCompletos.forEach { producto ->
                if (producto.nombre?.lowercase()?.contains(lowerCaseQuery) == true ||
                    producto.descripcion?.lowercase()?.contains(lowerCaseQuery) == true) {
                    productosMostrados.add(producto)
                }
            }
        }

        productAdapter.notifyDataSetChanged()
    }


    private fun setupTopNavigation() {
        if (binding.tabLayout.tabCount == 0) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.caja).setText("Inventario"))
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.campana).setText("Reportes"))
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.carrito).setText("Nueva Venta"))
        }

        val inventoryTab = binding.tabLayout.getTabAt(0)
        if (inventoryTab?.isSelected == false) {

            binding.tabLayout.post { inventoryTab.select() }
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {

                val currentActivityClass = this@InventoryActivity::class.java
                when (tab?.position) {
                    0 -> if (currentActivityClass != InventoryActivity::class.java) navigateTo(InventoryActivity::class.java)
                    1 -> if (currentActivityClass != ReportsActivity::class.java) navigateTo(ReportsActivity::class.java)
                    2 -> if (currentActivityClass != NewSaleActivity::class.java) navigateTo(NewSaleActivity::class.java)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {} // No hacer nada si se reselecciona
        })
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_sign_out -> {
                    auth.signOut()
                    Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, RegisterActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun onResume() {
        super.onResume()

        val inventoryTab = binding.tabLayout.getTabAt(0)
        if (inventoryTab?.isSelected == false) {
            binding.tabLayout.post { inventoryTab.select() }
        }
    }
}