package com.example.contacts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Activité principale : affiche la liste des contacts dans une ListView personnalisée.
 *
 * Fonctionnalités :
 * - Affichage de contacts avec photo, nom, prénom et téléphone (via ContactAdapter)
 * - Sélection d'un contact → ouverture de DetailActivity (Intent explicite)
 * - Réception du contact modifié via ActivityResult et mise à jour de la liste
 */
class MainActivity : AppCompatActivity() {

    // --- Données ---
    private val contacts = mutableListOf(
        Contact(0, "Ben Ali",    "Ahmed",   "+216 20 000 001", "ahmed.benali@email.tn"),
        Contact(1, "Trabelsi",   "Sarra",   "+216 20 000 002", "sarra.trabelsi@email.tn"),
        Contact(2, "Gharbi",     "Mohamed", "+216 20 000 003", "med.gharbi@email.tn"),
        Contact(3, "Jaziri",     "Ines",    "+216 20 000 004", "ines.jaziri@email.tn"),
        Contact(4, "Mansouri",   "Youssef", "+216 20 000 005", "youssef.mansouri@email.tn"),
        Contact(5, "Chaabane",   "Amira",   "+216 20 000 006", "amira.chaabane@email.tn"),
        Contact(6, "Bouazizi",   "Karim",   "+216 20 000 007", "karim.bouazizi@email.tn"),
        Contact(7, "Hammami",    "Nour",    "+216 20 000 008", "nour.hammami@email.tn")
    )

    private lateinit var adapter: ContactAdapter
    private lateinit var lvContacts: ListView

    // --- Lanceur d'activité avec retour de résultat (remplace startActivityForResult) ---
    private val detailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val updatedContact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra(DetailActivity.EXTRA_CONTACT_RESULT, Contact::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra<Contact>(DetailActivity.EXTRA_CONTACT_RESULT)
                }

                if (updatedContact != null) {
                    // Retrouver le contact dans la liste par son ID et le remplacer
                    val idx = contacts.indexOfFirst { it.id == updatedContact.id }
                    if (idx != -1) {
                        contacts[idx] = updatedContact
                        adapter.notifyDataSetChanged()
                        Toast.makeText(this,
                            getString(R.string.msg_contact_mis_a_jour, updatedContact.prenom),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lvContacts = findViewById(R.id.lvContacts)

        adapter = ContactAdapter(this, contacts)
        lvContacts.adapter = adapter

        // Clic sur un contact → ouvrir DetailActivity avec ce contact (Intent explicite)
        lvContacts.setOnItemClickListener { _, _, position, _ ->
            val contact = contacts[position]
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra(DetailActivity.EXTRA_CONTACT, contact)
                putExtra(DetailActivity.EXTRA_CONTACT_POSITION, position)
            }
            detailLauncher.launch(intent)
        }
    }
}
