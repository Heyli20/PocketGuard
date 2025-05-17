package com.example.pocketguard

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.pocketguard.databinding.FragmentResetPasswordBinding
import com.example.pocketguard.utils.SecurityUtils
import com.google.gson.Gson
import com.example.pocketguard.utils.SharedPrefManager

class ResetPasswordFragment : Fragment() {
    private lateinit var binding: FragmentResetPasswordBinding
    private val args: ResetPasswordFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentResetPasswordBinding.inflate(inflater, container, false)

        binding.etConfirmPassword.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                binding.btnResetPassword.performClick() // Trigger reset logic
                true
            } else false
        }

        binding.btnResetPassword.setOnClickListener {
            val newPassword = binding.etNewPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            val phone = args.phone
            val sharedPrefManager = SharedPrefManager(requireContext())

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                binding.etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            val hashedPsw = SecurityUtils.hashPassword(newPassword)

           if(sharedPrefManager.updatePassword(phone,hashedPsw)){
               Toast.makeText(requireContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show()

               findNavController().navigate(R.id.action_resetPasswordFragment_to_loginFragment)
           }else{
               Toast.makeText(requireContext(),"Password couldn't reset, Try again!",Toast.LENGTH_SHORT).show()
           }
        }

        return binding.root
    }
}
