package com.example.sariaudit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.example.sariaudit.ui.AppNavigation
import com.example.sariaudit.ui.theme.SariAuditTheme
import com.example.sariaudit.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            SariAuditTheme {
                AppNavigation(viewModel)
            }
        }
    }
}