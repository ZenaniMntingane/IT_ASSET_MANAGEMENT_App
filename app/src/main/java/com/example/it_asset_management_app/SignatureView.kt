package com.example.it_asset_management_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.graphics.Path

class SignatureView  constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    // ================= SIGNATURE PATH =================

    private val signaturePath = Path()


    // ================= SIGNATURE PAINT =================

    private val signaturePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }


    // ================= SIGNATURE LINE =================

    private val linePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 2f
        isAntiAlias = true
    }


    // ================= SIGNATURE STATUS =================

    private var signed = false


    // ============================================================
    // DRAW SIGNATURE
    // ============================================================

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        // White background
        canvas.drawColor(Color.WHITE)

        // Draw the user's signature
        canvas.drawPath(
            signaturePath,
            signaturePaint
        )

        // Draw signature line
        canvas.drawLine(
            20f,
            height - 30f,
            width - 20f,
            height - 30f,
            linePaint
        )
    }


    // ============================================================
    // TOUCH EVENTS
    // ============================================================

    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {

                signaturePath.moveTo(
                    event.x,
                    event.y
                )

                signed = true

                invalidate()

                return true
            }


            MotionEvent.ACTION_MOVE -> {

                signaturePath.lineTo(
                    event.x,
                    event.y
                )

                invalidate()

                return true
            }


            MotionEvent.ACTION_UP -> {

                invalidate()

                return true
            }
        }

        return true
    }


    // ============================================================
    // CLEAR SIGNATURE
    // ============================================================

    fun clearSignature() {

        signaturePath.reset()

        signed = false

        invalidate()
    }


    // ============================================================
    // CHECK IF SIGNED
    // ============================================================

    fun hasSignature(): Boolean {

        return signed
    }


    // ============================================================
    // GET SIGNATURE AS BITMAP
    // ============================================================

    fun getSignatureBitmap(): Bitmap {

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        // White background
        canvas.drawColor(Color.WHITE)

        // Draw signature
        canvas.drawPath(
            signaturePath,
            signaturePaint
        )

        return bitmap
    }
}