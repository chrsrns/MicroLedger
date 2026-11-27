package ph.chrsrns.microledger.ui.preferences

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType

/** One presence toggle inside a [SettingToggleMenu] dropdown. */
data class ToggleItem(
    val addLabelRes: Int,
    val removeLabelRes: Int,
    val present: Boolean,
    val onToggle: () -> Unit,
)

/** Preference row that opens a dropdown of labelled choices. */
@Composable
fun <T> SettingChoice(
    titleRes: Int,
    currentLabel: String,
    options: Map<T, String>,
    onSelect: (T) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Setting(
            stringResource(titleRes),
            currentLabel,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        ) { expanded = true }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize(true),
        ) {
            options.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(key)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/** Preference row that opens a dropdown of add/remove presence toggles. */
@Composable
fun SettingToggleMenu(
    titleRes: Int,
    summary: String,
    items: List<ToggleItem>,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Setting(
            stringResource(titleRes),
            summary,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        ) { expanded = true }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize(true),
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (item.present) item.removeLabelRes else item.addLabelRes,
                            ),
                        )
                    },
                    onClick = {
                        item.onToggle()
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/** Preference row that opens a single-textfield dialog. */
@Composable
fun SettingTextDialog(
    titleRes: Int,
    dialogTitleRes: Int,
    value: String,
    onSave: (String) -> Unit,
    filter: (String) -> Boolean = { true },
    numeric: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(value) }
    Setting(stringResource(titleRes), value) { open = true }
    SettingDialog(
        open,
        dialogTitleRes,
        canSave = filter(draft) && (!numeric || draft.isNotEmpty()),
        save = { onSave(draft) },
        dismiss = { open = false },
    ) {
        OutlinedTextField(
            draft,
            { if (it.isEmpty() || filter(it)) draft = it },
            keyboardOptions =
                if (numeric) {
                    KeyboardOptions(keyboardType = KeyboardType.Number)
                } else {
                    KeyboardOptions.Default
                },
        )
    }
}

/** Preference row that opens a prefix-list dialog. */
@Composable
fun SettingPrefixList(
    titleRes: Int,
    dialogTitleRes: Int,
    prefixes: List<String>,
    onSave: (List<String>) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Setting(stringResource(titleRes), prefixes.joinToString(", ")) { open = true }
    PrefixListDialog(open, dialogTitleRes, prefixes, onSave, { open = false })
}
