package com.circuitsense.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.DeleteSweep
import com.circuitsense.data.CircuitHistoryManager
import com.circuitsense.data.SampleCircuits
import com.circuitsense.data.SavedCircuitItem
import com.circuitsense.model.CircuitGraph
import com.circuitsense.recognition.CircuitRecognizer
import com.circuitsense.renderer.CharacterSprite
import com.circuitsense.renderer.SparkyExpression
import com.circuitsense.ui.theme.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Screen 1 — Capture / Home Screen (Senior Product Standard).
 * Built with proper system insets (statusBarsPadding, navigationBarsPadding),
 * rich hero showcase, vibrant curated preset cards, and on-device ML Kit camera scanner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCaptureScreen(
    onCircuitReady: (CircuitGraph, Boolean, String?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val recognizer = remember { CircuitRecognizer() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission optional — presets available below!", Toast.LENGTH_LONG).show()
        }
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Center Area: Live Camera Preview OR Animated Showcase Hero Card
        if (hasCameraPermission) {
            // Live Camera Viewfinder
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )

            // Animated Laser Reticle on active camera
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 300.dp, height = 240.dp)
                    .border(2.5.dp, ElectricBlue.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Align Ohm's Law Circuit (V = IR)",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        } else {
            // High-End Interactive Circuit Showcase Hero (Solves empty black screen)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 110.dp, bottom = 260.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = CardDark,
                    border = BorderStroke(1.5.dp, CardElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Animated Sparky Mini Circuit Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val loopPath = Path().apply {
                                    moveTo(w * 0.2f, h * 0.5f)
                                    quadraticBezierTo(w * 0.2f, h * 0.15f, w * 0.5f, h * 0.15f)
                                    quadraticBezierTo(w * 0.8f, h * 0.15f, w * 0.8f, h * 0.5f)
                                    quadraticBezierTo(w * 0.8f, h * 0.85f, w * 0.5f, h * 0.85f)
                                    quadraticBezierTo(w * 0.2f, h * 0.85f, w * 0.2f, h * 0.5f)
                                }
                                drawPath(
                                    path = loopPath,
                                    color = Color(0x332EC5FF),
                                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                                )
                                drawPath(
                                    path = loopPath,
                                    color = Color(0xFF475569),
                                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                                )

                                // Sparky Mascot
                                CharacterSprite.draw(
                                    drawScope = this,
                                    position = Offset(w * 0.5f, h * 0.15f),
                                    motionProgress = 0.5f,
                                    expression = SparkyExpression.EXCITED,
                                    speedFactor = 1.0f
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Transform Schematics into Motion",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Point camera at any drawn circuit or launch one of our verified textbook presets below.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = BackgroundDark
                            )
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable Camera Scanner", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Top Header Bar with Safe Status Bar Insets (Solves notch/clock overlapping)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x332EC5FF),
                    border = BorderStroke(1.dp, ElectricBlue),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⚡", fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "CircuitSense",
                        color = ElectricBlue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "AI Motion-Graphics Physics Tutor",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Study History / Gallery Button
                IconButton(onClick = { showHistorySheet = true }) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Study History",
                        tint = ElectricBlue
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, WarmAmber.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "V = IR MVP",
                        color = WarmAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Multi-Subject Study Bar (Physics, Mathematics, Chemistry)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ElectricBlue
                ) {
                    Text(
                        text = "⚡ Physics (Active)",
                        color = BackgroundDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, CardElevated),
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "📐 Mathematics: Geometry & Vector diagrams in Phase 2! Physics active.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        text = "📐 Maths (Soon)",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, CardElevated),
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "🧪 Chemistry: Molecular bond diagrams in Phase 2! Physics active.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        text = "🧪 Chemistry (Soon)",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Bottom Controls Container with Safe Navigation Bar Insets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, BackgroundDark.copy(alpha = 0.95f), BackgroundDark)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Curated Presets Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Curated Textbook Presets",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Instant 60 FPS",
                    color = SuccessGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rich Glassmorphic Preset Cards (4 Presets)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SampleCircuits.items) { sample ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = CardDark,
                        border = BorderStroke(1.dp, CardElevated),
                        modifier = Modifier.clickable {
                            onCircuitReady(sample.graph, false, null)
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .width(155.dp)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = sample.title,
                                color = ElectricBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${sample.graph.formula.V.toInt()}V ÷ ${sample.graph.formula.R.toInt()}Ω",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BackgroundDark
                            ) {
                                Text(
                                    text = "I = ${sample.graph.formula.I} A",
                                    color = SuccessGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shutter Button Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        val capture = imageCapture
                        if (capture != null && !isAnalyzing) {
                            isAnalyzing = true
                            val executor = Executors.newSingleThreadExecutor()
                            capture.takePicture(
                                executor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bitmap = imageProxyToBitmap(image)
                                        image.close()
                                        coroutineScope.launch {
                                            val result = recognizer.analyzeCircuitImage(bitmap)
                                            isAnalyzing = false
                                            onCircuitReady(result.graph, result.isFallbackUsed, result.fallbackReason)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isAnalyzing = false
                                        coroutineScope.launch {
                                            Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        } else {
                            // If camera not ready, launch permission or fallback to default sample
                            if (!hasCameraPermission) {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                onCircuitReady(
                                    SampleCircuits.defaultSample.graph,
                                    true,
                                    "Showing reference 9V, 100Ω circuit"
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(ElectricBlue, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan Circuit",
                        tint = BackgroundDark,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap to Scan Diagram with ML Kit OCR",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        // Loading overlay during OCR / CV analysis
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC0D1321)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ElectricBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing Circuit Diagram...",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Parsing components & calculating Ohm's Law JSON",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Study History / Saved Simulations Sheet (per user request)
    if (showHistorySheet) {
        val historyList = remember { CircuitHistoryManager.getHistory(context) }
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = CardDark,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = ElectricBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saved Study History", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = {
                            CircuitHistoryManager.clearHistory(context)
                            showHistorySheet = false
                            Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved circuits yet.\nSave any scanned circuit from the Overview screen to review it here!",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        historyList.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = BackgroundDark,
                                border = BorderStroke(1.dp, CardElevated),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showHistorySheet = false
                                        try {
                                            val graph = CircuitGraph.fromJson(item.graphJson)
                                            onCircuitReady(graph, false, null)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to load saved circuit", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(item.title, color = ElectricBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("${item.voltage.toInt()}V ÷ ${item.resistance.toInt()}Ω = ${item.current}A", color = SuccessGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        Text(item.dateFormatted, color = TextMuted, fontSize = 10.sp)
                                    }
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Load", tint = ElectricBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planeProxy = image.planes[0]
    val buffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
