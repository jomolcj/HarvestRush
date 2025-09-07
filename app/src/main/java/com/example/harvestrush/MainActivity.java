package com.example.harvestrush; // App's base package name – keeps classes logically grouped and avoids name collisions

import android.os.Bundle; // Bundle holds saved instance state across configuration changes

import androidx.activity.EdgeToEdge; // Helper to make content draw behind system bars (status/navigation)
import androidx.appcompat.app.AppCompatActivity; // Base class for Activities using the AppCompat support features
import androidx.core.graphics.Insets; // Represents insets (safe areas) like status bar height
import androidx.core.view.ViewCompat; // Backwards‑compatible view utilities (e.g., setting listeners)
import androidx.core.view.WindowInsetsCompat; // Compatibility wrapper for window insets across API levels

/**
 * MainActivity
 * This is currently a simple launcher/host activity that sets a layout and adjusts padding
 * so UI content does not get overlapped by system UI (status bar / navigation bar) while
 * still enabling an immersive edge‑to‑edge look.
 */
public class MainActivity extends AppCompatActivity { // Activity entry point (screen) extending compatibility support

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Lifecycle callback: invoked when the activity is first created
        super.onCreate(savedInstanceState); // Always call super to let the framework set up the base behavior
        EdgeToEdge.enable(this); // Enables drawing behind system bars (edge‑to‑edge layout)
        setContentView(R.layout.activity_main); // Inflate and attach the activity_main.xml layout as this screen's UI

        // Apply a listener to adjust the root view's padding based on system bar insets.
        // This ensures that when drawing edge‑to‑edge, content (like buttons/text) isn't hidden
        // behind the status bar or navigation bar. The view with id 'main' must exist in the layout.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); // Retrieve the current system bars (status + nav) dimensions
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom); // Add padding equal to system bar sizes to keep content visible
            return insets; // Return the original insets so further propagation (if any) can continue
        });

        // Configure the curved title view with the game title text.
        CurvedJiggleTitleView titleView = findViewById(R.id.titleView);
        if(titleView != null){
            titleView.setTitleText("HARVEST RUSH"); // Primary title text displayed along the curve
            titleView.setTextSizeSp(56); // Slightly larger than default for emphasis
        }
    }
}