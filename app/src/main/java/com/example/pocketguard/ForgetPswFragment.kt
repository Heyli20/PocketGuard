package com.example.pocketguard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.example.pocketguard.databinding.FragmentForgetPswBinding
import com.example.pocketguard.utils.SharedPrefManager

class ForgetPswFragment : Fragment() {
    private lateinit var binding: FragmentForgetPswBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForgetPswBinding.inflate(inflater, container, false)

        binding.etPhoneReset.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                binding.btnSendOtp.performClick() // Trigger reset logic
                true
            } else false
        }

        binding.btnSendOtp.setOnClickListener {
            val phone = binding.etPhoneReset.text.toString().trim()
            val sharedPrefManager = SharedPrefManager(requireContext())
            val user = sharedPrefManager.findUserByPhone(phone)

            if (phone.isEmpty() || phone.length < 10) {
                Toast.makeText(requireContext(), "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(user != null){
                val otp = (100000..999999).random().toString()
                sharedPrefManager.storeOTP(phone, otp)

                sendOtpSms(phone, otp)
                val action = ForgetPswFragmentDirections.actionForgetPswFragmentToVerifyOtpFragment(phone)
                findNavController().navigate(action)
            }else{
                Toast.makeText(requireContext(),"Phone number not found",Toast.LENGTH_LONG).show()
            }
        }

        return binding.root
    }

    private fun sendOtpSms(phone: String, otp: String) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.SEND_SMS),
                101
            )

        } else {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phone, null, "Your PocketGuard OTP is: $otp", null, null)
            Toast.makeText(requireContext(), "OTP sent to $phone", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Permission granted. Please tap Send OTP again.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "SMS permission denied", Toast.LENGTH_SHORT).show()
        }
    }

}
