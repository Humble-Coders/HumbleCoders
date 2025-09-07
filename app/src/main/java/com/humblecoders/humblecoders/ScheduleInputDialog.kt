package com.humblecoders.humblecoders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ScheduleInputDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onGenerateSchedule: (String) -> Unit,
    courseTitle: String
) {
    var selectedTimeframe by remember { mutableStateOf("") }
    val timeframes = listOf(
        "1 week" to "1 Week (Intensive)",
        "2 weeks" to "2 Weeks (Fast-paced)",
        "1 month" to "1 Month (Standard)",
        "2 months" to "2 Months (Comfortable)",
        "3 months" to "3 Months (Leisurely)",
        "6 months" to "6 Months (Very Flexible)"
    )

    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Schedule",
                        tint = Color(0xFF4A90E2),
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Create Study Schedule",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "for $courseTitle",
                        fontSize = 16.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "How much time would you like to complete this course?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Timeframe selection
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        timeframes.forEach { (value, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selectedTimeframe == value,
                                        onClick = { selectedTimeframe = value }
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedTimeframe == value,
                                    onClick = { selectedTimeframe = value },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF4A90E2)
                                    )
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Text(
                                    text = label,
                                    fontSize = 16.sp,
                                    color = if (selectedTimeframe == value) Color(0xFF2C3E50) else Color(0xFF666666)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF666666)
                            )
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                if (selectedTimeframe.isNotEmpty()) {
                                    onGenerateSchedule(selectedTimeframe)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedTimeframe.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4A90E2)
                            )
                        ) {
                            Text(
                                text = "Generate Schedule",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
