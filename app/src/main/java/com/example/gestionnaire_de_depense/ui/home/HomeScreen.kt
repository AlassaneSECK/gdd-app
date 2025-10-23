package com.example.gestionnaire_de_depense.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gestionnaire_de_depense.budget.domain.BudgetEntry
import com.example.gestionnaire_de_depense.budget.domain.BudgetEntryType
import com.example.gestionnaire_de_depense.budget.domain.BudgetSummary
import com.example.gestionnaire_de_depense.budget.presentation.BudgetEffect
import com.example.gestionnaire_de_depense.budget.presentation.BudgetUiState
import com.example.gestionnaire_de_depense.budget.presentation.BudgetViewModel
import com.example.gestionnaire_de_depense.ui.theme.AppTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    sessionEmail: String? = null,
    sessionExpiresAt: Instant? = null,
    onLogout: (() -> Unit)? = null,
    budgetViewModel: BudgetViewModel
) {
    val budgetState by budgetViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val formatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM) }
    val zoneId = remember { ZoneId.systemDefault() }
    val expirationText = remember(sessionExpiresAt, formatter, zoneId) {
        sessionExpiresAt?.let { formatter.withLocale(Locale.getDefault()).withZone(zoneId).format(it) }
    }
    var remainingText by remember(sessionExpiresAt) { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionExpiresAt) {
        val target = sessionExpiresAt
        if (target == null) {
            remainingText = null
        } else {
            while (true) {
                val remaining = Duration.between(Instant.now(), target)
                val normalized = if (remaining.isNegative) Duration.ZERO else remaining
                remainingText = formatRemaining(normalized)
                if (normalized.isZero) {
                    break
                }
                delay(1_000)
            }
        }
    }

    LaunchedEffect(sessionEmail, budgetState.isInitialized, budgetState.isLoading) {
        if (sessionEmail != null && !budgetState.isInitialized && !budgetState.isLoading) {
            budgetViewModel.refresh()
        }
    }

    LaunchedEffect(budgetViewModel) {
        budgetViewModel.effects.collectLatest { effect ->
            if (effect is BudgetEffect.SessionExpired) {
                onLogout?.invoke()
            }
        }
    }

    LaunchedEffect(budgetState.errorMessage) {
        val message = budgetState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        budgetViewModel.consumeError()
    }

    HomeContent(
        modifier = modifier,
        state = budgetState,
        snackbarHostState = snackbarHostState,
        sessionEmail = sessionEmail,
        expirationText = expirationText,
        remainingText = remainingText,
        onRefresh = budgetViewModel::refresh,
        onOpenCreateEntry = budgetViewModel::onCreateEntryClick,
        onDismissCreateEntry = budgetViewModel::onDismissCreateEntry,
        onEntryTypeChange = budgetViewModel::onEntryTypeChanged,
        onEntryAmountChange = budgetViewModel::onEntryAmountChanged,
        onEntryDescriptionChange = budgetViewModel::onEntryDescriptionChanged,
        onEntryDateChange = budgetViewModel::onEntryDateChanged,
        onSubmitEntry = budgetViewModel::submitEntry,
        onLoadMore = budgetViewModel::loadMore,
        onLogout = onLogout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    modifier: Modifier,
    state: BudgetUiState,
    snackbarHostState: SnackbarHostState,
    sessionEmail: String?,
    expirationText: String?,
    remainingText: String?,
    onRefresh: () -> Unit,
    onOpenCreateEntry: () -> Unit,
    onDismissCreateEntry: () -> Unit,
    onEntryTypeChange: (BudgetEntryType) -> Unit,
    onEntryAmountChange: (String) -> Unit,
    onEntryDescriptionChange: (String) -> Unit,
    onEntryDateChange: (String) -> Unit,
    onSubmitEntry: () -> Unit,
    onLoadMore: () -> Unit,
    onLogout: (() -> Unit)?
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val listState = rememberLazyListState()
    val currentState by rememberUpdatedState(state)

    LaunchedEffect(listState, state.entries.size, state.hasMoreEntries, state.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collectLatest { index ->
                val latest = currentState
                if (index != null &&
                    latest.entries.isNotEmpty() &&
                    index >= latest.entries.lastIndex &&
                    latest.hasMoreEntries &&
                    !latest.isLoadingMore
                ) {
                    onLoadMore()
                }
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenCreateEntry,
                icon = { Icon(Icons.Default.Add, contentDescription = "Nouvelle entrée") },
                text = { Text("Nouvelle entrée") },
                expanded = true
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("Tableau de bord budget") },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.isLoading && !state.isRefreshing
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                    }
                    if (onLogout != null) {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Se déconnecter")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading && !state.isInitialized) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                state = listState
            ) {
                item {
                    SessionCard(
                        sessionEmail = sessionEmail,
                        expirationText = expirationText,
                        remainingText = remainingText
                    )
                }
                item {
                    SummaryCard(
                        summary = state.summary,
                        formatter = currencyFormatter,
                        isRefreshing = state.isRefreshing
                    )
                }
                item {
                    EntriesHeader()
                }
                if (state.entries.isEmpty()) {
                    item {
                        EmptyEntriesPlaceholder(isLoading = state.isLoadingMore || state.isLoading)
                    }
                } else {
                    items(
                        items = state.entries,
                        key = { it.id }
                    ) { entry ->
                        BudgetEntryRow(entry = entry, formatter = currencyFormatter)
                    }
                }
                if (state.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        if (state.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxWidth()
            )
        }
    }

    if (state.showCreateEntryDialog) {
        CreateEntryDialog(
            entryType = state.entryType,
            amountInput = state.entryAmountInput,
            descriptionInput = state.entryDescriptionInput,
            dateInput = state.entryDateInput,
            onTypeChange = onEntryTypeChange,
            onAmountChange = onEntryAmountChange,
            onDescriptionChange = onEntryDescriptionChange,
            onDateChange = onEntryDateChange,
            onDismiss = onDismissCreateEntry,
            onSubmit = onSubmitEntry,
            isSubmitting = state.isCreateEntryInFlight
        )
    }
}

@Composable
private fun SessionCard(
    sessionEmail: String?,
    expirationText: String?,
    remainingText: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Bienvenue !",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            sessionEmail?.let { email ->
                Text(
                    text = "Connecté en tant que $email",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            expirationText?.let { formatted ->
                Text(
                    text = "Expiration du jeton : $formatted",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            remainingText?.let { remaining ->
                Text(
                    text = "Expiration dans : $remaining",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    summary: BudgetSummary?,
    formatter: NumberFormat,
    isRefreshing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Solde disponible",
                style = MaterialTheme.typography.titleMedium
            )
            if (summary == null) {
                Text(
                    text = "--",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
                )
            } else {
                Text(
                    text = formatter.format(summary.availableAmount),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EntriesHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Historique des mouvements",
            style = MaterialTheme.typography.titleMedium
        )
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun EmptyEntriesPlaceholder(isLoading: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Aucune entrée pour le moment.")
            }
        }
    }
}

@Composable
private fun BudgetEntryRow(
    entry: BudgetEntry,
    formatter: NumberFormat
) {
    val isIncome = entry.type == BudgetEntryType.INCOME
    val amountColor = if (isIncome) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT) }
    val zoneId = remember { ZoneId.systemDefault() }
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isIncome) "Revenu" else "Dépense",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatter.format(entry.amount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = amountColor
                )
            }
            entry.occurredAt?.let { instant ->
                val formatted = dateFormatter
                    .withLocale(Locale.getDefault())
                    .withZone(zoneId)
                    .format(instant)
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            entry.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreateEntryDialog(
    entryType: BudgetEntryType,
    amountInput: String,
    descriptionInput: String,
    dateInput: String,
    onTypeChange: (BudgetEntryType) -> Unit,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle entrée") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = entryType == BudgetEntryType.EXPENSE,
                        onClick = { onTypeChange(BudgetEntryType.EXPENSE) },
                        label = { Text("Dépense") }
                    )
                    FilterChip(
                        selected = entryType == BudgetEntryType.INCOME,
                        onClick = { onTypeChange(BudgetEntryType.INCOME) },
                        label = { Text("Revenu") }
                    )
                }
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = onAmountChange,
                    label = { Text("Montant") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    minLines = 1,
                    maxLines = 4
                )
                val zoneId = remember { ZoneId.systemDefault() }
                val dateTimeFormatter = remember {
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(Locale.getDefault())
                        .withZone(zoneId)
                }
                val parsedInstant = remember(dateInput) { dateInput.toInstantOrNull() }
                var showDatePicker by remember { mutableStateOf(false) }
                val displayDate = parsedInstant?.let { dateTimeFormatter.format(it) } ?: ""
                OutlinedTextField(
                    value = displayDate,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSubmitting) { showDatePicker = true },
                    label = { Text("Date (optionnelle)") },
                    placeholder = { Text("Aucune date sélectionnée") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    readOnly = true,
                    trailingIcon = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (parsedInstant != null && !isSubmitting) {
                                IconButton(onClick = { onDateChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer la date")
                                }
                            }
                            IconButton(onClick = { if (!isSubmitting) showDatePicker = true }) {
                                Icon(Icons.Default.Event, contentDescription = "Choisir une date")
                            }
                        }
                    },
                    supportingText = {
                        val helper = parsedInstant?.let { "ISO : ${it.truncatedToSecondString()}" }
                            ?: "Laissez vide pour laisser l'API définir la date."
                        Text(helper)
                    }
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            val instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)
                            onDateChange(instant.toString())
                        },
                        label = { Text("Maintenant") },
                        enabled = !isSubmitting
                    )
                    AssistChip(
                        onClick = {
                            val todayInstant = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant()
                            onDateChange(todayInstant.truncatedTo(ChronoUnit.SECONDS).toString())
                        },
                        label = { Text("Aujourd'hui") },
                        enabled = !isSubmitting
                    )
                }
                if (showDatePicker) {
                    val initialSelection = parsedInstant
                        ?.atZone(zoneId)
                        ?.toLocalDate()
                        ?.let { it.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelection)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val selectedMillis = datePickerState.selectedDateMillis
                                    if (selectedMillis != null) {
                                        val selectedDate = Instant.ofEpochMilli(selectedMillis)
                                            .atZone(ZoneOffset.UTC)
                                            .toLocalDate()
                                        val resultInstant = selectedDate.atStartOfDay(zoneId).toInstant()
                                            .truncatedTo(ChronoUnit.SECONDS)
                                        onDateChange(resultInstant.toString())
                                    }
                                    showDatePicker = false
                                }
                            ) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Annuler") }
                        }
                    ) {
                        DatePicker(
                            state = datePickerState,
                            showModeToggle = false
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, enabled = !isSubmitting) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Annuler")
            }
        }
    )
}

private fun formatRemaining(duration: Duration): String {
    if (duration.isZero) {
        return "expiré"
    }
    val totalSeconds = duration.seconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> String.format(Locale.getDefault(), "%dh %02dm %02ds", hours, minutes, seconds)
        minutes > 0 -> String.format(Locale.getDefault(), "%dm %02ds", minutes, seconds)
        else -> String.format(Locale.getDefault(), "%ds", seconds)
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    val summary = BudgetSummary(
        userId = 42L,
        availableAmount = BigDecimal("2450.50")
    )
    val entries = listOf(
        BudgetEntry(
            id = 1,
            type = BudgetEntryType.INCOME,
            amount = BigDecimal("1500.00"),
            occurredAt = Instant.now(),
            description = "Salaire"
        ),
        BudgetEntry(
            id = 2,
            type = BudgetEntryType.EXPENSE,
            amount = BigDecimal("120.45"),
            occurredAt = Instant.now(),
            description = "Courses hebdomadaires"
        )
    )
    AppTheme {
        Surface {
            HomeContent(
                modifier = Modifier,
                state = BudgetUiState(
                    isLoading = false,
                    summary = summary,
                    entries = entries,
                    hasMoreEntries = true
                ),
                snackbarHostState = SnackbarHostState(),
                sessionEmail = "demo@example.com",
                expirationText = "10 octobre 2025 12:00",
                remainingText = "59m 32s",
                onRefresh = {},
                onOpenCreateEntry = {},
                onDismissCreateEntry = {},
                onEntryTypeChange = {},
                onEntryAmountChange = {},
                onEntryDescriptionChange = {},
                onEntryDateChange = {},
                onSubmitEntry = {},
                onLoadMore = {},
                onLogout = {}
            )
        }
    }
}

private fun String.toInstantOrNull(): Instant? = runCatching { Instant.parse(this) }.getOrNull()

private fun Instant.truncatedToSecondString(): String =
    this.truncatedTo(ChronoUnit.SECONDS).toString()
