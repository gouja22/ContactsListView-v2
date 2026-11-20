package com.example.contacts

import android.os.Parcel
import android.os.Parcelable

/**
 * Modèle de données représentant un contact.
 * Implémente Parcelable pour être transmis entre activités via Intent.
 */
data class Contact(
    val id: Int,
    var nom: String,
    var prenom: String,
    var telephone: String,
    var email: String,
    var photoUri: String? = null  // URI de la photo (null = photo par défaut)
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id       = parcel.readInt(),
        nom      = parcel.readString() ?: "",
        prenom   = parcel.readString() ?: "",
        telephone= parcel.readString() ?: "",
        email    = parcel.readString() ?: "",
        photoUri = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(nom)
        parcel.writeString(prenom)
        parcel.writeString(telephone)
        parcel.writeString(email)
        parcel.writeString(photoUri)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Contact> {
        override fun createFromParcel(parcel: Parcel): Contact = Contact(parcel)
        override fun newArray(size: Int): Array<Contact?> = arrayOfNulls(size)
    }
}
