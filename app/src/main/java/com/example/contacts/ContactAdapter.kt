package com.example.contacts

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

/**
 * Adaptateur personnalisé pour afficher les contacts dans la ListView.
 * Chaque ligne présente : photo, nom+prénom, et numéro de téléphone.
 *
 * Utilise le patron ViewHolder pour de meilleures performances de scroll.
 */
class ContactAdapter(context: Context, private val contacts: List<Contact>)
    : ArrayAdapter<Contact>(context, 0, contacts) {

    // --- ViewHolder : évite les appels répétés à findViewById ---
    private class ViewHolder(view: View) {
        val imgPhoto: ImageView  = view.findViewById(R.id.imgPhoto)
        val tvNomPrenom: TextView = view.findViewById(R.id.tvNomPrenom)
        val tvTelephone: TextView = view.findViewById(R.id.tvTelephone)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val view: View

        if (convertView == null) {
            // Première utilisation : on gonfle la vue et on crée le holder
            view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            // Réutilisation d'une vue recyclée
            view = convertView
            holder = view.tag as ViewHolder
        }

        val contact = contacts[position]

        // Affichage du nom complet
        holder.tvNomPrenom.text = "${contact.prenom} ${contact.nom}"

        // Affichage du téléphone
        holder.tvTelephone.text = contact.telephone

        // Affichage de la photo
        if (contact.photoUri != null) {
            try {
                holder.imgPhoto.setImageURI(Uri.parse(contact.photoUri))
            } catch (e: Exception) {
                holder.imgPhoto.setImageResource(R.drawable.ic_person_placeholder)
            }
        } else {
            holder.imgPhoto.setImageResource(R.drawable.ic_person_placeholder)
        }

        return view
    }
}
