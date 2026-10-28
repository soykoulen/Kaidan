package com.example.kaidan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kaidan.ui.theme.KBlood
import com.example.kaidan.ui.theme.KBone
import com.example.kaidan.ui.theme.KDark
import com.example.kaidan.ui.theme.KMist
import com.example.kaidan.ui.theme.KNight
import com.example.kaidan.ui.theme.KShadow
import com.example.kaidan.ui.theme.KaidanTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KaidanTheme { KaidanApp() } }
    }
}

// ---------- Навигация ----------

enum class Dest(val title: String, val icon: ImageVector) {
    Home("Главная", Icons.Filled.Home),
    Catalog("Каталог", Icons.Filled.Search),
    Collection("Коллекция", Icons.Filled.Favorite),
    Profile("Профиль", Icons.Filled.Person)
}

@Composable
fun KaidanApp() {
    var tab by rememberSaveable { mutableStateOf(Dest.Home) }
    var detailId by rememberSaveable { mutableStateOf<Int?>(null) }   // открытая история
    var catKey by rememberSaveable { mutableStateOf<String?>(null) }  // открытая категория

    // Кнопка «назад»: сначала закрываем историю, потом категорию
    BackHandler(enabled = detailId != null || (tab == Dest.Catalog && catKey != null)) {
        if (detailId != null) detailId = null else catKey = null
    }

    Scaffold(
        containerColor = KNight,
        bottomBar = {
            NavigationBar(containerColor = KShadow) {
                Dest.values().forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t; detailId = null },
                        icon = { Icon(t.icon, contentDescription = t.title) },
                        label = { Text(t.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = KBone,
                            selectedTextColor = KBlood,
                            indicatorColor = KBlood,
                            unselectedIconColor = KMist,
                            unselectedTextColor = KMist
                        )
                    )
                }
            }
        }
    ) { padding ->
        // Внешний слой: история поверх вкладок (выезжает снизу)
        AnimatedContent(
            targetState = detailId,
            modifier = Modifier.padding(padding).fillMaxSize(),
            transitionSpec = {
                if (targetState != null)
                    (slideInVertically(tween(300)) { it / 8 } + fadeIn(tween(300))) togetherWith fadeOut(tween(200))
                else
                    fadeIn(tween(300)) togetherWith (slideOutVertically(tween(300)) { it / 8 } + fadeOut(tween(200)))
            },
            label = "detail"
        ) { id ->
            if (id != null) {
                DetailScreen(entries.first { it.id == id }) { detailId = null }
            } else {
                // Внутренний слой: плавный переход между вкладками
                AnimatedContent(
                    targetState = tab,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally(tween(350)) { it / 3 * dir } + fadeIn(tween(350))) togetherWith
                            (slideOutHorizontally(tween(350)) { -it / 3 * dir } + fadeOut(tween(200)))
                    },
                    label = "tabs"
                ) { current ->
                    when (current) {
                        Dest.Home -> HomeScreen { detailId = it.id }
                        Dest.Catalog -> CatalogScreen(catKey, { catKey = it }) { detailId = it.id }
                        Dest.Collection -> CollectionScreen()
                        Dest.Profile -> ProfileScreen()
                    }
                }
            }
        }
    }
}

// ---------- Общие элементы ----------

@Composable
fun Header(title: String, jp: String) {
    Row(
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, color = KBone, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp)
        Spacer(Modifier.width(10.dp))
        Text(jp, color = KBlood, fontSize = 18.sp, modifier = Modifier.padding(bottom = 4.dp))
    }
}

@Composable
fun FearMeter(level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { i ->
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (i < level) KBlood else KDark))
        }
    }
}

@Composable
fun KanjiBadge(kanji: String, size: Int = 56) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(KDark), contentAlignment = Alignment.Center) {
        Text(kanji, color = KBlood, fontSize = (size * 0.45).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EntryCard(e: Entry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = KShadow),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, KDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            KanjiBadge(e.kanji)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(e.title, color = KBone, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(e.subtitle, color = KMist, fontSize = 13.sp)
                if (e.fear != null) {
                    Spacer(Modifier.height(8.dp))
                    FearMeter(e.fear)
                }
            }
        }
    }
}

// ---------- Главная ----------

@Composable
fun HomeScreen(onOpen: (Entry) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("Kaidan", "怪談")
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Истории дня", color = KMist, fontSize = 14.sp) }
            items(entries.filter { it.cat != Cat.Place && it.cat != Cat.Ritual }) { EntryCard(it) { onOpen(it) } }
        }
    }
}

// ---------- Каталог ----------

@Composable
fun CatalogScreen(catKey: String?, onCat: (String?) -> Unit, onOpen: (Entry) -> Unit) {
    AnimatedContent(
        targetState = catKey,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val dir = if (targetState != null) 1 else -1
            (slideInHorizontally(tween(300)) { it / 3 * dir } + fadeIn(tween(300))) togetherWith
                (slideOutHorizontally(tween(300)) { -it / 3 * dir } + fadeOut(tween(200)))
        },
        label = "catalog"
    ) { key ->
        val cat = Cat.values().firstOrNull { it.name == key }
        if (cat == null) CategoryGrid(onCat) else CategoryList(cat, { onCat(null) }, onOpen)
    }
}

@Composable
fun CategoryGrid(onCat: (String?) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("Каталог", "目録")
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Cat.values().toList()) { cat ->
                Card(
                    onClick = { onCat(cat.name) },
                    colors = CardDefaults.cardColors(containerColor = KShadow),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, KDark)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        KanjiBadge(cat.kanji, 64)
                        Spacer(Modifier.height(12.dp))
                        Text(cat.title, color = KBone, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryList(cat: Cat, onBack: () -> Unit, onOpen: (Entry) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = KBone)
        }
        Header(cat.title, cat.kanji)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries.filter { it.cat == cat }) { EntryCard(it) { onOpen(it) } }
        }
    }
}

// ---------- Страница истории ----------

@Composable
fun DetailScreen(e: Entry, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = KBone)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            KanjiBadge(e.kanji, 72)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(e.title, color = KBone, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                Text(e.subtitle, color = KMist, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                e.cat.title, color = KBlood, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(KDark).padding(horizontal = 12.dp, vertical = 6.dp)
            )
            if (e.fear != null) {
                Spacer(Modifier.width(12.dp))
                FearMeter(e.fear)
            }
        }
        Spacer(Modifier.height(20.dp))
        e.body.split("\n\n").forEach {
            Text(it, color = KBone, fontSize = 16.sp, lineHeight = 24.sp)
            Spacer(Modifier.height(14.dp))
        }
    }
}

// ---------- Коллекция ----------

@Composable
fun CollectionScreen() {
    Column(Modifier.fillMaxSize()) {
        Header("Книга проклятий", "呪")
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            KanjiBadge("空", 96)
            Spacer(Modifier.height(16.dp))
            Text("Здесь пока пусто", color = KBone, fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Сохраняй истории, и они появятся в книге", color = KMist, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

// ---------- Профиль (имя, био и аватарка сохраняются на телефоне) ----------

fun loadAvatar(ctx: Context): ImageBitmap? {
    val f = File(ctx.filesDir, "avatar.jpg")
    return if (f.exists()) BitmapFactory.decodeFile(f.path)?.asImageBitmap() else null
}

fun saveAvatar(ctx: Context, uri: Uri) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
    val bmp = ctx.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return
    File(ctx.filesDir, "avatar.jpg").outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
}

@Composable
fun ProfileScreen() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("kaidan", Context.MODE_PRIVATE) }
    var name by remember { mutableStateOf(prefs.getString("name", "Читатель") ?: "Читатель") }
    var bio by remember { mutableStateOf(prefs.getString("bio", "Читатель ночных историй") ?: "") }
    var avatar by remember { mutableStateOf(loadAvatar(ctx)) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var draftName by remember { mutableStateOf(name) }
    var draftBio by remember { mutableStateOf(bio) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { saveAvatar(ctx, uri); avatar = loadAvatar(ctx) }
    }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = KBlood, unfocusedBorderColor = KDark,
        focusedTextColor = KBone, unfocusedTextColor = KBone,
        cursorColor = KBlood, focusedLabelColor = KBlood, unfocusedLabelColor = KMist
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Профиль", "人")
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val avatarMod = Modifier.size(110.dp).clip(CircleShape).border(2.dp, KBlood, CircleShape)
                .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            val a = avatar
            if (a != null) {
                Image(bitmap = a, contentDescription = "Аватар", contentScale = ContentScale.Crop, modifier = avatarMod)
            } else {
                Box(avatarMod.background(KDark), contentAlignment = Alignment.Center) {
                    Text(name.firstOrNull()?.uppercase() ?: "?", color = KBlood, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Нажми на фото, чтобы сменить", color = KMist, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))

            if (!editing) {
                Text(name, color = KBone, fontFamily = FontFamily.Serif, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (bio.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(bio, color = KMist, fontSize = 15.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { draftName = name; draftBio = bio; editing = true },
                    colors = ButtonDefaults.buttonColors(containerColor = KBlood, contentColor = KBone)
                ) { Text("Редактировать") }
            } else {
                OutlinedTextField(
                    value = draftName, onValueChange = { draftName = it.take(24) },
                    label = { Text("Имя") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = draftBio, onValueChange = { draftBio = it.take(120) },
                    label = { Text("О себе") }, minLines = 2, maxLines = 4, colors = fieldColors, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            name = draftName.trim().ifEmpty { "Читатель" }
                            bio = draftBio.trim()
                            prefs.edit().putString("name", name).putString("bio", bio).apply()
                            editing = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KBlood, contentColor = KBone)
                    ) { Text("Сохранить") }
                    OutlinedButton(
                        onClick = { editing = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KMist)
                    ) { Text("Отмена") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
