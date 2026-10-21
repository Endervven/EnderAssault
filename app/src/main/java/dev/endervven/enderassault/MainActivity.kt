package dev.endervven.enderassault

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.os.SystemClock
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(5, 8, 22)
        window.navigationBarColor = Color.rgb(5, 8, 22)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(GameView())
    }
}

private data class Enemy(var x: Float, var y: Float, var speed: Float, var size: Float)
private data class Shot(var x: Float, var y: Float)

private class GameView : View {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random(System.currentTimeMillis())
    private val enemies = mutableListOf<Enemy>()
    private val shots = mutableListOf<Shot>()
    private var playerX = 0f
    private var playerY = 0f
    private var score = 0
    private var lives = 3
    private var gameOver = false
    private var lastTime = 0L
    private var spawnTimer = 0f
    private var shotTimer = 0f
    private var dragging = false

    constructor() : super(null)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val dt = if (lastTime == 0L) 0.016f else
            ((SystemClock.uptimeMillis() - lastTime) / 1000f).coerceAtMost(0.04f)
        lastTime = SystemClock.uptimeMillis()

        canvas.drawColor(Color.rgb(5, 8, 22))

        // Sterne
        paint.color = Color.rgb(100, 130, 190)
        for (i in 0 until 65) {
            val x = ((i * 137f) % width.coerceAtLeast(1))
            val y = ((i * 251f + SystemClock.uptimeMillis() * (i % 3 + 1) * 0.025f) % height.coerceAtLeast(1))
            canvas.drawCircle(x, y, if (i % 5 == 0) 2.2f else 1f, paint)
        }

        if (playerX == 0f) {
            playerX = width / 2f
            playerY = height * 0.82f
        }

        if (!gameOver) updateGame(dt, canvas)

        // Spieler-Schiff
        paint.color = Color.rgb(0, 230, 255)
        val path = android.graphics.Path().apply {
            moveTo(playerX, playerY - 30)
            lineTo(playerX - 22, playerY + 22)
            lineTo(playerX, playerY + 12)
            lineTo(playerX + 22, playerY + 22)
            close()
        }
        canvas.drawPath(path, paint)

        // Laser
        paint.color = Color.rgb(80, 255, 160)
        paint.strokeWidth = 5f
        shots.forEach {
            canvas.drawLine(it.x, it.y, it.x, it.y - 18, paint)
        }

        // Gegner
        enemies.forEach { e ->
            paint.color = Color.rgb(255, 55, 105)
            canvas.drawCircle(e.x, e.y, e.size, paint)
            paint.color = Color.rgb(255, 170, 195)
            canvas.drawCircle(e.x, e.y, e.size * 0.4f, paint)
        }

        paint.color = Color.WHITE
        paint.textSize = 26f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("SCORE  $score", 24f, 45f, paint)
        canvas.drawText("❤  $lives", 24f, 80f, paint)

        if (gameOver) {
            paint.color = Color.argb(200, 3, 6, 20)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.rgb(0, 230, 255)
            paint.textSize = 42f
            canvas.drawText("GAME OVER", width / 2f, height / 2f - 40f, paint)
            paint.color = Color.WHITE
            paint.textSize = 24f
            canvas.drawText("Punkte: $score", width / 2f, height / 2f + 5f, paint)
            canvas.drawText("Tippen zum Neustarten", width / 2f, height / 2f + 55f, paint)
        }

        postInvalidateOnAnimation()
    }

    private fun updateGame(dt: Float, canvas: Canvas) {
        spawnTimer += dt
        shotTimer += dt

        if (spawnTimer > (0.85f - (score / 500f).coerceAtMost(0.45f))) {
            spawnTimer = 0f
            enemies.add(
                Enemy(
                    random.nextFloat() * (width - 50f).coerceAtLeast(1f) + 25f,
                    -30f,
                    170f + random.nextFloat() * 110f + score * 0.15f,
                    15f + random.nextFloat() * 8f
                )
            )
        }

        if (shotTimer > 0.22f) {
            shotTimer = 0f
            shots.add(Shot(playerX, playerY - 28f))
        }

        shots.forEach { it.y -= 560f * dt }
        shots.removeAll { it.y < -30f }

        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val e = iterator.next()
            e.y += e.speed * dt

            if (abs(e.x - playerX) < e.size + 20f &&
                abs(e.y - playerY) < e.size + 20f) {
                iterator.remove()
                lives--
                if (lives <= 0) gameOver = true
                continue
            }

            if (e.y > height + 30f) {
                iterator.remove()
                lives--
                if (lives <= 0) gameOver = true
                continue
            }

            val hit = shots.firstOrNull {
                abs(it.x - e.x) < e.size + 5f &&
                    abs(it.y - e.y) < e.size + 8f
            }
            if (hit != null) {
                shots.remove(hit)
                iterator.remove()
                score += 10
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (gameOver && event.action == MotionEvent.ACTION_DOWN) {
                    enemies.clear()
                    shots.clear()
                    score = 0
                    lives = 3
                    gameOver = false
                    spawnTimer = 0f
                    shotTimer = 0f
                }
                playerX = event.x.coerceIn(25f, width - 25f)
                playerY = event.y.coerceIn(height * 0.35f, height - 45f)
                dragging = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return true
    }
}
