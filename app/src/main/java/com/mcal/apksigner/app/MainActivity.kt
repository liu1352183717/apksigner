package com.mcal.apksigner.app

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apksigner.ApkSigner
import com.mcal.apksigner.app.databinding.ActivityMainBinding
import com.mcal.apksigner.app.filepicker.FilePickHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(
            layoutInflater
        )
    }

    private var apkFile: File? = null
    private var pk8File: File? = null
    private var x509File: File? = null
    private var jksFile: File? = null

    private var pickApkFile =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    contentResolver.openInputStream(uri)?.let { inputStream ->
                        val name = FilePickHelper.getFileName(this, uri)
                        File(filesDir, name).also {
                            it.writeBytes(inputStream.readBytes())
                            apkFile = it
                        }
                        inputStream.close()
                        setEnabled(binding.signApkWithPem, true)
                        setEnabled(binding.signApkWithJks, true)
                        binding.selectApk.text = name
                    }
                }
            }
        }

    private var pickPk8File =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    contentResolver.openInputStream(uri)?.let { inputStream ->
                        val name = FilePickHelper.getFileName(this, uri)
                        File(filesDir, name).also {
                            it.writeBytes(inputStream.readBytes())
                            pk8File = it
                        }
                        inputStream.close()
                        binding.selectPk8.text = name
                    }
                }
            }
        }

    private var pickX509File =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    val name = FilePickHelper.getFileName(this, uri)
                    contentResolver.openInputStream(uri)?.let { inputStream ->
                        File(filesDir, name).also {
                            it.writeBytes(inputStream.readBytes())
                            x509File = it
                        }
                        inputStream.close()
                        binding.selectX509.text = name
                    }
                }
            }
        }

    private var pickJks =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    contentResolver.openInputStream(uri)?.let { inputStream ->
                        val name = FilePickHelper.getFileName(this, uri)
                        File(filesDir, name).also {
                            it.writeBytes(inputStream.readBytes())
                            jksFile = it
                        }
                        inputStream.close()
                        binding.selectJksKey.text = name
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.selectApk.setOnClickListener {
            pickApkFile.launch(FilePickHelper.pickFile(true))
        }
        binding.selectPk8.setOnClickListener {
            pickPk8File.launch(FilePickHelper.pickFile(false))
        }
        binding.selectX509.setOnClickListener {
            pickX509File.launch(FilePickHelper.pickFile(false))
        }
        binding.selectJksKey.setOnClickListener {
            pickJks.launch(FilePickHelper.pickFile(false))
        }
        binding.signApkWithJks.setOnClickListener { view ->
            setEnabled(view, false)
            val apk = apkFile
            if (apk != null && apk.exists()) {
                val jks = jksFile
                if (jks != null && jks.exists()) {
                    val certPass = binding.certPass.text.toString().trim()
                    if (certPass.isNotEmpty()) {
                        val certAlias = binding.certAlias.text.toString().trim()
                        if (certAlias.isNotEmpty()) {
                            val keyPass = binding.keyPass.text.toString().trim()
                            if (keyPass.isNotEmpty()) {
                                val dialog = dialog().apply {
                                    show()
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    ApkSigner.sign(
                                        apk,
                                        File(getExternalFilesDir(null), "app_signed.apk"),
                                        jks,
                                        certPass,
                                        certAlias,
                                        keyPass
                                    )
                                    withContext(Dispatchers.Main) {
                                        dialog.dismiss()
                                        setEnabled(view, true)
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Enter key password!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } else {
                            Toast.makeText(this, "Enter cert alias!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Enter cert password!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please select JKS keystore!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please select APK File!", Toast.LENGTH_SHORT).show()
            }
        }
        binding.signApkWithPem.setOnClickListener { view ->
            setEnabled(view, false)
            val apk = apkFile
            if (apk != null && apk.exists()) {
                val pk8 = pk8File
                if (pk8 != null && pk8.exists()) {
                    val x509 = x509File
                    if (x509 != null && x509.exists()) {
                        val dialog = dialog().apply {
                            show()
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            ApkSigner.sign(
                                apk,
                                File(getExternalFilesDir(null), "app_signed.apk"),
                                pk8,
                                x509
                            )
                            withContext(Dispatchers.Main) {
                                dialog.dismiss()
                                setEnabled(view, true)
                            }
                        }
                    } else {
                        Toast.makeText(this, "Please select x509.pem keystore!", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(this, "Please select pk8 keystore!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please select APK File!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dialog(): AlertDialog {
        return MaterialAlertDialogBuilder(this).apply {
            setView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(48, 48, 48, 48)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                addView(ProgressBar(context).apply {
                    setPadding(0, 48, 0, 48)
                })
                addView(AppCompatTextView(context).apply {
                    text = context.getString(R.string.signing)
                    setPadding(0, 0, 0, 48)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                })
            })
        }.create()
    }

    private fun setVisibility(view: View, mode: Int) {
        if (view.visibility != mode) {
            view.visibility = mode
        }
    }

    private fun setEnabled(view: View, mode: Boolean) {
        if (view.isEnabled != mode) {
            view.isEnabled = mode
        }
    }
}