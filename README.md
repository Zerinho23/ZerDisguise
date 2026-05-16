<div align="center">

  # 🎭 ZerDisguise

  **Plugin de disfraces con GUI interactiva para servidores de Minecraft**
  Desarrollado por **zerinho23**

  [![Versión](https://img.shields.io/badge/versión-1.0.0-purple?style=for-the-badge)](https://github.com/Zerinho23/ZerDisguise/releases/latest)
  [![Paper](https://img.shields.io/badge/Paper-1.20--1.21+-blue?style=for-the-badge)](https://papermc.io)
  [![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge)](https://adoptium.net)

  </div>

  ---

  ## 📖 ¿Qué es ZerDisguise?

  ZerDisguise es un plugin que permite a los jugadores cambiarse el nombre de forma visual mediante una **GUI interactiva**, con soporte para rangos configurables, cabeza Base64 personalizada y flujo de escritura en el chat.

  ---

  ## ✨ Características

  | Característica | Descripción |
  |---|---|
  | 🖥️ **GUI interactiva** | Menú completo con cabeza del jugador e información |
  | 🧠 **Cabeza Base64** | Botón especial con textura personalizada |
  | 💬 **Flujo de chat** | El jugador escribe su disfraz directamente en el chat |
  | 👑 **Selector de rangos** | Elige entre Default, VIP, Admin (configurable) |
  | 📊 **Info completa** | Muestra rango actual, disfraz actual y disfraz anterior |
  | ☠️ **Pierde al morir** | El disfraz se elimina automáticamente al morir |
  | 🎨 **Soporte HEX** | Colores `&#RRGGBB` en mensajes y menús |
  | 🔧 **Disfrazar a otros** | Los admins pueden gestionar el disfraz de cualquier jugador |

  ---

  ## 📥 Instalación

  1. Descarga el JAR desde [Releases](https://github.com/Zerinho23/ZerDisguise/releases/latest)
  2. Colócalo en la carpeta `plugins/` de tu servidor
  3. Reinicia el servidor
  4. Edita `plugins/ZerDisguise/config.yml` a tu gusto
  5. Usa `/disguise reload` para aplicar cambios sin reiniciar

  **Requisitos:** Paper o Spigot **1.20 – 1.21+** · Java **17+**

  ---

  ## 🎮 Flujo del menú

  ```
  /disguise
     └─► Menú principal
           ├─ [Cabeza jugador]   → Muestra rango, disfraz actual y anterior
           ├─ [Cabeza Base64]    → Cierra menú + aparece título "Escribe tu disfraz"
           │                       └─► El jugador escribe el nombre en el chat
           │                             └─► Menú de confirmación
           │                                   ├─ [Cabeza del objetivo]
           │                                   ├─ [Selector de rangos]  ← elige Default/VIP/Admin
           │                                   ├─ [Cambiar nombre]      ← vuelve al chat
           │                                   ├─ [Confirmar]           ← aplica el disfraz
           │                                   └─ [Volver]              ← menú principal
           └─ [Remover disfraz]  → Quita el disfraz actual (solo si está activo)
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
  | `zerdisguise.rank.default` | Seleccionar rango Default | Todos |
  | `zerdisguise.rank.vip` | Seleccionar rango VIP | OP |
  | `zerdisguise.rank.admin` | Seleccionar rango Admin | OP |

  ---

  ## 👑 Rangos

  Los rangos son completamente configurables en `config.yml`:

  ```yaml
  ranks:
    default:
      name: "Default"
      color: "&7"
      prefix: "&7[Default]&r"
      permission: zerdisguise.rank.default
      material: STONE
    vip:
      name: "VIP"
      color: "&a"
      prefix: "&a[VIP]&r"
      permission: zerdisguise.rank.vip
      material: EMERALD
    admin:
      name: "Admin"
      color: "&c"
      prefix: "&c[Admin]&r"
      permission: zerdisguise.rank.admin
      material: NETHER_STAR
  ```

  Puedes agregar o quitar rangos libremente. El rango `default` se asigna automáticamente si el jugador no selecciona ninguno.

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
  