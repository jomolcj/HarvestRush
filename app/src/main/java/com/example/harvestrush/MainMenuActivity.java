package com.example.harvestrush;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {
    private static final String TAG = "MainMenu";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        Log.d(TAG, "onCreate: MainMenuActivity loaded");

        CurvedJiggleTitleView titleView = findViewById(R.id.animated_title);
        Button startGameBtn = findViewById(R.id.btn_start_game);
        Button exitBtn = findViewById(R.id.btn_exit);
        TextView rulesTv = findViewById(R.id.tv_rules);

        if (titleView != null) {
            titleView.setTitleText("HARVEST RUSH");
            titleView.setStraight(false);
            titleView.setPulse(0.11f,0.85f);
        }

        if (rulesTv != null) {
            String header = getString(R.string.instructions_header);
            StringBuilder sb = new StringBuilder(header);
            try {
                String[] points = getResources().getStringArray(R.array.game_instructions);
                if(points.length>0) sb.append("\n\n");
                for (String p : points) sb.append("• ").append(p).append("\n");
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') sb.deleteCharAt(sb.length() - 1);
            } catch (Exception e){
                Log.w(TAG, "game_instructions array missing: "+ e.getMessage());
            }
            rulesTv.setText(sb.toString());
        }

        if (startGameBtn != null) startGameBtn.setOnClickListener(v -> startActivity(new Intent(this, GameActivity.class)));
        if (exitBtn != null) exitBtn.setOnClickListener(v -> finish());
    }
}
