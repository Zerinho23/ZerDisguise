# ZerDisguise

  Plugin de disfraces con GUI interactiva para servidores **Paper / Spigot 1.20 – 1.21+**.

  ---

  ## ¿Qué hace?

  Permite a los jugadores disfrazarse como cualquier jugador de Minecraft: cambia su **skin**, **nombre visible**, **prefijo de rango en el chat** y **tab/scoreboard** sin necesidad de mods en el cliente. Todo desde un menú interactivo completamente configurable.

  ---

  ## Características

  | Función | Descripción |
  |---|---|
  | 🎭 **Disfraz completo** | Cambia skin, nombre, rango visual y prefijo real en LuckPerms/Vault |
  | 🏷️ **Solo rango visual** | Elige un rango sin cambiar la skin ni recibir permisos reales |
  | 👁️ **Menú GUI 54 slots** | Interfaz interactiva con paginación de jugadores online |
  | 💀 **Persistencia en muerte** | El disfraz NO se pierde al morir — solo con `/undisguise` |
  | ⚡ **Skin asíncrona** | Carga de skins de Mojang en segundo plano sin lag |
  | 🔄 **Re-aplicación en respawn** | La skin se restaura automáticamente al revivir |
  | 🔗 **LuckPerms / Vault** | Rangos reales de LuckPerms o Vault con fallback a config |
  | 💬 **Chat sin duplicados** | El prefijo del disfraz sobreescribe el real en LP/Vault (prioridad 9999) — un solo rango en el chat |
  | 📋 **TAB / Scoreboard automático** | Compatible con TAB, NameTagEdit, CMI y otros — el disfraz aparece solo, sin configuración extra |
  | 🎨 **100% configurable** | `menu.yml` controla cada botón, material, slot, lore y título |
  | 📋 **PlaceholderAPI** | Soporte para placeholders de LP y Vault |

  ---

  ## Comandos

  | Comando | Descripción | Permiso |
  |---|---|---|
  | `/disguise` | Abre el menú de disfraces | `zerdisguise.use` |
  | `/disguise remove` | Quita el disfraz activo | `zerdisguise.use` |
  | `/disguise reload` | Recarga la configuración | `zerdisguise.reload` |
  | `/disguise <jugador>` | Abre el menú de disfraz de otro jugador | `zerdisguise.others` |
  | `/undisguise` | Quita el disfraz rápidamente | `zerdisguise.use` |
  | `/disfraz` | Alias de `/disguise` | `zerdisguise.use` |
  | `/zd` | Alias de `/disguise` | `zerdisguise.use` |

  ---

  ## Permisos

  | Permiso | Descripción | Por defecto |
  |---|---|---|
  | `zerdisguise.use` | Usar el menú de disfraces | ✅ todos |
  | `zerdisguise.others` | Abrir menú de otro jugador | OP |
  | `zerdisguise.reload` | Recargar configuración | OP |
  | `zerdisguise.bypass` | No puede ser disfrazado por otros | ❌ nadie |
  | `zerdisguise.rank.default` | Elegir el rango Default | ✅ todos |
  | `zerdisguise.rank.vip` | Elegir el rango VIP | OP |
  | `zerdisguise.rank.mod` | Elegir el rango Mod | OP |
  | `zerdisguise.rank.admin` | Elegir el rango Admin | OP |
  | `zerdisguise.rank.owner` | Elegir el rango Owner | OP |

  ---

  ## Menú interactivo

  ### Menú principal
  ```
  [Esquina] [Borde] [Borde] [Borde] [Borde] [Borde] [Borde] [Borde] [Esquina]
  [Tu cabeza] [ ] [🏷️ Rango] [ ] [✦ Escribir] [ ] [ ] [ ] [✖ Quitar]
  [──── divisor ────] [★ Jugadores en línea] [──── divisor ────]
  [ cabeza ] [ cabeza ] [ cabeza ] ... hasta 18 jugadores por página
  [ cabeza ] [ cabeza ] [ cabeza ] ... (haz clic para disfrazarte al instante)
  [Esquina] [◄] [  ] [  ] [📄 Página] [  ] [  ] [►] [Esquina]
  ```

  ### Menú de rango visual
  - Muestra **todos los grupos de LuckPerms / Vault** como ítems de cristal
  - Al hacer clic solo se aplica el **prefijo visual** — sin skin, sin permisos
  - El disfraz previo se combina con el nuevo rango automáticamente
  - Paginación si hay más de 18 rangos

  ### Menú de confirmación
  Muestra la cabeza con la skin real del jugador antes de aplicar el disfraz.

  ---

  ## Archivos de configuración

  | Archivo | Descripción |
  |---|---|
  | `config.yml` | Prefijo, rangos, mensajes, prompt del chat |
  | `menu.yml` | **Todo** el aspecto visual del menú: materiales, slots, nombres, lores, títulos |

  ### menu.yml — secciones configurables

  | Sección | Qué controla |
  |---|---|
  | `titles` | Títulos de cada menú (principal, confirmación, rangos) |
  | `design` | Materiales del marco (borde, filler, divisor, esquinas) |
  | `buttons` | Todos los botones del menú principal |
  | `confirm-menu` | Botones del menú de confirmación |
  | `rank-menu` | Etiqueta, botón volver y lore de ítems del menú de rangos |
  | `rank-glass-fallback` | Colores de cristal para cada rango (cuando no hay config específica) |

  ---

  ## Instalación

  1. Descarga `ZerDisguise-2.5.1.jar` de [Releases](https://github.com/Zerinho23/ZerDisguise/releases/latest)
  2. Colócalo en la carpeta `plugins/` de tu servidor
  3. Reinicia el servidor
  4. Edita `plugins/ZerDisguise/config.yml` y `menu.yml` a tu gusto
  5. Ejecuta `/disguise reload` para aplicar los cambios sin reiniciar

  ### Dependencias opcionales (recomendadas)
  - **LuckPerms** — para rangos reales, prefijos automáticos y chat sin duplicados
  - **Vault + un plugin de permisos** — alternativa a LuckPerms
  - **PlaceholderAPI** — para placeholders de prefijo

  ---

  ## Compatibilidad con plugins de TAB / Scoreboard

  ZerDisguise v2.5.5 es compatible de forma **automática y sin configuración** con:

  | Plugin | Compatibilidad |
  |---|---|
  | **TAB** (neznamy) | ✅ Automática — LP prefix priority 9999 |
  | **NameTagEdit** | ✅ Automática — scoreboard personal |
  | **CMI** | ✅ Automática — scoreboard personal |
  | **UltimateNameTag** | ✅ Automática — tarea de re-aplicación 2 ticks |
  | **EssentialsChat** | ✅ Automática — LP/Vault prefix override |

  ### ¿Cómo funciona?
  Al disfrazarse, ZerDisguise:
  1. **Inyecta un nodo de prefijo en LuckPerms** con prioridad 9999 — los plugins de chat y TAB leen este prefijo en lugar del real, mostrando solo el rango del disfraz sin duplicados.
  2. **Aplica el team de scoreboard** no solo en el mainScoreboard sino en el **scoreboard personal de cada jugador** — así TAB y NameTagEdit ven el prefijo correcto sobre la cabeza.
  3. **Tarea de vigilancia cada 2 ticks** — si algún plugin intenta sobreescribir el nameplate, ZerDisguise lo restaura de inmediato.

  Al quitar el disfraz, el nodo de LP se elimina y el prefijo real se restaura automáticamente.

  ---

  ## Compatibilidad

  - **Paper** 1.20.x, 1.21.x ✅
  - **Spigot** 1.20.x, 1.21.x ✅ (sin garantía en funciones Paper-API exclusivas)
  - **Java** 17+ requerido

  ---

  ## Historial de versiones

  ### v2.5.8
  - 🔍 **Diagnóstico total de TAB hook** — `TabHook.init()` ahora emite `[WARNING]` en consola con cada paso: versión de TAB detectada, clases encontradas, lista completa de métodos disponibles, y estrategia seleccionada. Si falla, indica exactamente por qué.
  - 🛡️ **Tarea periódica de seguridad** — cada 40 ticks (2 s) se re-aplica el nombre del disfraz en TAB para todos los jugadores activos. Aunque TAB resetee el nombre, se corrige automáticamente en ≤ 2 s.
  - ⏱️ **Delays correctos en apply/respawn** — en lugar de llamar `setTabName` inmediatamente tras `hidePlayer/showPlayer`, se espera 10 ticks para que TAB termine su propio ciclo de re-procesado antes de sobreescribir.
  - 🔧 **Fallback por nombre** — si la firma exacta del método `setCustomTabName` no coincide, ahora busca también por nombre + cantidad de parámetros (robustez para subversiones de TAB v6).
  - 🖥️ **Nuevo subcomando `/zd debug`** — muestra estado del hook, estrategia activa, y fuerza el nombre en TAB al momento para el jugador que lo ejecuta. Requiere permiso `zerdisguise.reload`.

  ### v2.5.7
  - 🏷️ **Fix TAB (neznamy) — nombre del disfraz en tab list** — El plugin TAB ignora `setPlayerListName()` por completo. ZerDisguise ahora hookea la API interna de TAB via reflexión para forzar el nombre del disfraz ("The_Titan19") en el tab list sin requerir ProtocolLib. Compatible con TAB v4 / v5 / v6.
  - 🔄 **Re-aplicación tras skin load y respawn** — el nombre en TAB se re-aplica después del ciclo hide/show del skin para evitar que TAB lo revierta al recibir el paquete de re-spawn de la entidad.
  - 📋 **TAB añadido a softdepend** en `plugin.yml` para garantizar el orden de carga correcto.

  ### v2.5.6
  - 🐛 **Fix crítico: config.yml y menu.yml** — corregida indentación YAML incorrecta que causaba `InvalidConfigurationException` al cargar el plugin (`expected '<document start>'` en línea 72)
  - 🔧 **Fix plugin.yml** — corregida entrada `-ProtocolLib` sin espacio en la lista `softdepend`
  - 📝 **README actualizado** — historial de versiones corregido (v2.5.5 duplicado eliminado)

  ### v2.5.5
  - 🔧 **Compatibilidad LP 5.x completa** — reemplazado `NodeMap.clear(Predicate)` (solo LP 5.4+) por `getNodes() + remove()`, compatible con **todas las versiones de LuckPerms API 5.x**
  - 🐛 Sin cambios de comportamiento respecto a v2.5.5 — solo corrección de compatibilidad interna

  ### v2.5.4
  - 💬 **Chat sin doble rango** — se inyecta un nodo de prefijo en LuckPerms con prioridad 9999 al disfrazarse, sobreescribiendo el rango real para plugins de chat (EssentialsChat, LP chat format, etc.)
  - 📋 **TAB/Scoreboard automático** — el disfraz se aplica en los scoreboards personales de cada jugador, compatible con TAB, NameTagEdit, CMI sin configuración extra
  - 🔄 **Tarea de vigilancia optimizada** — reducida de 5 a 2 ticks para responder más rápido que plugins de tab que sobreescriben el nametag
  - 👁️ **Skin visible para todos** — delay de refresh de observadores aumentado de 3 a 10 ticks, garantizando que el cliente procese el cambio correctamente
  - 🐛 **Nombre de team único** — usa UUID en lugar del nombre del jugador para evitar colisiones con nombres largos
  - 🧹 **Limpieza al desconectarse** — el nodo de LP/Vault se elimina cuando el jugador sale del servidor

  ### v2.4.0 / v2.4.1
  - Barra de acción configurable con timer
  - Fix: reinicio correcto de tarea al hacer /reload

  ### v2.3.0
  - ✨ Nuevo **selector de rango visual** en el menú principal
  - 🔒 El disfraz **persiste tras la muerte** — solo `/undisguise` lo quita
  - 🔄 Re-aplicación automática de skin al respawnear
  - 📝 **menú de rango 100% configurable** desde `menu.yml`

  ### v2.2.1
  - Menú de confirmación rediseñado
  - Soporte PlaceholderAPI para prefijos
  - Mejoras en el manejo de errores de skin

  ### v2.2.0
  - Integración con LuckPerms y Vault
  - Menú de selección de jugadores con paginación
  - Sistema de rangos configurables

  ---

  ## Autor

  Desarrollado por **zerinho23**


  ---

  ## 🔌 ProtocolLib — Integración recomendada

  ### ¿Por qué instalarlo?

  El plugin **TAB** (y otros como NameTagEdit, CMI) interceptan los paquetes del
  tab list a nivel de red, ignorando lo que Bukkit establece con `setPlayerListName()`.
  Esto hace que el tab list muestre el nombre real ("ZeroIndent") en lugar del disfraz
  ("The_Titan19").

  Con **ProtocolLib instalado**, ZerDisguise intercepta esos mismos paquetes con
  prioridad **HIGHEST** (después de TAB) y reescribe el nombre mostrado con el del
  disfraz, garantizando que el tab list siempre muestre el nombre correcto.

  ### Instalación

  1. Descarga [ProtocolLib](https://github.com/dmulloy2/ProtocolLib/releases) compatible con tu versión de Paper.
  2. Colócalo en la carpeta `plugins/`.
  3. Reinicia el servidor — ZerDisguise detecta ProtocolLib automáticamente.

  ### Sin ProtocolLib

  El plugin sigue funcionando. El nombre del disfraz se re-aplica cada ~1 segundo
  mediante un mecanismo interno que puede ser visible como un parpadeo en el tab list.
  