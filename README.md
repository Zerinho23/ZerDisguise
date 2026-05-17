<div align="center">

# 🎭 ZerDisguise

**Plugin de disfraces con GUI interactiva para servidores de Minecraft**
Desarrollado por **zerinho23**

[![Versión](https://img.shields.io/badge/versión-2.1.0-purple?style=for-the-badge)](https://github.com/Zerinho23/ZerDisguise/releases/latest)
[![Paper](https://img.shields.io/badge/Paper-1.20--1.21+-blue?style=for-the-badge)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge)](https://adoptium.net)

</div>

---

## 📖 ¿Qué es ZerDisguise?

ZerDisguise permite a los jugadores cambiarse el nombre y la skin visualmente mediante una **GUI interactiva de 54 slots**, con soporte para rangos reales de LuckPerms/Vault, paginación de jugadores conectados, flujo de escritura en el chat y restauración automática de apariencia.

La versión **2.1.0** corrige el bug crítico de cambio de skin (ahora usa la API nativa de Paper en lugar de reflexión NMS), agrega el comando `/undisguise`, mensajes diferenciados y limpieza completa al desconectarse.

---

## ✨ Características

| Característica | Descripción |
|---|---|
| 🖥️ **GUI 54 slots** | Menú rediseñado con estilo oscuro, bordes organizados y tooltips detallados |
| 👥 **Jugadores online** | Sección con cabezas de todos los conectados, clic = disfraz instantáneo |
| 📄 **Paginación** | Flechas ← → para navegar cuando hay más de 18 jugadores |
| 👑 **Rangos reales** | Lee automáticamente los grupos de LuckPerms o Vault |
| 🎨 **Skin por nombre** | Aplica la skin real del jugador objetivo (Paper API — sin reflexión NMS) |
| 💾 **Caché de skins** | Hasta 200 skins en caché LRU para no repetir llamadas a Mojang |
| 💬 **Flujo de chat** | Escribe el nombre directamente en el chat con validación |
| 📊 **Info completa** | Muestra rango actual, disfraz actual y disfraz anterior |
| ☠️ **Pierde al morir** | El disfraz se elimina automáticamente al morir con mensaje |
| 🚪 **Limpieza al salir** | Al desconectarse se libera toda la memoria del disfraz |
| 🔒 **Bypass** | Permiso `zerdisguise.bypass` para protegerse de ser disfrazado por admins |
| 🔧 **Disfrazar a otros** | Los admins pueden abrir el menú de cualquier jugador |
| 🎨 **Soporte HEX** | Colores `&#RRGGBB` en mensajes y menús |
| 🛡️ **Validación** | Nombres con formato Minecraft (solo letras, números, _) |

---

## 📦 Dependencias

| Plugin | Tipo | Uso |
|---|---|---|
| **LuckPerms** | Recomendado | Rangos y prefijos reales de grupos |
| **Vault** | Recomendado | Alternativa a LuckPerms para prefijos |
| **PlaceholderAPI** | Opcional | Soporte de placeholders |

> Sin ninguno de los anteriores, el plugin usa los rangos definidos en `config.yml`.

---

## 📥 Instalación

1. Descarga el JAR desde [Releases](https://github.com/Zerinho23/ZerDisguise/releases/latest)
2. Colócalo en la carpeta `plugins/` de tu servidor
3. Reinicia el servidor
4. Edita `plugins/ZerDisguise/config.yml` si lo necesitas
5. Usa `/disguise reload` para aplicar cambios sin reiniciar

**Requisitos:** Paper o Spigot **1.20 – 1.21+** · Java **17+**

---

## 🎮 Flujo del menú

```
/disguise
   └─► Menú principal (54 slots)
         ├─ [Cabeza jugador]        → Muestra rango real, disfraz actual y anterior
         ├─ [✦ Escribir nombre]     → Prompt de chat "Escribe tu disfraz"
         │                               └─► Menú de confirmación
         │                                     ├─ [Cabeza preview]
         │                                     ├─ [✦ Información del disfraz]
         │                                     ├─ [✎ Cambiar nombre]
         │                                     ├─ [✔ Confirmar] ← aplica nombre + skin + rango
         │                                     └─ [◄ Volver]
         ├─ [✖ Quitar disfraz]      → Solo aparece si tienes disfraz activo
         ├─ [Jugadores online x18] → Clic directo = disfraz instantáneo con su skin y rango
         └─ [← Pág / Pág →]        → Paginación cuando hay más de 18 jugadores
```

---

## 🎨 Diseño del menú

El menú usa un estilo de **panel informativo oscuro**:

- **Fondo**: Vidrio negro (`BLACK_STAINED_GLASS_PANE`)
- **Bordes**: Vidrio gris + esquinas cyan
- **Divisor**: Vidrio morado separando la sección de jugadores
- **Tooltips**: Estilo `┌─────┐ │ info │ └─────┘` con información organizada
- **Brillo**: Items activos/confirmados tienen brillo mágico

---

## 🖥️ Comandos

| Comando | Descripción | Permiso |
|---|---|---|
| `/disguise` | Abre el menú de disfraces | `zerdisguise.use` |
| `/disguise remove` | Quita el disfraz actual | `zerdisguise.use` |
| `/undisguise` | Quita el disfraz (atajo rápido) | `zerdisguise.use` |
| `/disguise <jugador>` | Abre el menú de otro jugador | `zerdisguise.others` |
| `/disguise reload` | Recarga la configuración | `zerdisguise.reload` |

**Alias de `/disguise`:** `/disfraz` · `/zd` · `/zerdisguise`
**Alias de `/undisguise`:** `/quitardisfraz` · `/removedisguise`

---

## 🔑 Permisos

| Permiso | Descripción | Por defecto |
|---|---|---|
| `zerdisguise.use` | Usar el menú de disfraces | Todos |
| `zerdisguise.others` | Abrir el menú de otro jugador | OP |
| `zerdisguise.reload` | Recargar configuración | OP |
| `zerdisguise.bypass` | No puede ser disfrazado por admins | false |
| `zerdisguise.rank.*` | Seleccionar cualquier rango | OP |

---

## 🎨 Colores en mensajes

Todos los mensajes y títulos del menú soportan:

```
Códigos &:    &d Morado   &a Verde   &c Rojo   &e Amarillo   &l Negrita
Hex RGB:      &#FF00FF   &#00AAFF   &#FF6600
```

---

## 📋 Changelog

### v2.1.0
- **Corrección crítica**: skin ahora se aplica correctamente usando la API nativa de Paper (`PlayerProfile` + `ProfileProperty`) en lugar de reflexión NMS frágil que fallaba silenciosamente en 1.20.4+
- Nuevo comando `/undisguise` (alias: `/quitardisfraz`, `/removedisguise`) para quitar disfraz rápidamente
- Mensaje informativo al morir con disfraz activo (`disguise-death` en config.yml)
- Limpieza completa al desconectarse (libera estado de disfraz y caché de skin — evita fuga de memoria)
- Mensajes diferenciados: si la skin falla pero el nombre sí cambia, se notifica correctamente
- Permiso `zerdisguise.bypass` para proteger jugadores de ser disfrazados por admins
- Tab completion respeta el permiso bypass (oculta jugadores protegidos)
- Migración de `AsyncPlayerChatEvent` (deprecated) al moderno `AsyncChatEvent` de Paper
- Eliminado código muerto del selector de rangos manual (ya auto-detectado desde v2.0)
- Métodos utilitarios `isAwaiting()` y `cancelAwait()` en ChatListener
- Mensaje si se intenta quitar un disfraz que no existe

### v2.0.0
- Rediseño completo del menú (estilo panel oscuro con tooltips enriquecidos)
- Skin por nombre directamente sin paso intermedio de UUID lookup
- Caché LRU de 200 skins para evitar llamadas repetidas a Mojang
- Validación de nombres en el chat
- Prevención de drag en inventarios del plugin
- Rango auto-detectado desde LuckPerms

### v1.3.0
- Versión inicial pública

---

## 🤝 Créditos

Desarrollado con ❤️ por **zerinho23**

[![GitHub](https://img.shields.io/badge/GitHub-Zerinho23-black?style=flat-square&logo=github)](https://github.com/Zerinho23)
