package com.example.contacts

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Activité de détail : permet de consulter et modifier un contact.
 *
 * Fonctionnalités :
 * - Affichage des champs : photo, nom, prénom, téléphone, e-mail
 * - Modification des champs texte
 * - Bouton « Appeler »       → Intent implicite ACTION_DIAL
 * - Bouton « E-mail »        → Intent implicite ACTION_SENDTO
 * - Bouton « Choisir photo » → Intent implicite vers la galerie
 * - Bouton « Prendre photo » → Intent implicite vers la caméra (avec FileProvider)
 * - Bouton « Enregistrer »   → renvoie le contact modifié à MainActivity (RESULT_OK)
 * - Bouton « Annuler »       → ferme sans sauvegarder (RESULT_CANCELED)
 * - Gestion des erreurs : champs vides, app manquante, caméra indisponible
 */
class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONTACT          = "extra_contact"
        const val EXTRA_CONTACT_POSITION = "extra_contact_position"
        const val EXTRA_CONTACT_RESULT   = "extra_contact_result"
    }

    // --- Vues ---
    private lateinit var imgPhoto: ImageView
    private lateinit var etNom: EditText
    private lateinit var etPrenom: EditText
    private lateinit var etTelephone: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnAppeler: Button
    private lateinit var btnEmail: Button
    private lateinit var btnChoisirPhoto: Button
    private lateinit var btnPrendrePhoto: Button
    private lateinit var btnEnregistrer: Button
    private lateinit var btnAnnuler: Button

    // --- Données ---
    private lateinit var contact: Contact
    private var photoUri: String? = null       // URI de la photo actuelle/nouvelle

    // URI temporaire de la photo prise par la caméra
    private var cameraImageUri: Uri? = null

    // =========================================================================
    // Lanceurs d'activité (Galerie, Caméra, Permission caméra)
    // =========================================================================

    /** Galerie : sélectionner une image existante */
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                // Prendre une permission persistante de lecture
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { /* non-persistable, pas grave */ }

                photoUri = uri.toString()
                imgPhoto.setImageURI(uri)
            } else {
                Toast.makeText(this, R.string.msg_selection_photo_annulee, Toast.LENGTH_SHORT).show()
            }
        }

    /** Caméra : prendre une photo */
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success && cameraImageUri != null) {
                photoUri = cameraImageUri.toString()
                imgPhoto.setImageURI(cameraImageUri)
            } else {
                Toast.makeText(this, R.string.msg_photo_annulee, Toast.LENGTH_SHORT).show()
            }
        }

    /** Demande de permission caméra */
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                lancerCamera()
            } else {
                Toast.makeText(this, R.string.msg_permission_camera_refusee, Toast.LENGTH_SHORT).show()
            }
        }

    // =========================================================================
    // Cycle de vie
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // Récupérer le contact transmis par MainActivity
        contact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_CONTACT, Contact::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Contact>(EXTRA_CONTACT)
        } ?: run {
                Toast.makeText(this, R.string.erreur_contact_introuvable, Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        photoUri = contact.photoUri

        // Lier les vues
        imgPhoto        = findViewById(R.id.imgPhotoDetail)
        etNom           = findViewById(R.id.etNom)
        etPrenom        = findViewById(R.id.etPrenom)
        etTelephone     = findViewById(R.id.etTelephone)
        etEmail         = findViewById(R.id.etEmail)
        btnAppeler      = findViewById(R.id.btnAppeler)
        btnEmail        = findViewById(R.id.btnEmail)
        btnChoisirPhoto = findViewById(R.id.btnChoisirPhoto)
        btnPrendrePhoto = findViewById(R.id.btnPrendrePhoto)
        btnEnregistrer  = findViewById(R.id.btnEnregistrer)
        btnAnnuler      = findViewById(R.id.btnAnnuler)

        // Pré-remplir les champs
        etNom.setText(contact.nom)
        etPrenom.setText(contact.prenom)
        etTelephone.setText(contact.telephone)
        etEmail.setText(contact.email)
        afficherPhoto()

        // ---- Listeners ----

        // Intent implicite : ouvrir le composeur téléphonique
        btnAppeler.setOnClickListener { appelerContact() }

        // Intent implicite : ouvrir une application de messagerie
        btnEmail.setOnClickListener { envoyerEmail() }

        // Galerie
        btnChoisirPhoto.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        // Caméra (avec vérification de permission)
        btnPrendrePhoto.setOnClickListener {
            verifierPermissionEtLancerCamera()
        }

        // Enregistrer → renvoi du résultat à MainActivity
        btnEnregistrer.setOnClickListener { enregistrer() }

        // Annuler → retour sans modification
        btnAnnuler.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    // =========================================================================
    // Fonctions métier
    // =========================================================================

    /** Affiche la photo dans l'ImageView (photo personnalisée ou icône par défaut) */
    private fun afficherPhoto() {
        if (photoUri != null) {
            try {
                imgPhoto.setImageURI(Uri.parse(photoUri))
            } catch (e: Exception) {
                imgPhoto.setImageResource(R.drawable.ic_person_placeholder)
            }
        } else {
            imgPhoto.setImageResource(R.drawable.ic_person_placeholder)
        }
    }

    /** Intent implicite : ouvre le composeur téléphonique avec le numéro du contact */
    private fun appelerContact() {
        val tel = etTelephone.text.toString().trim()
        if (tel.isEmpty()) {
            etTelephone.error = getString(R.string.msg_telephone_vide)
            return
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.msg_aucune_app_telephone, Toast.LENGTH_SHORT).show()
        }
    }

    /** Intent implicite : ouvre une application d'e-mail avec l'adresse pré-remplie */
    private fun envoyerEmail() {
        val mail = etEmail.text.toString().trim()
        if (mail.isEmpty()) {
            etEmail.error = getString(R.string.msg_email_vide)
            return
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$mail"))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.msg_aucune_app_email, Toast.LENGTH_SHORT).show()
        }
    }

    /** Vérifie la permission caméra avant de lancer l'appareil photo */
    private fun verifierPermissionEtLancerCamera() {
        // Sur Android 10+ la permission CAMERA est toujours requise pour accéder directement
        // à la caméra via Intent ACTION_IMAGE_CAPTURE
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, R.string.msg_camera_indisponible, Toast.LENGTH_SHORT).show()
            return
        }
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> lancerCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /** Crée un URI MediaStore pour stocker la photo et lance la caméra */
    private fun lancerCamera() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "contact_photo_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        cameraImageUri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        )
        if (cameraImageUri != null) {
            cameraLauncher.launch(cameraImageUri!!)
        } else {
            Toast.makeText(this, R.string.msg_camera_indisponible, Toast.LENGTH_SHORT).show()
        }
    }

    /** Valide les champs et renvoie le contact modifié à MainActivity */
    private fun enregistrer() {
        val nom      = etNom.text.toString().trim()
        val prenom   = etPrenom.text.toString().trim()
        val tel      = etTelephone.text.toString().trim()
        val mail     = etEmail.text.toString().trim()

        // Validation : champs obligatoires
        if (nom.isEmpty()) { etNom.error = getString(R.string.msg_champ_obligatoire); return }
        if (prenom.isEmpty()) { etPrenom.error = getString(R.string.msg_champ_obligatoire); return }
        if (tel.isEmpty()) { etTelephone.error = getString(R.string.msg_champ_obligatoire); return }

        // Mettre à jour le contact
        val updatedContact = contact.copy(
            nom       = nom,
            prenom    = prenom,
            telephone = tel,
            email     = mail,
            photoUri  = photoUri
        )

        val resultIntent = Intent().apply {
            putExtra(EXTRA_CONTACT_RESULT, updatedContact)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
