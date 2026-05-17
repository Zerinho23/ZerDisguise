package me.zerith.zerdisguise;

  import org.bukkit.inventory.Inventory;
  import org.bukkit.inventory.InventoryHolder;

  /**
   * Holder personalizado para los inventarios de ZerDisguise.
   *
   * Tipos disponibles:
   *   MAIN    → menú principal (/disguise)
   *   CONFIRM → menú de confirmación (tras escribir un nombre)
   *   RANK    → menú de selección de rango visual
   */
  public class ZerInventoryHolder implements InventoryHolder {

      public enum MenuType { MAIN, CONFIRM, RANK }

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
  