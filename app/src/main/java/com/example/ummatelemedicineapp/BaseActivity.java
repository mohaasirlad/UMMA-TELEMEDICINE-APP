package com.example.ummatelemedicineapp;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ummatelemedicineapp.utils.LocaleHelper;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}