package me.zerith.zerdisguise;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Holder personalizado para los inventarios de ZerDisguise.
 *
 * Permite al MenuListener identificar de forma fiable los menús del plugin
 * sin depender del título configurado en menu.yml. De esta forma el usuario
 * puede cambiar el título libremente sin romper la lógica de clics.
 *
 * Tipos disponibles:
 *   MAIN    → menú principal (/disguise)
 *   CONFIRM → menú de confirmación (tras escribir un nombre)
 */
public class ZerInventoryHolder implements InventoryHolder {

    public enum MenuType { MAIN, CONFIRM }

    private final MenuType type;
    private Inventory inventory;

    public ZerInventoryHolder(MenuType type) {
        this.type = type;
    }

    public MenuType getType() { return type; }

    @Override
    public Inventory getInventory() { return inventory; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }
}
