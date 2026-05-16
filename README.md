<div align="center">

  # 🎭 ZerDisguise

  **Plugin de disfraces con GUI interactiva para servidores de Minecraft**
  Desarrollado por **zerinho23**

  [![Versión](https://img.shields.io/badge/versión-1.3.0-purple?style=for-the-badge)](https://github.com/Zerinho23/ZerDisguise/releases/latest)
  [![Paper](https://img.shields.io/badge/Paper-1.20--1.21+-blue?style=for-the-badge)](https://papermc.io)
  [![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge)](https://adoptium.net)

  </div>

  ---

  ## 📖 ¿Qué es ZerDisguise?

  ZerDisguise es un plugin que permite a los jugadores cambiarse el nombre y la skin visualmente mediante una **GUI interactiva de 54 slots**, con soporte para rangos reales de LuckPerms/Vault, sección de jugadores conectados con paginación y flujo de escritura en el chat.

  ---

  ## ✨ Características

  | Característica | Descripción |
  |---|---|
  | 🖥️ **GUI 54 slots** | Menú rediseñado con 6 filas, bien estructurado |
  | 👥 **Jugadores online** | Sección con cabezas de todos los conectados, clic = disfraz instantáneo |
  | 📄 **Paginación** | Flechas ← → para navegar cuando hay más de 18 jugadores |
  | 👑 **Rangos reales** | Lee automáticamente los grupos de LuckPerms o Vault |
  | 🏷️ **PlaceholderAPI** | Soporte para `%luckperms_prefix%` y `%vault_prefix%` |
  | 🎨 **Skin automática** | Aplica la skin del nombre elegido vía Mojang API |
  | 💬 **Flujo de chat** | Escribe el nombre directamente en el chat |
  | 📊 **Info completa** | Muestra rango actual, disfraz actual y disfraz anterior |
  | ☠️ **Pierde al morir** | El disfraz se elimina automáticamente al morir |
  | 🔧 **Disfrazar a otros** | Los admins pueden gestionar el disfraz de cualquier jugador |
  | 🎨 **Soporte HEX** | Colores `&#RRGGBB` en mensajes y menús |

  ---

  ## 📦 Dependencias

  | Plugin | Tipo | Uso |
  |---|---|---|
  | **LuckPerms** | Recomendado | Rangos y prefijos reales de grupos |
  | **Vault** | Recomendado | Alternativa a LuckPerms para prefijos |
  | **PlaceholderAPI** | Opcional | Soporte de placeholders `%luckperms_prefix%` / `%vault_prefix%` |

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
           ├─ [Cabeza Base64]         → Prompt de chat "Escribe tu disfraz"
           │                               └─► Menú de confirmación
           │                                     ├─ [Cabeza preview]
           │                                     ├─ [Selector de rangos LP/Vault]
           │                                     ├─ [Cambiar nombre]
           │                                     ├─ [Confirmar] ← aplica nombre + skin + rango
           │                                     └─ [Volver]
           ├─ [Remover disfraz]       → Quita el disfraz actual
           ├─ [Jugadores online x18] → Clic directo = disfraz instantáneo con su skin
           └─ [← Página / Página →]  → Paginación cuando hay más de 18 jugadores
  ```

  ---

  ## 🖥️ Comandos

  | Comando | Descripción | Permiso |
  |---|---|---|
  | `/disguise` | Abre el menú de disfraces | `zerdisguise.use` |
  | `/disguise remove` | Quita el disfraz actual | `zerdisguise.use` |
  | `/disguise <jugador>` | Abre el menú de otro jugador | `zerdisguise.others` |
  | `/disguise reload` | Recarga la configuración | `zerdisguise.reload` |

  **Alias:** `/disfraz` · `/zd` · `/zerdisguise`

  ---

  ## 🔑 Permisos

  | Permiso | Descripción | Por defecto |
  |---|---|---|
  | `zerdisguise.use` | Usar el menú de disfraces | Todos |
  | `zerdisguise.others` | Abrir el menú de otro jugador | OP |
  | `zerdisguise.reload` | Recargar configuración | OP |
  | `zerdisguise.rank.*` | Seleccionar cualquier rango | OP |

  > Con LuckPerms o Vault los rangos disponibles en el menú se toman **directamente de los grupos del servidor**, sin necesidad de configurarlos en `config.yml`.

  ---

  ## 🎨 Colores en mensajes

  Todos los mensajes y títulos del menú soportan:

  ```
  Códigos &:    &d Morado   &a Verde   &c Rojo   &e Amarillo   &l Negrita
  Hex RGB:      &#FF00FF   &#00AAFF   &#FF6600
  ```

  ---

  ## 🤝 Créditos

  Desarrollado con ❤️ por **zerinho23**

  [![GitHub](https://img.shields.io/badge/GitHub-Zerinho23-black?style=flat-square&logo=github)](https://github.com/Zerinho23)
  