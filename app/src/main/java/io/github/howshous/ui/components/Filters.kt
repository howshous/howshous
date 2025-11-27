package io.github.howshous.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.InputShape
import io.github.howshous.ui.theme.inputColors

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, "Clear")
                }
            }
        },
        shape = InputShape,
        colors = inputColors(),
        singleLine = true
    )
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedFilterChip(
        selected = selected,
        onClick = { onSelectedChange(!selected) },
        label = { Text(label) },
        modifier = modifier
    )
}

@Composable
fun PriceRangeSlider(
    minPrice: Int,
    maxPrice: Int,
    onRangeChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Price Range", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("₱${minPrice / 1000}K", style = MaterialTheme.typography.bodySmall)
            Text("₱${maxPrice / 1000}K", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun LocationFilter(
    selectedLocations: List<String>,
    availableLocations: List<String>,
    onLocationToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Locations", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        availableLocations.forEach { location ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = location in selectedLocations,
                    onCheckedChange = { onLocationToggle(location) }
                )
                Spacer(Modifier.width(8.dp))
                Text(location, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
