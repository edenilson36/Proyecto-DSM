
package com.udb.minikuventas

import android.os.Bundle
import android.view.View // Importa View si no está
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // Importa AlertDialog si no está
import androidx.appcompat.app.AppCompatActivity
import com.udb.minikuventas.datos.Producto
import com.udb.minikuventas.databinding.ActivityAddProductBinding // Usamos ViewBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private lateinit var database: DatabaseReference


    private var key = ""
    private var accion = ""
    private var nombreProductoOriginal = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)


        database = FirebaseDatabase.getInstance().getReference("productos")

        obtenerDatosDeIntent()


        binding.btnGuardar.setOnClickListener {
            guardar()
        }


        binding.btnBorrar.setOnClickListener {
            mostrarDialogoConfirmacionBorrar()
        }
    }

    private fun obtenerDatosDeIntent() {
        val datos: Bundle? = intent.extras
        if (datos != null) {
            key = datos.getString("key").orEmpty() // Usamos orEmpty para evitar nulls
            accion = datos.getString("accion").orEmpty()

            if (accion == "e") { // Si es "Editar"
                nombreProductoOriginal = datos.getString("nombre").orEmpty()
                // Llenamos los campos
                binding.txtNombre.setText(nombreProductoOriginal)
                binding.txtDescripcion.setText(datos.getString("descripcion"))
                binding.txtPrecio.setText(datos.getDouble("precioUnitario", 0.0).toString()) // Default 0.0
                binding.txtStock.setText(datos.getInt("stock", 0).toString()) // Default 0

                // ¡¡MOSTRAMOS EL BOTÓN BORRAR!!
                binding.btnBorrar.visibility = View.VISIBLE
            } else { // Si es "Añadir"
                // Mantenemos el botón Borrar oculto (ya está GONE en el XML)
                binding.btnBorrar.visibility = View.GONE
            }
        } else {
            // Si no hay datos ( improbable pero seguro), asumimos que es Añadir
            binding.btnBorrar.visibility = View.GONE
        }
    }

    private fun guardar() {
        val nombre = binding.txtNombre.text.toString()
        val descripcion = binding.txtDescripcion.text.toString()
        val precio = binding.txtPrecio.text.toString().toDoubleOrNull() ?: 0.0
        val stock = binding.txtStock.text.toString().toIntOrNull() ?: 0

        if (nombre.isEmpty() || precio <= 0.0 || stock < 0) {
            Toast.makeText(this, "Nombre, Precio válido y Stock son requeridos", Toast.LENGTH_SHORT).show()
            return
        }

        // Creamos el objeto Producto (sin key por ahora)
        // Nota: Si implementas fotos, aquí recogerías también la imageUrl
        val producto = Producto(nombre = nombre, descripcion = descripcion, precioUnitario = precio, stock = stock)

        if (accion == "a") { // Agregar
            database.push().setValue(producto)
                .addOnSuccessListener {
                    Toast.makeText(this, "Producto guardado", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        } else if (accion == "e") {
            database.child(key).updateChildren(producto.toMap())
                .addOnSuccessListener {
                    Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun mostrarDialogoConfirmacionBorrar() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Borrado")
            .setMessage("¿Está seguro de que desea eliminar '${nombreProductoOriginal}'?")
            .setPositiveButton("Sí, Borrar") { _, _ ->
                borrarProducto() // Llama a la función de borrado si confirma
            }
            .setNegativeButton("No", null) // No hace nada si cancela
            .show()
    }


    private fun borrarProducto() {
        if (key.isNotEmpty()) { // Solo borra si tenemos una key válida (estamos editando)
            database.child(key).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
                    finish() // Cierra la actividad después de borrar
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "No se puede eliminar un producto sin ID", Toast.LENGTH_SHORT).show()
        }
    }

}