package com.example.touchlessdroid.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.touchlessdroid.domain.model.NavigationItem
import com.example.touchlessdroid.domain.model.getNavigationItems

@Composable
fun DrawerContent(currentRoute: String,onItemClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {

        //Text("Menu", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))

        val items = getNavigationItems()

        items.forEach { item->
            DrawerItem(item,currentRoute) { onItemClick(item.route) }
        }
    }
}

@Composable
fun DrawerItem(item: NavigationItem, currentRoute: String, onClick: () -> Unit) {

    NavigationDrawerItem(
        label = {
            Text(
                item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        },
        selected = currentRoute==item.route,
        onClick = { onClick() },
        icon = {
            Icon(
                imageVector = if (item.route == currentRoute) {
                    item.selectedIcon
                } else item.unselectedIcon,
                contentDescription = item.title,
                modifier = Modifier.height(24.dp).width(24.dp)
            )
        },
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding) //padding between items
    )


}