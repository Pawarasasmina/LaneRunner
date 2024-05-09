package com.example.lanerunnergame

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import kotlin.random.Random

class GameView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val runnerBitmap = BitmapFactory.decodeResource(resources, R.drawable.car)
    private val runnerBitmap2 = BitmapFactory.decodeResource(resources, R.drawable.obstacle1)
    private val explosionBitmap = BitmapFactory.decodeResource(resources, R.drawable.explosion1)
    private val shotBitmap = BitmapFactory.decodeResource(resources, R.drawable.obstacle2)
    private val heartBitmap = BitmapFactory.decodeResource(resources, R.drawable.life)



    private val bgBitmap = BitmapFactory.decodeResource(resources, R.drawable.bg) // Load background image

    private val obstaclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var laneWidth = 0f
    private var runnerPosition = 1 // Runner starts in lane 2 out of 0-3
    private var obstacles = mutableListOf<Obstacle>()
    private var score = 0
    private var hits = 0

    private var targetRunnerPosition = runnerPosition
    private var collisionScale = 1f
    private var runnerAnimator: ValueAnimator? = null
    private var initialX = 0f
    private var previousX = 0f
    private var isSwiping = false

    private var lives = 3
    private val lifeBitmap = BitmapFactory.decodeResource(resources, R.drawable.life)

    private var isNewHighScore = false

    private val collisionSound: MediaPlayer = MediaPlayer.create(context, R.raw.crash)
    private val collisionSound1: MediaPlayer = MediaPlayer.create(context, R.raw.win)





    init {
        obstaclePaint.color = Color.RED
        spawnObstacle()
    }



    private val collisionAnimator = ValueAnimator.ofFloat(1f, 2f).apply {
        duration = 10000 // Increase animation duration to 1000 milliseconds
        repeatMode = ValueAnimator.REVERSE // Reverse the animation
        repeatCount = ValueAnimator.INFINITE // Loop the animation indefinitely
        addUpdateListener {
            val animatedValue = it.animatedValue as Float
            collisionScale = 1f + (animatedValue - 1f) * 0.5f // Adjust step size to 0.5f
            invalidate()
        }
    }


    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 300 // Animation duration in milliseconds
        addUpdateListener {
            val animatedValue = it.animatedValue as Float
            runnerPosition = ((1 - animatedValue) * runnerPosition + animatedValue * targetRunnerPosition).toInt()
            invalidate()
        }
    }
    private fun drawLives(canvas: Canvas) {
        val lifeSize = 60f // Size of the life bitmap
        val spacing = 10f // Spacing between life bitmaps
        val startX = 20f // Starting x-coordinate
        val startY = 20f // Starting y-coordinate

        for (i in 0 until lives) {
            val left = startX + i * (lifeSize + spacing)
            val top = startY
            canvas.drawBitmap(lifeBitmap, null, RectF(left, top, left + lifeSize, top + lifeSize), null)
        }
    }


    private fun startRunnerAnimation(startLane: Int, endLane: Int) {
        runnerAnimator?.cancel() // Cancel previous animation if running

        runnerAnimator = ValueAnimator.ofFloat(startLane.toFloat(), endLane.toFloat()).apply {
            duration = 500 // Increase animation duration
            interpolator = AccelerateDecelerateInterpolator() // Use AccelerateDecelerateInterpolator
            addUpdateListener { animator ->
                runnerPosition = (animator.animatedValue as Float).toInt()
                invalidate()
            }
            start()
        }
    }


    enum class ObstacleType {
        NORMAL, SHOT, HEART
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        laneWidth = w / 4.0f // Divide the width into 4 lanes
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bgBitmap.recycle()
        collisionSound.release()
        collisionSound1.release()
    }
    private fun calculateLevel(): Int {
        return score / 100 + 1 // Increase level every 100 marks
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawBitmap(bgBitmap, null, Rect(0, 0, width, height), null)


        drawLevelText(canvas) // Draw level text

        drawLives(canvas) // Draw lives on canvas
        drawScore(canvas) // Draw the score



        // Calculate scaled width and height for the runner
        val runnerWidth =runnerBitmap.width.toFloat()*0.5f
        val runnerHeight = runnerBitmap.height.toFloat()*0.11f

        // Scale the runner bitmap
        val scaledRunnerBitmap = Bitmap.createScaledBitmap(runnerBitmap, runnerWidth.toInt(), runnerHeight.toInt(), true)

        // Draw runner using scaled bitmap
        val runnerLeft = runnerPosition * laneWidth
        canvas.drawBitmap(scaledRunnerBitmap, null, RectF(runnerLeft, height - runnerHeight, runnerLeft + laneWidth, height.toFloat()), null)

        // Draw obstacles and explosions
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            if (obstacle.y > height) {
                iterator.remove()
                score += obstacle.score // Increase score based on obstacle type
            } else {
                val obstacleHeight = 150f * collisionScale // Apply animation scale to obstacle height
                obstacle.draw(canvas, laneWidth, obstaclePaint, shotBitmap, runnerBitmap2, heartBitmap, obstacleHeight)

                if(score<100){
                    obstacle.y +=20
                }
                if(score>=100){
                    obstacle.y +=30
                }
                if(score>=200){
                    obstacle.y +=35
                }
            }





            // Check for collisions
            if (obstacle.y + 150 >= height - runnerHeight && obstacle.lane == runnerPosition) {
                when (obstacle.type) {
                    ObstacleType.NORMAL -> {
                        // Play collision sound
                        collisionSound.start()
                        hits += 1
                        collisionAnimator.start() // Start maximizing animation
                        canvas.drawBitmap(explosionBitmap, null, RectF(obstacle.lane * laneWidth, height - 300f, (obstacle.lane + 1) * laneWidth, height - 150f), null)
                        iterator.remove()
                        lives-- // Decrease the number of lives
                        if (lives <= 0) {
                            endGame()
                        }
                    }
                    ObstacleType.HEART -> {
                        collisionSound1.start()
                        lives++ // Increase the number of lives
                        if (lives > 3) {
                            lives = 3 // Cap lives at 3
                        }
                        iterator.remove()
                    }
                    ObstacleType.SHOT -> {
                        // Play collision sound
                        collisionSound.start()
                        hits += 1
                        collisionAnimator.start() // Start maximizing animation
                        canvas.drawBitmap(explosionBitmap, null, RectF(obstacle.lane * laneWidth, height - 300f, (obstacle.lane + 1) * laneWidth, height - 150f), null)
                        iterator.remove()
                        lives-- // Decrease the number of lives
                        if (lives <= 0) {
                            endGame()
                        }
                    }
                }
                invalidate()  // Make sure to update the screen
            }


        }

        // Recycle the scaled bitmap to free up memory
        scaledRunnerBitmap.recycle()
        showNewHighScoreToast()

        invalidate()
    }

    private fun drawLevelText(canvas: Canvas) {
        val levelText = "Level: ${calculateLevel()}"
        val levelPaint = Paint().apply {
            color = Color.YELLOW
            textSize = 70f
            typeface = Typeface.DEFAULT_BOLD
        }

        val x = (width - levelPaint.measureText(levelText)) / 2
        val y = 120f

        canvas.drawText(levelText, x, y, levelPaint)
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                previousX = initialX
                isSwiping = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isSwiping) {
                    val currentX = event.x
                    val deltaX = currentX - previousX

                    if (Math.abs(deltaX) > 50) { // Swipe threshold
                        if (deltaX > 0 && runnerPosition < 3) {
                            runnerPosition++ // Swipe right
                            previousX = currentX
                            invalidate()
                        } else if (deltaX < 0 && runnerPosition > 0) {
                            runnerPosition-- // Swipe left
                            previousX = currentX
                            invalidate()
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                isSwiping = false
            }
        }
        return true
    }




    private fun spawnObstacle() {
        val lane = Random.nextInt(4)
        val randomValue = Random.nextFloat()

        val type = when {
            randomValue < 0.2f -> ObstacleType.SHOT // 20% chance to spawn SHOT
            randomValue < 0.3f -> ObstacleType.HEART // 10% chance to spawn HEART
            else -> ObstacleType.NORMAL
        }


        obstacles.add(Obstacle(lane, -150f, type)) // Spawn at top

        // Check for new high score during gameplay
        if (score > getHighScore()) {
            saveHighScore(score)
            isNewHighScore = true
        }



        postDelayed({ spawnObstacle() }, 800) // Spawn new obstacles periodically
    }

    private fun saveHighScore(score: Int) {
        val sharedPreferences = context.getSharedPreferences("HighScore", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("HIGH_SCORE", score)
        editor.apply()
    }
    private fun saveHighScores(score: Int) {
        val sharedPreferences = context.getSharedPreferences("HighScore", Context.MODE_PRIVATE)
        val scoresSet = sharedPreferences.getStringSet("HIGH_SCORES", setOf())?.toMutableSet() ?: mutableSetOf()
        scoresSet.add(score.toString())
        sharedPreferences.edit().putStringSet("HIGH_SCORES", scoresSet).apply()
    }

    private fun getHighScore(): Int {
        val sharedPreferences = context.getSharedPreferences("HighScore", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("HIGH_SCORE", 0)
    }

    private fun getHighScores(): List<Int> {
        val sharedPreferences = context.getSharedPreferences("HighScore", Context.MODE_PRIVATE)
        val scoresSet = sharedPreferences.getStringSet("HIGH_SCORES", setOf()) ?: setOf()
        return scoresSet.mapNotNull { it.toIntOrNull() }.sortedDescending().take(3)
    }

    private fun endGame() {
        saveHighScores(score)
        // Save high score
        if (score > getHighScore()) {
            saveHighScore(score)
            isNewHighScore = true
        }

        val intent = Intent(context, GameOverActivity::class.java)
        intent.putExtra("SCORE", score)
        intent.putExtra("HIGH_SCORE", getHighScore()) // Pass high score to GameOverActivity
        intent.putExtra("IS_NEW_HIGH_SCORE", isNewHighScore) // Pass isNewHighScore to GameOverActivity
        context.startActivity(intent)
        (context as Activity).finish()
    }

    data class Obstacle(var lane: Int, var y: Float, var type: ObstacleType = ObstacleType.NORMAL) {

        val score: Int
            get() = when (type) {
                ObstacleType.NORMAL -> 5
                ObstacleType.SHOT -> 10
                ObstacleType.HEART -> 0  // Assuming heart doesn't give a score, change this if it does
            }

        fun draw(canvas: Canvas, laneWidth: Float, paint: Paint, shotBitmap: Bitmap, runnerBitmap2: Bitmap, heartBitmap: Bitmap?, obstacleHeight: Float) {
            val x = lane * laneWidth
            when (type) {
                ObstacleType.NORMAL -> canvas.drawBitmap(shotBitmap, null, RectF(x, y, x + (laneWidth)*1.3f, y + (obstacleHeight)*1.3f), null)
                ObstacleType.SHOT -> canvas.drawBitmap(runnerBitmap2, null, RectF(x, y, x + (laneWidth)*0.7f, y + (obstacleHeight)*1.6f), null)
                ObstacleType.HEART -> heartBitmap?.let {
                    canvas.drawBitmap(it, null, RectF(x, y, x + (laneWidth)*0.5f, y + (obstacleHeight)*0.7f), null)
                }
            }
        }

    }


    private fun showNewHighScoreToast() {
        if (isNewHighScore) {
            Toast.makeText(context, "New High Score!", Toast.LENGTH_SHORT).show()
            isNewHighScore = false // Reset the flag after displaying the message
        }
    }
    private fun drawScore(canvas: Canvas) {
        val scoreText = "Score: $score"
        val scorePaint = Paint().apply {
            color = Color.WHITE
            textSize = 60f
            typeface = Typeface.DEFAULT_BOLD
        }

        // Calculate position for text (top right corner)
        val x = width - scorePaint.measureText(scoreText) - 20f
        val y = 70f

        canvas.drawText(scoreText, x, y, scorePaint)
    }



}


