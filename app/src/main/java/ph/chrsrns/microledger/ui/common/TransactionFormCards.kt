package ph.chrsrns.microledger.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ph.chrsrns.microledger.R

@Composable
fun TransactionHeaderCard(
    formattedDate: String,
    status: String?,
    code: String?,
    payee: String?,
    note: String?,
    possiblePayees: List<String>,
    possibleNotes: List<String>,
    onDateChange: (Long) -> Unit,
    onStatusChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onPayeeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                DateField(
                    formattedDate = formattedDate,
                    onDateChange = onDateChange,
                    modifier = Modifier.weight(0.35f),
                )
                if (status != null) {
                    StatusSelector(
                        status = status,
                        onStatusChange = onStatusChange,
                        modifier = Modifier.weight(0.15f),
                    )
                }
                if (code != null) {
                    CodeField(
                        code = code,
                        onCodeChange = onCodeChange,
                        modifier = Modifier.weight(0.5f),
                    )
                }
            }
            if (payee != null) {
                PayeeSelector(
                    payee = payee,
                    options = possiblePayees,
                    onPayeeChange = onPayeeChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (note != null) {
                NoteSelector(
                    note = note,
                    options = possibleNotes,
                    onNoteChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun DateField(
    formattedDate: String,
    onDateChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var dateDialogOpen by remember { mutableStateOf(false) }
    val initialMillis = remember(formattedDate) { dateFormat.parse(formattedDate)?.time }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    OutlinedTextField(
        value = formattedDate,
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        label = {
            Text(
                stringResource(R.string.date),
                maxLines = 1,
            )
        },
        textStyle = LocalTextStyle.current,
        modifier =
            modifier
                .onFocusChanged {
                    if (it.isFocused) {
                        dateDialogOpen = true
                    }
                },
    )

    if (dateDialogOpen) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { dateDialogOpen = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateChange(it) }
                        dateDialogOpen = false
                        focusManager.clearFocus()
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }
}
