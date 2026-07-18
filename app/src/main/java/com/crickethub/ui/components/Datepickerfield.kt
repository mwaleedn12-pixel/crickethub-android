package com.crickethub.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crickethub.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minDate: Long? = null,
    maxDate: Long? = null
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date", tint = NeonGreen)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = NeonGreen,
            unfocusedBorderColor = BorderColor,
            focusedLabelColor = NeonGreen,
            unfocusedLabelColor = TextSecondary,
            cursorColor = NeonGreen
        )
    )

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (value.isNotBlank()) {
                try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(value)?.time
                } catch (e: Exception) { null }
            } else null,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val afterMin = minDate == null || utcTimeMillis >= minDate
                    val beforeMax = maxDate == null || utcTimeMillis <= maxDate
                    return afterMin && beforeMax
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        onValueChange(sdf.format(Date(millis)))
                    }
                    showPicker = false
                }) {
                    Text("OK", color = NeonGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF0D2018),
                titleContentColor = TextPrimary,
                headlineContentColor = NeonGreen,
                weekdayContentColor = TextSecondary,
                subheadContentColor = TextSecondary,
                navigationContentColor = TextPrimary,
                yearContentColor = TextPrimary,
                currentYearContentColor = NeonGreen,
                selectedYearContainerColor = NeonGreen,
                selectedYearContentColor = Color.Black,
                dayContentColor = TextPrimary,
                selectedDayContainerColor = NeonGreen,
                selectedDayContentColor = Color.Black,
                todayContentColor = NeonGreen,
                todayDateBorderColor = NeonGreen,
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }
}