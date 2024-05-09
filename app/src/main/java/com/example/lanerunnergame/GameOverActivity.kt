package com.example.lanerunnergame

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
class GameOverActivity : AppCompatActivity() {

    private lateinit var lostSound: MediaPlayer // Declare MediaPlayer for lost sound



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_over)

        val score = intent.getIntExtra("SCORE", 0)
        val highScore = intent.getIntExtra("HIGH_SCORE", 0)
        val isNewHighScore = intent.getBooleanExtra("IS_NEW_HIGH_SCORE", false) // Retrieve isNewHighScore

        // Initialize lost sound MediaPlayer
        lostSound = MediaPlayer.create(this, R.raw.lost)


        val tvScore: TextView = findViewById(R.id.tvScore)
        tvScore.text = "Score: $score"

        val tvHighScore: TextView = findViewById(R.id.tvHighScore)
        tvHighScore.text = "High Score: $highScore"

        if (isNewHighScore) {
            Toast.makeText(this, "New High Score!", Toast.LENGTH_SHORT).show() // Display Toast message

        }

        val btnRestart: Button = findViewById(R.id.btnRestart)
        btnRestart.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        val homeButton = findViewById<Button>(R.id.homeButton)
        homeButton.setOnClickListener {
            // Intent to start MainActivity
            val intent = Intent(this, StartPageActivity::class.java)
            // Clear all previous activities
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
// Play lost sound when the activity is created (game over)
        lostSound.start()


    }

}