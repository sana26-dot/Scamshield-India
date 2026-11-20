package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CommunityReport
import com.example.model.ReportCategory
import com.example.model.UiState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RiskHighColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommunityScreen(
    currentUserId: String,
    reportsState: UiState<List<CommunityReport>>,
    initialReportTarget: String? = null,
    onClearInitialTarget: () -> Unit = {},
    onOpenPhoneScanner: () -> Unit,
    onSubmitReport: (ReportCategory, String, String, String) -> Unit,
    onDeleteReport: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<ReportCategory?>(null) }
    var showReportSheet by remember(initialReportTarget) {
        mutableStateOf(!initialReportTarget.isNullOrBlank())
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scam Alert Community",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Community reports are user-submitted indicators, not verified legal findings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenPhoneScanner,
                        modifier = Modifier.testTag("community_phone_check_button")
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Check No.")
                    }
                }
            }

            when (reportsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item { ErrorBanner(reportsState.message) }
                }
                is UiState.Success -> {
                    val allReports = reportsState.data
                    val filtered = if (selectedFilter == null) {
                        allReports
                    } else {
                        allReports.filter { ReportCategory.fromRaw(it.category) == selectedFilter }
                    }

                    // Aggregated Trends Card
                    item {
                        GlassmorphicCard {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Aggregated Community Trends (${allReports.size} reports)",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                if (allReports.isEmpty()) {
                                    Text(
                                        text = "No community reports submitted yet. Tap '+ Report Scam' to warn others safely without sharing personal PII.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    val counts = allReports.groupingBy { ReportCategory.fromRaw(it.category) }
                                        .eachCount()
                                        .entries
                                        .sortedByDescending { it.value }
                                        .take(4)
                                    val total = allReports.size.coerceAtLeast(1)
                                    counts.forEach { (cat, count) ->
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(cat.displayLabel, style = MaterialTheme.typography.labelMedium)
                                                Text("$count", style = MaterialTheme.typography.labelSmall)
                                            }
                                            LinearProgressIndicator(
                                                progress = { count.toFloat() / total.toFloat() },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(CircleShape),
                                                color = ElectricViolet
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Filter Chips
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedFilter == null,
                                    onClick = { selectedFilter = null },
                                    label = { Text("All") }
                                )
                            }
                            items(ReportCategory.entries) { category ->
                                FilterChip(
                                    selected = selectedFilter == category,
                                    onClick = {
                                        selectedFilter = if (selectedFilter == category) null else category
                                    },
                                    label = { Text(category.displayLabel) }
                                )
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            GlassmorphicCard {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("No matching reports", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "Help protect other Indian users by submitting a sanitized report.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { report ->
                            CommunityReportCard(
                                report = report,
                                isOwner = report.userId == currentUserId,
                                onDelete = { onDeleteReport(report.id) }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showReportSheet = true },
            containerColor = ElectricViolet,
            contentColor = PureWhite,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("community_add_report_fab")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Report Scam")
                Spacer(Modifier.width(6.dp))
                Text("Report Scam", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showReportSheet) {
        val today = remember {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        var category by remember { mutableStateOf(ReportCategory.SCAM_CALL) }
        var description by remember { mutableStateOf("") }
        var target by remember(initialReportTarget) { mutableStateOf(initialReportTarget.orEmpty()) }
        var incidentDate by remember { mutableStateOf(today) }

        ModalBottomSheet(
            onDismissRequest = {
                showReportSheet = false
                onClearInitialTarget()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Submit Community Scam Report",
                    style = MaterialTheme.typography.headlineSmall
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Privacy protection: Do NOT include your own Aadhaar, bank details, OTPs, or personal address. Reports are marked as unverified community submissions.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReportCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.displayLabel, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Suspicious phone number, URL, or handle (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_target_input")
                )

                OutlinedTextField(
                    value = incidentDate,
                    onValueChange = { incidentDate = it },
                    label = { Text("Date of incident (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Describe the scam attempt (min 10 characters)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_description_input")
                )

                Button(
                    onClick = {
                        onSubmitReport(category, description, target, incidentDate)
                        showReportSheet = false
                        onClearInitialTarget()
                    },
                    enabled = description.trim().length >= 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_report_button")
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Submit Report")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CommunityReportCard(
    report: CommunityReport,
    isOwner: Boolean,
    onDelete: () -> Unit
) {
    val category = ReportCategory.fromRaw(report.category)
    GlassmorphicCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = RiskHighColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, RiskHighColor.copy(alpha = 0.4f)),
                    shape = CircleShape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = RiskHighColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${category.displayLabel} • Reported as suspicious",
                            style = MaterialTheme.typography.labelSmall,
                            color = RiskHighColor
                        )
                    }
                }
                if (isOwner) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete report",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (!report.reportedTarget.isNullOrBlank()) {
                Text(
                    text = "Target: ${report.reportedTarget}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Incident date: ${report.incidentDate} • Unverified community submission",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
