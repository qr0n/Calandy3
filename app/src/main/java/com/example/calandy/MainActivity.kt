package com.example.calandy

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.calandy.ui.theme.CalandyTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material.icons.rounded.Add
import java.io.File
import java.util.regex.Pattern
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.produceState
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import android.provider.MediaStore.Images.Media
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
//import androidx.compose.ui.graphics.BlendMode.Companion.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import androidx.camera.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.text.TextRecognizer
import java.io.IOException
import androidx.compose.ui.graphics.Color
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.content.edit
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.IntSize
import androidx.camera.core.ImageAnalysis
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.core.content.ContentProviderCompat.requireContext
import android.provider.CalendarContract
import java.util.Calendar
import java.util.TimeZone
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.location.Location
import android.os.Looper
import coil.compose.AsyncImage
import com.example.calandy.api.EventsApi
import com.example.calandy.model.Event


private const val CAMERA_PERMISSION_REQUEST_CODE = 100


object PermissionHelper {
    private const val PREFS_NAME = "CameraPermissionPrefs"
    private const val PERM_REQUESTED_KEY = "camera_permission_requested"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun hasRequestedPermission(context: Context): Boolean {
        return getPrefs(context).getBoolean(PERM_REQUESTED_KEY, false)
    }

    fun markPermissionRequested(context: Context) {
        getPrefs(context).edit { putBoolean(PERM_REQUESTED_KEY, true) }
    }

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}

class DateNormalizer(private val context: Context) {
    private val userLocale = context.resources.configuration.locales[0]
    private val dateFormats = mutableListOf<SimpleDateFormat>()

    init {
        // Initialize date formats based on locale
        when (userLocale.country.uppercase()) {
            "US" -> {
                dateFormats.add(SimpleDateFormat("MM/dd/yyyy", userLocale))
                dateFormats.add(SimpleDateFormat("MM-dd-yyyy", userLocale))
            }
            "GB", "IN", "AU" -> {
                dateFormats.add(SimpleDateFormat("dd/MM/yyyy", userLocale))
                dateFormats.add(SimpleDateFormat("dd-MM-yyyy", userLocale))
            }
            else -> {
                // Default to ISO format
                dateFormats.add(SimpleDateFormat("yyyy-MM-dd", userLocale))
            }
        }

        // Add common formats
        dateFormats.add(SimpleDateFormat("MMMM dd, yyyy", userLocale))
        dateFormats.add(SimpleDateFormat("MMM dd, yyyy", userLocale))
        dateFormats.add(SimpleDateFormat("dd MMM yyyy", userLocale))
    }

    fun normalizeDate(dateStr: String): String? {
        for (format in dateFormats) {
            try {
                val date = format.parse(dateStr)
                return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }
}


class CalendarHelper(private val context: Context) {
    // Update the addEventToCalendar method in CalendarHelper
    fun addEventToCalendar(title: String, dateString: String, description: String? = null) {
        try {
            // Get available calendars
            val calendarId = getDefaultCalendarId() ?: run {
                Toast.makeText(context, "No calendar available", Toast.LENGTH_SHORT).show()
                return
            }

            val dateNormalizer = DateNormalizer(context)
            val normalizedDate = dateNormalizer.normalizeDate(dateString) ?: run {
                Toast.makeText(context, "Invalid date format: $dateString", Toast.LENGTH_SHORT).show()
                return
            }

            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            calendar.time = sdf.parse(normalizedDate) ?: run {
                Toast.makeText(context, "Could not parse date", Toast.LENGTH_SHORT).show()
                return
            }

            // Set default time to noon
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.set(Calendar.MINUTE, 0)

            val startMillis = calendar.timeInMillis
            calendar.add(Calendar.HOUR, 2) // Default 2-hour duration
            val endMillis = calendar.timeInMillis

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description ?: "Event added by Calandy")
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)?.let {
                Toast.makeText(context, "Event added to calendar", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(context, "Failed to add event", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("Calendar", "Error adding event", e)
            Toast.makeText(context, "Failed to add event: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDefaultCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                if (idColumn != -1) {
                    return cursor.getLong(idColumn)
                }
            }
        }
        return null
    }
}

class TextAnalyzer(
    private val context: Context,
    private val onTextFound: (List<Text.TextBlock>, Boolean, String?) -> Unit
) : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val calendarHelper = CalendarHelper(context)

    // Expanded date patterns
    private val datePatterns = listOf(
        // Standard numeric formats
        """(\d{1,2}[-/\.]\d{1,2}[-/\.]\d{2,4})""".toRegex(),

        // Month name formats (full and abbreviated)
        """\b(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[.,]?\s+\d{1,2}[,]?\s+\d{4}\b""".toRegex(RegexOption.IGNORE_CASE),

        // Day-Month format
        """\b\d{1,2}(st|nd|rd|th)?\s+(of\s+)?(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[.,]?\s+\d{4}\b""".toRegex(RegexOption.IGNORE_CASE),

        // Special formats
        """\b(Next|This)\s+(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)\b""".toRegex(RegexOption.IGNORE_CASE),

        // Tomorrow/Today
        """\b(Tomorrow|Today|Tonight)\b""".toRegex(RegexOption.IGNORE_CASE)
    )

    private var lastProcessingTimeMs = 0L
    private val processingIntervalMs = 200L
    private var consecutiveDateDetections = 0
    private val requiredConsecutiveDetections = 2
    private var lastDetectedDate: String? = null
    private val dateNormalizer = DateNormalizer(context)

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimeMs = System.currentTimeMillis()
        if (currentTimeMs - lastProcessingTimeMs < processingIntervalMs) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnCompleteListener {
                    imageProxy.close()
                    lastProcessingTimeMs = currentTimeMs
                }
                .addOnSuccessListener { visionText ->
                    var dateFound = false
                    var detectedDate: String? = null

                    // Look for dates in each text block
                    for (block in visionText.textBlocks) {
                        val text = block.text
                        for (pattern in datePatterns) {
                            val match = pattern.find(text)
                            if (match != null) {
                                detectedDate = match.value
                                Toast.makeText(context, detectedDate.toString(), Toast.LENGTH_SHORT).show()
                                if (detectedDate == lastDetectedDate) {
                                    consecutiveDateDetections++
                                    if (consecutiveDateDetections >= requiredConsecutiveDetections) {
                                        detectedDate.let { date ->
                                            calendarHelper.addEventToCalendar(
                                                title = "Event from Poster",
                                                dateString = date,
                                                description = "Detected from poster using Calandy"
                                            )
                                        }
                                        dateFound = true
                                        break
                                    }
                                } else {
                                    consecutiveDateDetections = 1
                                    lastDetectedDate = detectedDate
                                }
                            }
                        }
                        if (dateFound) break
                    }

                    onTextFound(visionText.textBlocks, dateFound, detectedDate)
                }
                .addOnFailureListener {
                    consecutiveDateDetections = 0
                    lastDetectedDate = null
                    imageProxy.close()
                }
        }
    }
}

@Composable
fun BoundingBoxOverlay(
    blocks: List<Text.TextBlock>,
    hasDate: Boolean,
    viewSize: IntSize,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        blocks.forEach { block ->
            block.boundingBox?.let { rect ->
                val scaleX = size.width / viewSize.width.toFloat()
                val scaleY = size.height / viewSize.height.toFloat()

                drawRect(
                    color = if (hasDate)
                        Color.Green.copy(alpha = 0.6f)
                    else
                        Color.Red.copy(alpha = 0.3f),
                    topLeft = Offset(rect.left * scaleX, rect.top * scaleY),
                    size = Size(rect.width() * scaleX, rect.height() * scaleY),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun DateFoundIndicator() {
    Text(
        "Date Found!",
        modifier = Modifier
            .padding(16.dp)
            .background(
                Color.Green.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        color = Color.White
    )

}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            Log.d("MainActivity", "Starting onCreate")

            setContent {
                CalandyTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppUI()
                    }
                }
            }
            Log.d("MainActivity", "Completed onCreate")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
            throw e
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUI() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavigationBar(selectedTab) { selectedTab = it }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ScanPosterScreen()
                1 -> EventsScreen()
                2 -> SavedPostersScreen()
                3 -> SettingsScreen()
            }
        }
    }
}

@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar {
        val items = listOf(
            "Scan Poster" to Icons.Default.Add,
            "Upcoming" to Icons.Default.DateRange,
            "Saved" to Icons.Default.Favorite,
            "Settings" to Icons.Default.Settings
        )

        items.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = selectedTab == index,
                onClick = { onTabSelected(index) }
            )
        }
    }
}

private fun processImageFromUri(context: Context, uri: Uri) {
    try {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Process the recognized text
                for (block in visionText.textBlocks) {
                    Log.d("TextRecognition", "Block text: ${block.text}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("TextRecognition", "Text recognition failed: ${e.message}")
            }
    } catch (e: IOException) {
        Log.e("TextRecognition", "Error processing image: ${e.message}")
    }
}



@Composable
fun ScanPosterScreen() {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(PermissionHelper.hasCameraPermission(context))
    }

    var detectedBlocks by remember { mutableStateOf<List<Text.TextBlock>>(emptyList()) }
    var hasDate by remember { mutableStateOf(false) }
    var previewSize by remember { mutableStateOf(IntSize(0, 0)) }
    var lastUpdateTime by remember { mutableStateOf(0L) }
    val updateIntervalMs = 100L
    var showFlash by remember { mutableStateOf(false) }


    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        PermissionHelper.markPermissionRequested(context)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !PermissionHelper.hasRequestedPermission(context)) {
            launcher.launch(android.Manifest.permission.CAMERA)
        }
    }


    // Keep camera provider in remember to prevent recreation
    val cameraProvider = remember {
        ProcessCameraProvider.getInstance(context)
    }

    // Keep imageCapture instance in remember
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            val previewView = remember { PreviewView(context) }
            val preview = Preview.Builder().build()
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(
                        ContextCompat.getMainExecutor(context),
                        TextAnalyzer(context) { blocks, dateFound, detectedDate ->
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdateTime >= updateIntervalMs) {
                                detectedBlocks = blocks
                                hasDate = dateFound

                                lastUpdateTime = currentTime
                            }
                        }
                    )
                }

            DisposableEffect(previewView) {
                val provider = cameraProvider.get()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        context as LifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("Camera", "Use case binding failed", e)
                }
                onDispose {
                    provider.unbindAll()
                }
            }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            ) { view ->
                previewSize = IntSize(view.width, view.height)
            }

            BoundingBoxOverlay(
                blocks = detectedBlocks,
                hasDate = hasDate,
                viewSize = previewSize
            )

            AnimatedVisibility(
                visible = hasDate,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                DateFoundIndicator()
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                detectedBlocks.forEach { block ->
                    block.boundingBox?.let { rect ->
                        val scaleX = size.width / previewSize.width.toFloat()
                        val scaleY = size.height / previewSize.height.toFloat()

                        drawRect(
                            color = if (hasDate) Color.Green else Color.Red,
                            topLeft = Offset(rect.left * scaleX, rect.top * scaleY),
                            size = Size(rect.width() * scaleX, rect.height() * scaleY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            if (hasDate) {
                Log.d("Date block", "FOUND DATE");
//                Text(
//                    "Date Found!",
//                    modifier = Modifier
//                        .align(Alignment.TopCenter)
//                        .padding(16.dp)
//                        .background(Color.Green.copy(alpha = 0.7f))
//                        .padding(8.dp),
//                    color = Color.White
//                )
            }


            FloatingActionButton(
                onClick = {
                    showFlash = true

                    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                        .format(System.currentTimeMillis())
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                            put(Media.RELATIVE_PATH, "Pictures/Calandy")
                        }
                    }

                    val outputOptions = ImageCapture.OutputFileOptions
                        .Builder(context.contentResolver,
                            Media.EXTERNAL_CONTENT_URI,
                            contentValues)
                        .build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                Log.d("Camera", "Photo capture succeeded: ${output.savedUri}")
                                // Process the captured image here
                                output.savedUri?.let { uri ->
                                    processImageFromUri(context, uri)
                                }
                            }
                            override fun onError(exc: ImageCaptureException) {
                                Log.e("Camera", "Photo capture failed: ${exc.message}", exc)
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Take photo",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Flash effect
            if (showFlash) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.8f))
                )
                LaunchedEffect(Unit) {
                    delay(100)
                    showFlash = false
                }
            }
        } else { // <--- OVER HERE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Camera permission is required")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    launcher.launch(android.Manifest.permission.CAMERA)
                }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}
@Composable
fun EventsScreen() {
    val nearbyEvents = remember { EventsApi.getNearbyEvents() }
    val attendingEvents = remember { EventsApi.getAttendingEvents() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Events near you section
        item {
            Text(
                text = "Events Near You",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(nearbyEvents.size) { index ->
            val event = nearbyEvents[index]
            EventCard(
                event = event,
                showDistance = true,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Attending section
        item {
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                text = "Attending",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(attendingEvents.size) { index ->
            val event = attendingEvents[index]
            EventCard(
                event = event,
                showDistance = false,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    showDistance: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = event.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Date: ${event.date}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Venue: ${event.venue}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (showDistance && event.distance != null) {
                    Text(
                        text = "Distance: ${String.format("%.1f", event.distance)} miles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

private fun loadImagesFromGallery(context: Context): List<Uri> {
    val images = mutableListOf<Uri>()
    val projection = arrayOf(
        Media._ID,
        Media.DATE_ADDED
    )

    val selection = "${Media.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf("%Pictures/Calandy%")
    val sortOrder = "${Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val contentUri = Uri.withAppendedPath(
                Media.EXTERNAL_CONTENT_URI,
                id.toString()
            )
            images.add(contentUri)
        }
    }
    return images
}

@Composable
fun SavedPostersScreen() {
    val context = LocalContext.current
    val images by produceState<List<Uri>>(initialValue = emptyList()) {
        value = loadImagesFromGallery(context)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Saved Posters",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (images.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No saved posters yet.\nTime to make some memories! <3")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images.size) { index ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(images[index]),
                            contentDescription = "Saved poster ${index + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        var notificationsEnabled by remember { mutableStateOf(false) }
        var darkModeEnabled by remember { mutableStateOf(false) }

        ListItem(
            headlineContent = { Text("Notifications") },
            trailingContent = {
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
            }
        )

        ListItem(
            headlineContent = { Text("Dark Mode") },
            trailingContent = {
                Switch(
                    checked = darkModeEnabled,
                    onCheckedChange = { darkModeEnabled = it }
                )
            }
        )

        ListItem(
            headlineContent = { Text("No account since we believe in data privacy.\nEverything is localized.")},
        )
    }
}

fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
) {
    onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
        CAMERA_PERMISSION_REQUEST_CODE -> {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can update your UI here if needed
                println()
            }
        }
    }
}