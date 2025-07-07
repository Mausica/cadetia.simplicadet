package com.cadetia.simplicadet.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadetia.simplicadet.activities.ui.theme.SimplicadetappTheme
import com.cadetia.simplicadet.database.DbQuery
import com.cadetia.simplicadet.listeners.MyCompleteListener

class PathSelector : ComponentActivity() {
    private lateinit var learningPathPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        learningPathPrefs = getSharedPreferences("LearningPathProgress", Context.MODE_PRIVATE)

        setContent {
            SimplicadetappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PathSelectorScreen(
                        onPathSelected = { learningPath ->
                            selectLearningPath(learningPath)
                        },
                        learningPathPrefs = learningPathPrefs
                    )
                }
            }
        }
    }

    private fun selectLearningPath(learningPath: DbQuery.LearningPath) {
        // Save selected learning path to preferences
        val editor = getSharedPreferences("SelectedLearningPath", Context.MODE_PRIVATE).edit()
        editor.putString("selectedPathId", learningPath.id)
        editor.putString("selectedPathTitle", learningPath.title)
        editor.apply()

        // Set as current learning path in DbQuery
        DbQuery.g_learningPath = learningPath

        // Go back to home
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathSelectorScreen(
    onPathSelected: (DbQuery.LearningPath) -> Unit,
    learningPathPrefs: SharedPreferences
) {
    val context = LocalContext.current
    var learningPaths by remember { mutableStateOf<List<DbQuery.LearningPath>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load all learning paths
    LaunchedEffect(Unit) {
        loadAllLearningPaths { paths, errorMsg ->
            learningPaths = paths
            error = errorMsg
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Select Learning Path",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? ComponentActivity)?.finish()
                    }) {
                        Text("←", fontSize = 24.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: $error",
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                error = null
                                loadAllLearningPaths { paths, errorMsg ->
                                    learningPaths = paths
                                    error = errorMsg
                                    isLoading = false
                                }
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
                learningPaths.isEmpty() -> {
                    Text(
                        text = "No learning paths available",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(learningPaths) { learningPath ->
                            LearningPathCard(
                                learningPath = learningPath,
                                learningPathPrefs = learningPathPrefs,
                                onPathClick = { onPathSelected(learningPath) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LearningPathCard(
    learningPath: DbQuery.LearningPath,
    learningPathPrefs: SharedPreferences,
    onPathClick: () -> Unit
) {
    // Calculate progress
    val progress = remember(learningPath) {
        if (learningPath.nodes.isNullOrEmpty()) {
            0
        } else {
            val completedNodes = learningPath.nodes.count { node ->
                val nodeKey = "${learningPath.id}_${node.id}"
                learningPathPrefs.getBoolean(nodeKey, false)
            }
            (completedNodes * 100) / learningPath.nodes.size
        }
    }

    val totalNodes = learningPath.nodes?.size ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPathClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with title and progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = learningPath.title ?: "Unknown Path",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Progress circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            if (progress == 100) Color.Green.copy(alpha = 0.1f)
                            else Color.Blue.copy(alpha = 0.1f)
                        )
                ) {
                    Text(
                        text = "$progress%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (progress == 100) Color.Green else Color.Blue
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progress == 100) Color.Green else Color.Blue
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Path info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$totalNodes lessons",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Text(
                    text = when {
                        progress == 100 -> "Completed"
                        progress > 0 -> "In Progress"
                        else -> "Not Started"
                    },
                    fontSize = 12.sp,
                    color = when {
                        progress == 100 -> Color.Green
                        progress > 0 -> Color.Blue
                        else -> Color.Gray
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Helper function to load all learning paths
private fun loadAllLearningPaths(callback: (List<DbQuery.LearningPath>, String?) -> Unit) {
    // This would need to be implemented in your DbQuery class
    // For now, I'll show how it should work with the existing structure

    try {
        DbQuery.loadAllLearningPaths(object : MyCompleteListener {
            override fun onSucces() {
                // Assuming DbQuery.g_allLearningPaths would contain all paths
                val paths = DbQuery.g_allLearningPaths ?: emptyList()
                callback(paths, null)
            }

            override fun onFailure() {
                callback(emptyList(), "Failed to load learning paths")
            }
        })
    } catch (e: Exception) {
        Log.e("PathSelector", "Error loading learning paths", e)
        callback(emptyList(), "Error: ${e.message}")
    }
}