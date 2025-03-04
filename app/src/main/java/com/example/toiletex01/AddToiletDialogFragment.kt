package com.example.toiletex01

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.widget.EditText

class AddToiletDialogFragment : DialogFragment() {

    interface OnAddToiletListener {
        fun onAddToiletSubmit(toiletName: String, password: String)
    }

    private var listener: OnAddToiletListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnAddToiletListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnAddToiletListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate the custom layout for the dialog
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_add_toilet, null)
        val toiletNameEditText = view.findViewById<EditText>(R.id.editTextToiletName)
        val passwordEditText = view.findViewById<EditText>(R.id.editTextPassword)

        return AlertDialog.Builder(requireContext())
            .setTitle("Add Toilet")
            .setView(view)
            .setPositiveButton("추가하기") { dialog, which ->
                val toiletName = toiletNameEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()
                if (toiletName.isNotEmpty() && password.isNotEmpty()) {
                    listener?.onAddToiletSubmit(toiletName, password)
                }
            }
            .setNegativeButton("취소", null)
            .create()
    }
}