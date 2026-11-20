package es.carnavalverde.agenda.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.carnavalverde.agenda.data.Tarea
import es.carnavalverde.agenda.data.enMadrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLista(vm: AgendaVM) {
    val agenda by vm.agenda.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sinc by vm.sincronizando.collectAsStateWithLifecycle()
    val ahora by vm.ahora.collectAsStateWithLifecycle()
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    val tareas = agenda.tareas.sortedBy { it.epochMs }
    val hechas = estado.hechas
    val atrasadas = tareas.filter { it.id !in hechas && it.epochMs <= ahora }
    val futuras = tareas.filter { it.id !in hechas && it.epochMs > ahora }

    Scaffold(
        containerColor = C.fondo,
        topBar = {
            TopAppBar(
                title = { Text("Agenda de redes", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { vm.ir(Pantalla.Ajustes) }) {
                        Icon(Icons.Filled.Settings, "Ajustes", tint = C.texto)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.fondo),
            )
        },
    ) { pad ->
        PullToRefreshBox(
            isRefreshing = sinc,
            onRefresh = { vm.sincronizar() },
            modifier = Modifier.padding(pad).fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (estado.ultimoFallo > 0) item {
                    Row(
                        Modifier.fillMaxWidth().background(C.tarjeta, RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.CloudOff, null, tint = C.texto2, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sin conexión · última actualización: " +
                                if (estado.ultimaSync > 0) fechaCorta(estado.ultimaSync) else "nunca",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                item { LoSiguiente(futuras.firstOrNull(), atrasadas, ahora) { vm.abrirTarea(it) } }
                item {
                    TabRow(
                        selectedTabIndex = pestana,
                        containerColor = C.fondo,
                        contentColor = C.texto,
                        indicator = { pos ->
                            TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[pestana]), color = C.verde)
                        },
                        divider = {},
                    ) {
                        listOf("Pendientes", "Hechas", "Todo").forEachIndexed { i, t ->
                            Tab(
                                selected = pestana == i, onClick = { pestana = i },
                                text = { Text(t, style = MaterialTheme.typography.labelLarge) },
                                selectedContentColor = C.verde, unselectedContentColor = C.texto2,
                            )
                        }
                    }
                }
                when (pestana) {
                    0 -> {
                        if (atrasadas.isNotEmpty()) {
                            item { Cabecera("Atrasadas", C.naranja) }
                            items(atrasadas, key = { "at-" + it.id }) { t ->
                                TarjetaTarea(t, hecha = false, atrasada = true) { vm.abrirTarea(t.id) }
                            }
                        }
                        porDias(futuras, hechas, ahora, vm)
                        if (atrasadas.isEmpty() && futuras.isEmpty()) item { Vacio("No tienes nada pendiente.") }
                    }
                    1 -> {
                        val h = tareas.filter { it.id in hechas }
                        porDias(h.sortedByDescending { it.epochMs }, hechas, ahora, vm)
                        if (h.isEmpty()) item { Vacio("Todavía no has marcado nada como hecho.") }
                    }
                    else -> {
                        porDias(tareas, hechas, ahora, vm)
                        if (tareas.isEmpty()) item { Vacio("No hay tareas. Tira hacia abajo para sincronizar.") }
                    }
                }
                if (agenda.pendientes.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)); Cabecera("Me falta de ti", C.texto) }
                    item {
                        Column(
                            Modifier.fillMaxWidth().background(C.tarjeta, RoundedCornerShape(16.dp)).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            agenda.pendientes.forEach { p ->
                                Row {
                                    Text("•  ", color = C.verde, style = MaterialTheme.typography.bodyLarge)
                                    Text(p.texto, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.porDias(lista: List<Tarea>, hechas: Set<String>, ahora: Long, vm: AgendaVM) {
    lista.groupBy { it.epochMs.enMadrid().toLocalDate() }.forEach { (dia, ts) ->
        item(key = "dia-$dia-${ts.first().id}") { Cabecera(etiquetaDia(dia), C.texto2) }
        items(ts, key = { "t-" + it.id }) { t ->
            val hecha = t.id in hechas
            TarjetaTarea(t, hecha = hecha, atrasada = !hecha && t.epochMs <= ahora) { vm.abrirTarea(t.id) }
        }
    }
}

@Composable
private fun Cabecera(texto: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        texto, style = MaterialTheme.typography.titleSmall, color = color,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun Vacio(texto: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = C.texto2)
    }
}

@Composable
private fun LoSiguiente(proxima: Tarea?, atrasadas: List<Tarea>, ahora: Long, abrir: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(C.tarjeta, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Text("Lo siguiente", style = MaterialTheme.typography.labelMedium, color = C.verde)
        Spacer(Modifier.height(6.dp))
        if (proxima == null) {
            Text("Nada programado", style = MaterialTheme.typography.headlineSmall)
        } else {
            Column(Modifier.clickable { abrir(proxima.id) }) {
                Text(proxima.titulo, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        relativo(proxima.epochMs, ahora),
                        style = MaterialTheme.typography.titleMedium, color = C.verde,
                    )
                    Text(
                        "  ·  ${fechaLarga(proxima.epochMs)}",
                        style = MaterialTheme.typography.bodyMedium, color = C.texto2,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (atrasadas.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                if (atrasadas.size == 1) "1 atrasada" else "${atrasadas.size} atrasadas",
                style = MaterialTheme.typography.labelMedium, color = C.naranja,
            )
            atrasadas.take(3).forEach { t ->
                Text(
                    "${hora(t.epochMs)}  ${t.titulo}",
                    style = MaterialTheme.typography.bodyMedium, color = C.naranja,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clickable { abrir(t.id) }.padding(vertical = 3.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TarjetaTarea(t: Tarea, hecha: Boolean, atrasada: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(C.tarjeta, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Column(Modifier.width(56.dp)) {
            Text(
                hora(t.epochMs),
                style = MaterialTheme.typography.titleMedium,
                color = when { hecha -> C.texto2; atrasada -> C.naranja; else -> C.verde },
            )
            if (hecha) Icon(Icons.Filled.CheckCircle, "Hecha", tint = C.verde, modifier = Modifier.padding(top = 6.dp).size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                t.titulo,
                style = MaterialTheme.typography.titleMedium,
                color = if (hecha) C.texto2 else C.texto,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AppChip(t.app)
                if (atrasada) Chip("Atrasada", fondo = C.naranja, color = C.fondo)
                if (t.programable && !hecha) Chip("Se puede programar ya", fondo = C.tarjeta2, color = C.verde)
                t.donde.forEach { Chip(it, color = C.texto2) }
            }
        }
    }
}
