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
import com.example.pocketguard.databinding.FragmentVerifyOtpBinding
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.pocketguard.utils.SharedPrefManager


class VerifyOtpFragment : Fragment() {
    private lateinit var binding: FragmentVerifyOtpBinding
    private val args: VerifyOtpFragmentArgs by navArgs() // receives phone number

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVerifyOtpBinding.inflate(inflater, container, false)

        binding.etOtp.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                binding.btnVerifyOtp.performClick() // Trigger reset logic
                true
            } else false
        }

        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            val sharedPrefManager = SharedPrefManager(requireContext())

            if (otp.length != 6) {
                binding.etOtp.error = "Enter 6-digit OTP"
            } else {
                val storedOTP = sharedPrefManager.getOTP(args.phone)
                if (storedOTP != null && storedOTP.otp == otp) {
                    if (sharedPrefManager.isOTPValid(args.phone)) {
                        Toast.makeText(requireContext(), "OTP Verified", Toast.LENGTH_LONG).show()
                        val action = VerifyOtpFragmentDirections.actionVerifyOtpFragmentToResetPasswordFragment(args.phone)
                        findNavController().navigate(action)
                    } else {
                        Toast.makeText(requireContext(), "OTP has expired. Please request a new one.", Toast.LENGTH_LONG).show()
                        binding.etOtp.setText("")
                    }
                } else {
                    sharedPrefManager.incrementOTPAttempts(args.phone)
                    if (sharedPrefManager.getOTPAttempts(args.phone) >= 3) {
                        sharedPrefManager.clearOTP(args.phone)
                        Toast.makeText(requireContext(), "Too many attempts. Please request a new OTP.", Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(requireContext(), "Invalid OTP. ${3 - sharedPrefManager.getOTPAttempts(args.phone)} attempts remaining.", Toast.LENGTH_LONG).show()
                        binding.etOtp.setText("")
                    }
                }
            }
        }

        return binding.root
    }
}
