package com.example.moneytracker.util

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.example.moneytracker.databinding.ToastCustomBinding

fun showCustomToast(context: Context, message: String) {
    val binding = ToastCustomBinding.inflate(LayoutInflater.from(context))
    binding.tvToast.text = message
    Toast(context).apply {
        view = binding.root
        duration = Toast.LENGTH_SHORT
    }.show()
}
