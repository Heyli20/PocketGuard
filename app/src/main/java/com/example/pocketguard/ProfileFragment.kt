package com.example.pocketguard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.example.pocketguard.utils.SharedPrefManager
import com.example.pocketguard.models.User
import com.example.pocketguard.utils.NotificationUtils
import com.example.pocketguard.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide
import android.util.Log
import androidx.appcompat.app.AlertDialog

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var imgProfile: ImageView
    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var spinnerCurrency: Spinner
    private var selectedImageUri: Uri? = null
    private var user: User? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            // All permissions granted
            pickImageFromGallery()
        } else {
            // Permission denied
            Toast.makeText(requireContext(), "Permission denied to access gallery", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                displayImageSafely(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        imgProfile = binding.imgProfile
        etName = binding.etName
        etPhone = binding.etPhone
        etPhone.isEnabled = false
        etPhone.isFocusable = false
        etEmail = binding.etEmail
        spinnerCurrency = binding.spinnerCurrency

        val btnUploadPhoto = binding.btnUploadPhoto
        val btnSave = binding.btnSaveProfile
        val btnLogout = binding.btnLogout

        val currencies = listOf("USD", "EUR", "INR", "GBP", "LKR")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCurrency.adapter = adapter

        val sharedPrefManager = SharedPrefManager(requireContext())
        val currentUserPhone = sharedPrefManager.getCurrentUserPhone() ?: ""

        if (currentUserPhone.isNotEmpty()) {
            val foundUser = sharedPrefManager.findUserByPhone(currentUserPhone)
            user = foundUser?.let {
                User(
                    name = it.name,
                    phone = it.phone,
                    email = it.email,
                    password = it.password,
                    currency = it.currency,
                    imageUri = it.imageUri
                )
            }

            user?.let { currentUser ->
                etName.setText(currentUser.name)
                etPhone.setText(currentUser.phone)
                etEmail.setText(currentUser.email)

                val currencyIndex = currencies.indexOf(currentUser.currency)
                if (currencyIndex != -1) {
                    spinnerCurrency.setSelection(currencyIndex)
                }

                // Load the saved image using Glide
                currentUser.imageUri?.let { uriString ->
                    try {
                        val uri = Uri.parse(uriString)
                        Glide.with(requireContext())
                            .load(uri)
                            .placeholder(R.drawable.profilepic)
                            .error(R.drawable.profilepic)
                            .into(imgProfile)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("ProfileFragment", "Error loading saved image: ${e.message}")
                        imgProfile.setImageResource(R.drawable.profilepic)
                    }
                }
            }
        } else {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_LONG).show()
            return binding.root
        }

        btnUploadPhoto.setOnClickListener {
            checkPermissionAndPickImage()
        }

        btnSave.setOnClickListener {
            val updatedUser = User(
                name = etName.text.toString(),
                phone = etPhone.text.toString(),
                email = etEmail.text.toString(),
                currency = spinnerCurrency.selectedItem.toString(),
                imageUri = selectedImageUri?.toString(),
                password = user?.password
            )

            sharedPrefManager.saveUserProfile(updatedUser)
            Toast.makeText(requireContext(), "Profile saved successfully!", Toast.LENGTH_SHORT).show()
        }

        setupLogoutButton()

        return binding.root
    }

    private fun setupLogoutButton() {
        binding.btnLogout.apply {
            isClickable = true
            isFocusable = true
            
            setOnClickListener { 
                Log.d("ProfileFragment", "Logout button clicked") // Debug log
                showLogoutConfirmationDialog()
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                Log.d("ProfileFragment", "Logout confirmed") // Debug log
                performLogout()
            }
            .setNegativeButton("No", null)
            .create()
            .show()
    }

    private fun performLogout() {
        try {
            val sharedPrefManager = SharedPrefManager(requireContext())
            sharedPrefManager.logout()

            // Show success message
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Show notification
            NotificationUtils.showNotification(
                context = requireContext(),
                title = "Logout Successful",
                message = "You have been logged out successfully. Thank you for using PocketGuard!",
                id = 111
            )

            // Navigate to Auth activity and clear back stack
            val intent = Intent(requireContext(), Auth::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error during logout: ${e.message}")
            Toast.makeText(requireContext(), "Logout failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            ))
        } else {
            // For Android 12 and below
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ))
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
        pickImageLauncher.launch(intent)
    }

    private fun displayImageSafely(uri: Uri?) {
        try {
            uri?.let {
                // Take persistent permissions
                requireContext().contentResolver.let { resolver ->
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    resolver.persistedUriPermissions.forEach { perm ->
                        if (perm.uri == uri) {
                            // Permission already exists
                            return@let
                        }
                    }
                    resolver.takePersistableUriPermission(uri, flags)
                }

                // Save the URI string to SharedPreferences
                val sharedPrefManager = SharedPrefManager(requireContext())
                val currentUserPhone = sharedPrefManager.getCurrentUserPhone()
                if (currentUserPhone != null) {
                    val user = sharedPrefManager.findUserByPhone(currentUserPhone)
                    user?.let { existingUser ->
                        val updatedUser = existingUser.copy(imageUri = it.toString())
                        sharedPrefManager.saveUserProfile(updatedUser)
                    }
                }

                // Display the image
                Glide.with(requireContext())
                    .load(it)
                    .placeholder(R.drawable.profilepic)
                    .error(R.drawable.profilepic)
                    .into(imgProfile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ProfileFragment", "Error loading image: ${e.message}")
            Toast.makeText(requireContext(), "Unable to load image: ${e.message}", Toast.LENGTH_SHORT).show()
            imgProfile.setImageResource(R.drawable.profilepic)
        }
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val view = activity?.currentFocus ?: View(requireContext())
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}









