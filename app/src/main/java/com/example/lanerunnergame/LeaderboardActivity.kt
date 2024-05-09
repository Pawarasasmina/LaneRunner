package com.example.lanerunnergame
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LeaderboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val highScores = getHighScores() // Retrieve the highest 3 scores
        Log.d("LeaderboardActivity", "High Scores: $highScores") // Log the retrieved scores

        val textView1: TextView = findViewById(R.id.textView)
        val textView2: TextView = findViewById(R.id.textView3)
        val textView3: TextView = findViewById(R.id.textView4)

        // Update the TextViews with the highest 3 scores
        textView1.text = "${highScores.getOrElse(0) { 0 }}"
        textView2.text = "${highScores.getOrElse(1) { 0 }}"
        textView3.text = "${highScores.getOrElse(2) { 0 }}"

        val btnRestart: Button = findViewById(R.id.homebtn)
        btnRestart.setOnClickListener {
            startActivity(Intent(this, StartPageActivity::class.java))
            finish()
        }
    }

    private fun getHighScores(): List<Int> {
        val sharedPreferences = getSharedPreferences("HighScore", MODE_PRIVATE)
        val scoresSet = sharedPreferences.getStringSet("HIGH_SCORES", setOf()) ?: setOf()
        val scoresList = scoresSet.mapNotNull { it.toIntOrNull() }
        val sortedScores = scoresList.sortedDescending().take(3)
        Log.d("LeaderboardActivity", "Sorted Scores: $sortedScores") // Log the sorted scores
        return sortedScores
    }


}
