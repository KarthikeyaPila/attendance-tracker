package com.example.attendancetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.attendancetracker.ui.theme.AttendanceTrackerTheme
import kotlinx.coroutines.launch

data class Worker(
    val name: String,
    val monthlySalary: Int = 0,
    val daysPresent: Int = 0
)

class MainActivity : ComponentActivity() {
    private lateinit var dataStore: WorkerDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataStore = WorkerDataStore(this)

        setContent {
            AttendanceTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WorkerListScreen(dataStore = dataStore)
                }
            }
        }
    }
}

@Composable
fun WorkerListScreen(dataStore: WorkerDataStore) {
    var workers by remember { mutableStateOf<List<Worker>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val savedWorkers = dataStore.loadWorkers()
        workers = if (savedWorkers.isEmpty()) {
            val defaultWorkers = listOf(
                Worker("Ravi", 500),
                Worker("Sita", 400),
                Worker("Lakshmi", 450)
            )
            dataStore.saveWorkers(defaultWorkers)
            defaultWorkers
        } else {
            savedWorkers
        }
    }

    WorkerListUI(
        workers = workers,
        onWorkersChange = { updatedWorkers ->
            workers = updatedWorkers
            scope.launch {
                dataStore.saveWorkers(updatedWorkers)
            }
        },
        onResetAttendance = {
            val resetWorkers = workers.map { it.copy(daysPresent = 0) }
            workers = resetWorkers
            scope.launch {
                dataStore.saveWorkers(resetWorkers)
            }
        }
    )
}

@Composable
fun WorkerCard(worker: Worker, onWorkerUpdate: (Worker) -> Unit, onDelete: () -> Unit) {
    val daysPresent = worker.daysPresent
    var monthlySalaryInput by remember(worker.name) {
        mutableStateOf(worker.monthlySalary.toString())
    }

    val dailyWage = (worker.monthlySalary / 30.0).toInt()
    val finalSalary = dailyWage * daysPresent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Name: ${worker.name}",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = monthlySalaryInput,
                onValueChange = { input ->
                    monthlySalaryInput = input
                    val updatedSalary = input.toIntOrNull() ?: worker.monthlySalary
                    onWorkerUpdate(worker.copy(monthlySalary = updatedSalary))
                },
                label = { Text("Monthly Salary (₹)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Per Day Wage: ₹$dailyWage", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Days Present: $daysPresent", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Total Salary: ₹$finalSalary", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(
                    onClick = {
                        onWorkerUpdate(worker.copy(daysPresent = daysPresent + 1))
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("✔ Present", fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (daysPresent > 0) {
                            onWorkerUpdate(worker.copy(daysPresent = daysPresent - 1))
                        }
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("❌ Absent", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun WorkerListUI(
    workers: List<Worker>,
    onWorkersChange: (List<Worker>) -> Unit,
    onResetAttendance: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newSalaryInput by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showAddWorkerInputs by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Fixed top buttons area
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Attendance")
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (showAddWorkerInputs) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Worker Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newSalaryInput,
                    onValueChange = { newSalaryInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Monthly Salary (₹)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = {
                            val salary = newSalaryInput.toIntOrNull() ?: 0
                            if (newName.isNotBlank() && salary > 0 && workers.none { it.name == newName.trim() }) {
                                val updatedList = workers + Worker(name = newName.trim(), monthlySalary = salary)
                                onWorkersChange(updatedList)
                                newName = ""
                                newSalaryInput = ""
                                showAddWorkerInputs = false
                            }
                        },
                        enabled = newName.isNotBlank() && newSalaryInput.isNotBlank()
                    ) {
                        Text("Add Worker")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        newName = ""
                        newSalaryInput = ""
                        showAddWorkerInputs = false
                    }) {
                        Text("Cancel")
                    }
                }
            } else {
                Button(
                    onClick = { showAddWorkerInputs = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Worker")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LazyColumn for scrollable worker list
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(workers, key = { it.name }) { worker ->
                WorkerCard(
                    worker = worker,
                    onWorkerUpdate = { updatedWorker ->
                        val updatedList = workers.map {
                            if (it.name == updatedWorker.name) updatedWorker else it
                        }
                        onWorkersChange(updatedList)
                    },
                    onDelete = {
                        onWorkersChange(workers.filterNot { it.name == worker.name })
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirm Reset") },
                text = { Text("Are you sure you want to reset all attendance? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onResetAttendance()
                            showConfirmDialog = false
                        }
                    ) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("No")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WorkerListPreview() {
    AttendanceTrackerTheme {
        WorkerListUI(
            workers = listOf(
                Worker("Ravi", 500, 3),
                Worker("Sita", 400, 5),
                Worker("Lakshmi", 450, 2)
            ),
            onWorkersChange = {},
            onResetAttendance = {}
        )
    }
}
