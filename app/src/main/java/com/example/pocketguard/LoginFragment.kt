package com.example.pocketguard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.pocketguard.databinding.FragmentLoginBinding
import com.example.pocketguard.utils.SecurityUtils
import com.example.pocketguard.utils.SharedPrefManager
import com.example.pocketguard.models.User

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etLoginPassword.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                binding.btnLogin.performClick()
                true
            } else false
        }

        // Sign In button click
            binding.btnLogin.setOnClickListener {
                val emailOrPhone = binding.etEmailOrPhone.text.toString().trim()
                val password = binding.etLoginPassword.text.toString().trim()
                val inputHashedPassword = SecurityUtils.hashPassword(password)

                val sharedPrefManager = SharedPrefManager(requireContext())

                // Explicitly cast the result to the correct User type
                val user = sharedPrefManager.findUserByPhone(emailOrPhone)?.let { foundUser ->
                    User(
                        name = foundUser.name,
                        phone = foundUser.phone,
                        email = foundUser.email,
                        password = foundUser.password,
                        currency = foundUser.currency,
                        imageUri = foundUser.imageUri
                    )
                }

                if (user != null) {
                    if (user.password == inputHashedPassword) {
                        sharedPrefManager.createSession(user.phone)
                        Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()

                        val intent = Intent(requireContext(), Home::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Invalid Email or Password",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "User doesn't exist", Toast.LENGTH_LONG).show()
                }
            }


        // Navigate to Register Page
        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Navigate to Forgot Password Page
        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }



    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



