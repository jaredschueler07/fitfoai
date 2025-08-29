package com.runningcoach.v2.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.runningcoach.v2.R

@Composable
fun Logo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.rectangle),
        contentDescription = "Logo",
        modifier = modifier
            .requiredWidth(width = 513.dp)
            .requiredHeight(height = 512.dp)
    )
}

@Preview(widthDp = 513, heightDp = 512)
@Composable
private fun LogoPreview() {
    Logo(Modifier)
}