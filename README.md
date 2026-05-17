# ZerDisguise

  Plugin de disfraces con GUI interactiva para servidores **Paper / Spigot 1.20 – 1.21+**.

  ---

  ## ¿Qué hace?

  Permite a los jugadores disfrazarse como cualquier jugador de Minecraft: cambia su **skin**, **nombre visible** y **prefijo de rango** sin necesidad de mods en el cliente. Todo desde un menú interactivo completamente configurable.

  ---

  ## Características

  | Función | Descripción |
  |---|---|
  | 🎭 **Disfraz completo** | Cambia skin, nombre y rango visual con un clic |
  | 🏷️ **Solo rango visual** | Elige un rango sin cambiar la skin ni recibir permisos reales |
  | 👁️ **Menú GUI 54 slots** | Interfaz interactiva con paginación de jugadores online |
  | 💀 **Persistencia en muerte** | El disfraz NO se pierde al morir — solo con `/undisguise` |
  | ⚡ **Skin asíncrona** | Carga de skins de Mojang en segundo plano sin lag |
  | 🔄 **Re-aplicación en respawn** | La skin se restaura automáticamente al revivir |
  | 🔗 **LuckPerms / Vault** | Rangos reales de LuckPerms o Vault con fallback a config |
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

  ### Menú de rango visual (NUEVO en v2.3.0)
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

  1. Descarga `ZerDisguise-2.3.0.jar` de [Releases](https://github.com/Zerinho23/ZerDisguise/releases/latest)
  2. Colócalo en la carpeta `plugins/` de tu servidor
  3. Reinicia el servidor
  4. Edita `plugins/ZerDisguise/config.yml` y `menu.yml` a tu gusto
  5. Ejecuta `/disguise reload` para aplicar los cambios sin reiniciar

  ### Dependencias opcionales (recomendadas)
  - **LuckPerms** — para rangos reales y prefijos automáticos
  - **Vault + un plugin de permisos** — alternativa a LuckPerms
  - **PlaceholderAPI** — para placeholders de prefijo

  ---

  ## Compatibilidad

  - **Paper** 1.20.x, 1.21.x ✅
  - **Spigot** 1.20.x, 1.21.x ✅ (sin garantía en funciones Paper-API exclusivas)
  - **Java** 17+ requerido

  ---

  ## Historial de versiones

  ### v2.3.0
  - ✨ Nuevo **selector de rango visual** en el menú principal
  - 🔒 El disfraz **persiste tras la muerte** — solo `/undisguise` lo quita
  - 🔄 Re-aplicación automática de skin al respawnear
  - 📝 **menú de rango 100% configurable** desde `menu.yml`
  - 🐛 Corrección de indentación YAML en `menu.yml`

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
  