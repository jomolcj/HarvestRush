package com.example.harvestrush;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Set;

public class GameOverActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);
        enableImmersive();
        int score = getIntent().getIntExtra("score", 0);
        boolean completed = getIntent().getBooleanExtra("completed", false);

        CurvedJiggleTitleView title = findViewById(R.id.title_curved);
        if (title != null) {
            title.setForceHarvestRush(false);
            title.setCurvedMode(false);
            title.setBreathingEnabled(false);
            title.setTextSizeSp(46f); // reduced from 54f
            title.setTitleText(completed ? "Congragulations!!" : "Game Over!!");
        }

        TextView tvScore = findViewById(R.id.tv_final_score);
        if (tvScore != null) tvScore.setText("Final Score: " + score);

        SharedPreferences prefs = getSharedPreferences("scores", MODE_PRIVATE);
        int storedHigh = prefs.getInt("high_score", -1);
        if (storedHigh == -1) {
            Set<String> legacy = prefs.getStringSet("high_scores", null);
            if (legacy != null && !legacy.isEmpty()) {
                int max = 0;
                for (String s : legacy) {
                    try { max = Math.max(max, Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
                }
                storedHigh = max;
                prefs.edit().putInt("high_score", storedHigh).remove("high_scores").apply();
            } else {
                storedHigh = 0;
            }
        }
        boolean newHigh = false;
        if (score > storedHigh) {
            storedHigh = score;
            newHigh = true;
            prefs.edit().putInt("high_score", storedHigh).apply();
        }

        TextView tvHighScores = findViewById(R.id.tv_high_scores);
        if (tvHighScores != null) {
            String prefix = "🏆 High Score: " + storedHigh;
            if (newHigh) prefix += "  (NEW!)";
            tvHighScores.setText(prefix);
        }

        Button playAgainBtn = findViewById(R.id.btn_play_again);
        if (playAgainBtn != null) {
            playAgainBtn.setOnClickListener(v -> {
                startActivity(new Intent(this, GameActivity.class));
                finish();
            });
        }

        Button backMenuBtn = findViewById(R.id.btn_back_menu);
        if (backMenuBtn != null) {
            backMenuBtn.setOnClickListener(v -> {
                startActivity(new Intent(this, MainMenuActivity.class));
                finish();
            });
        }
    }

    private void enableImmersive() {
        Window w = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
            WindowInsetsController c = w.getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            View decor = w.getDecorView();
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decor.setSystemUiVisibility(flags);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enableImmersive();
    }
}
