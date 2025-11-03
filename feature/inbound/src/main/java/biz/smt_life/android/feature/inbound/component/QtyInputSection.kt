package biz.smt_life.android.feature.inbound.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import biz.smt_life.android.core.designsystem.component.HandyTextField
import biz.smt_life.android.core.domain.model.ItemDto

@Composable
fun QtyInputSection(
    selectedItem: ItemDto?,
    qtyCase: String,
    onQtyCaseChange: (String) -> Unit,
    qtyEach: String,
    onQtyEachChange: (String) -> Unit,
    expirationDate: TextFieldValue,
    onExpirationDateChange: (TextFieldValue) -> Unit,
    labelCount: String,
    onLabelCountChange: (String) -> Unit,
    fieldErrors: Map<String, String>,
    onAddEntry: () -> Unit,
    isAdding: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedItem != null) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Selected Item",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedItem.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Code: ${selectedItem.code} | Pack: ${selectedItem.packSize}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HandyTextField(
                    value = qtyCase,
                    onValueChange = onQtyCaseChange,
                    label = "Qty Case",
                    enabled = !isAdding,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = fieldErrors.containsKey("qty"),
                    errorMessage = fieldErrors["qty"],
                    modifier = Modifier.weight(1f)
                )

                HandyTextField(
                    value = qtyEach,
                    onValueChange = onQtyEachChange,
                    label = "Qty Each",
                    enabled = !isAdding,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = fieldErrors.containsKey("qty"),
                    modifier = Modifier.weight(1f)
                )
            }

            // Expiration Date with TextFieldValue for cursor control
            OutlinedTextField(
                value = expirationDate,
                onValueChange = onExpirationDateChange,
                label = { Text("Expiration Date (yyyy.mm.dd)") },
                enabled = !isAdding,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HandyTextField(
                value = labelCount,
                onValueChange = onLabelCountChange,
                label = "Label Count (0-99)",
                enabled = !isAdding,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = fieldErrors.containsKey("labelCount"),
                errorMessage = fieldErrors["labelCount"],
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onAddEntry,
                enabled = !isAdding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Add Entry")
                }
            }
        }
    }
}
