package com.zornoely.crates;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class Main extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<String, CrateData> crates = new HashMap<>();
    private final Map<Location, String> placedCrates = new HashMap<>();
    private final Map<Location, List<ArmorStand>> holograms = new HashMap<>();
    private final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&8[&bZornoEly&8] &7");

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("kasa") != null) {
            getCommand("kasa").setExecutor(this);
        }
        loadData();
        getLogger().info("ZornoCrates Kasa Sistemi aktif edildi!");
    }

    @Override
    public void onDisable() {
        saveData();
        for (List<ArmorStand> stands : holograms.values()) {
            if (stands != null) {
                for (ArmorStand stand : stands) {
                    if (stand != null) stand.remove();
                }
            }
        }
        getLogger().info("ZornoCrates devre disi birakildi!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bu komutu sadece oyuncular kullanabilir.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&l--- &bZornoEly Crate Yardim &8&l---"));
            player.sendMessage(ChatColor.YELLOW + "/kasa olustur <isim> " + ChatColor.WHITE + "- Yeni kasa tanimlar");
            player.sendMessage(ChatColor.YELLOW + "/kasa al <isim> " + ChatColor.WHITE + "- Kasa sandigini alirsin");
            player.sendMessage(ChatColor.YELLOW + "/kasa anahtarver <oyuncu> <isim> <adet> " + ChatColor.WHITE + "- Anahtar verir");
            player.sendMessage(ChatColor.YELLOW + "/kasa yonet <isim> " + ChatColor.WHITE + "- Kasa ayarlari GUI");
            return true;
        }

        if (args[0].equalsIgnoreCase("olustur")) {
            if (args.length < 2) {
                player.sendMessage(PREFIX + ChatColor.RED + "Kullanim: /kasa olustur <isim>");
                return true;
            }
            String name = args[1].toLowerCase();
            if (crates.containsKey(name)) {
                player.sendMessage(PREFIX + ChatColor.RED + "Bu isimde zaten bir kasa var!");
                return true;
            }

            ItemStack defaultKey = new ItemStack(Material.TRIPWIRE_HOOK);
            ItemMeta km = defaultKey.getItemMeta();
            if (km != null) {
                km.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&8[&7" + args[1].toUpperCase() + " KASA ANAHTARI&8]"));
                defaultKey.setItemMeta(km);
            }

            CrateData crate = new CrateData(args[1], Material.CHEST, "KIRMIZI", defaultKey, new ArrayList<>());
            crates.put(name, crate);
            saveData();
            player.sendMessage(PREFIX + ChatColor.GREEN + "'" + args[1] + "' kasasi ZornoEly altyapisiyla olusturuldu!");
            return true;
        }

        if (args[0].equalsIgnoreCase("al")) {
            if (args.length < 2) {
                player.sendMessage(PREFIX + ChatColor.RED + "Kullanim: /kasa al <isim>");
                return true;
            }
            String name = args[1].toLowerCase();
            CrateData crate = crates.get(name);
            if (crate == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Böyle bir kasa bulunamadı!");
                return true;
            }

            ItemStack item = new ItemStack(crate.blockMaterial);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&l" + crate.displayName + " Kasası"));
                meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&', "&7Yere koyarak kasayi aktif et!")));
                item.setItemMeta(meta);
            }
            player.getInventory().addItem(item);
            player.sendMessage(PREFIX + ChatColor.GREEN + "Kasa sandığı eline verildi!");
            return true;
        }

        if (args[0].equalsIgnoreCase("anahtarver")) {
            if (args.length < 3) {
                player.sendMessage(PREFIX + ChatColor.RED + "Kullanim: /kasa anahtarver <oyuncu> <isim> [adet]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Oyuncu bulunamadi!");
                return true;
            }
            String name = args[2].toLowerCase();
            CrateData crate = crates.get(name);
            if (crate == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Böyle bir kasa bulunamadı!");
                return true;
            }

            int amount = 1;
            if (args.length >= 4) {
                try { amount = Integer.parseInt(args[3]); } catch (Exception ignored) {}
            }

            ItemStack key = crate.keyItem.clone();
            key.setAmount(amount);
            target.getInventory().addItem(key);
            player.sendMessage(PREFIX + ChatColor.GREEN + target.getName() + " adli oyuncuya " + amount + " adet anahtar gönderildi.");
            return true;
        }

        if (args[0].equalsIgnoreCase("yonet")) {
            if (args.length < 2) {
                player.sendMessage(PREFIX + ChatColor.RED + "Kullanim: /kasa yonet <isim>");
                return true;
            }
            String name = args[1].toLowerCase();
            CrateData crate = crates.get(name);
            if (crate == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Böyle bir kasa bulunamadı!");
                return true;
            }
            openManageGUI(player, crate);
            return true;
        }

        return true;
    }

    private void openManageGUI(Player player, CrateData crate) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&8ZornoEly Kasa Yönetim: &b" + crate.displayName));

        ItemStack colorItem = new ItemStack(Material.NAME_TAG);
        ItemMeta cm = colorItem.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(ChatColor.YELLOW + "Renk Degistir: " + ChatColor.GREEN + crate.colorTheme);
            cm.setLore(Collections.singletonList(ChatColor.GRAY + "Tiklayarak renk temasini degistir."));
            colorItem.setItemMeta(cm);
        }
        inv.setItem(11, colorItem);

        ItemStack keyItem = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta km = keyItem.getItemMeta();
        if (km != null) {
            km.setDisplayName(ChatColor.YELLOW + "Anahtari Guncelle");
            km.setLore(Collections.singletonList(ChatColor.GRAY + "Elindeki esyayi bu kasanin anahtari yap."));
            keyItem.setItemMeta(km);
        }
        inv.setItem(13, keyItem);

        ItemStack rewardItem = new ItemStack(Material.EMERALD);
        ItemMeta rm = rewardItem.getItemMeta();
        if (rm != null) {
            rm.setDisplayName(ChatColor.GREEN + "Ödülleri Ayarla");
            rm.setLore(Collections.singletonList(ChatColor.GRAY + "Kasadan çıkacak ödülleri yapılandır."));
            rewardItem.setItemMeta(rm);
        }
        inv.setItem(15, rewardItem);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.contains("ZornoEly Kasa Yönetim:")) {
            event.setCancelled(true);
            String rawTitle = ChatColor.stripColor(title);
            String crateName = rawTitle.replace("ZornoEly Kasa Yönetim:", "").trim().toLowerCase();
            CrateData crate = crates.get(crateName);
            if (crate == null) return;

            if (event.getRawSlot() == 11) {
                if (crate.colorTheme.equals("KIRMIZI")) crate.colorTheme = "MAVI";
                else if (crate.colorTheme.equals("MAVI")) crate.colorTheme = "YESIL";
                else if (crate.colorTheme.equals("YESIL")) crate.colorTheme = "SARI";
                else crate.colorTheme = "KIRMIZI";

                saveData();
                player.sendMessage(PREFIX + ChatColor.GREEN + "Kasa renk temasi güncellendi: " + crate.colorTheme);
                openManageGUI(player, crate);
            } else if (event.getRawSlot() == 13) {
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem.getType() == Material.AIR) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Elinde yeni anahtar olacak bir esya olmali!");
                    return;
                }
                crate.keyItem = handItem.clone();
                saveData();
                player.sendMessage(PREFIX + ChatColor.GREEN + "Kasa anahtari elindeki eşya ile güncellendi!");
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (name.endsWith(" Kasası")) {
                String crateName = name.replace(" Kasası", "").trim().toLowerCase();
                if (crates.containsKey(crateName)) {
                    Location loc = event.getBlock().getLocation();
                    placedCrates.put(loc, crateName);
                    saveData();
                    spawnHologram(loc, crates.get(crateName));
                    event.getPlayer().sendMessage(PREFIX + ChatColor.GREEN + "ZornoEly Kasa koruması ile yerleştirildi!");
                }
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (placedCrates.containsKey(loc)) {
            event.setCancelled(true);
            String crateName = placedCrates.remove(loc);
            saveData();
            removeHologram(loc);
            event.getPlayer().sendMessage(PREFIX + ChatColor.YELLOW + "Kasa başarıyla kaldırıldı.");
            event.getBlock().setType(Material.AIR);
            
            CrateData crate = crates.get(crateName);
            if (crate != null) {
                ItemStack item = new ItemStack(crate.blockMaterial);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&l" + crate.displayName + " Kasası"));
                    item.setItemMeta(meta);
                }
                loc.getWorld().dropItemNaturally(loc, item);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Location loc = event.getClickedBlock().getLocation();
        if (!placedCrates.containsKey(loc)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        String crateName = placedCrates.get(loc);
        CrateData crate = crates.get(crateName);
        if (crate == null) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            player.sendMessage(PREFIX + ChatColor.AQUA + crate.displayName + " Kasası içerikleri inceleniyor...");
            // Örnek önizleme tetikleyicisi
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (!isSimilarKey(hand, crate.keyItem)) {
                player.sendMessage(PREFIX + ChatColor.RED + "Bu kasayı açmak için gereken ZornoEly anahtarına sahip değilsin!");
                return;
            }

            hand.setAmount(hand.getAmount() - 1);
            player.sendMessage(PREFIX + ChatColor.GOLD + "🎁 " + crate.displayName + " kasası açılıyor...");
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));
            player.sendTitle(ChatColor.translateAlternateColorCodes('&', "&b&lZORNOELY KASA"), ChatColor.YELLOW + "3x Elmas Kazandın!", 10, 40, 10);
        }
    }

    private boolean isSimilarKey(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        if (a.hasItemMeta() && b.hasItemMeta()) {
            return Objects.equals(a.getItemMeta().getDisplayName(), b.getItemMeta().getDisplayName());
        }
        return true;
    }

    private void spawnHologram(Location loc, CrateData crate) {
        removeHologram(loc);
        List<ArmorStand> stands = new ArrayList<>();
        Location baseLoc = loc.clone().add(0.5, 0.3, 0.5);

        // İstediğin PhoenixCrates tarzı çoklu satır hologram yapısı
        String[] lines = {
            ChatColor.translateAlternateColorCodes('&', "&8Sağ-Tık &7Kasayı Açar!"),
            ChatColor.translateAlternateColorCodes('&', "&8Sol-Tık &7Kasayı Görüntüler!"),
            "",
            ChatColor.translateAlternateColorCodes('&', "&8&l" + crate.displayName + " Kasası!")
        };

        for (int i = 0; i < lines.length; i++) {
            Location lineLoc = baseLoc.clone().add(0, i * 0.25, 0);
            ArmorStand stand = loc.getWorld().spawn(lineLoc, ArmorStand.class, s -> {
                s.setGravity(false);
                s.setVisible(false);
                s.setCustomNameVisible(true);
                s.setCustomName(lines[lines.length - 1 - i]); // Doğru sıra için tersliyoruz
            });
            stands.add(stand);
        }
        holograms.put(loc, stands);
    }

    private void removeHologram(Location loc) {
        if (holograms.containsKey(loc)) {
            List<ArmorStand> stands = holograms.remove(loc);
            if (stands != null) {
                for (ArmorStand stand : stands) {
                    if (stand != null) stand.remove();
                }
            }
        }
    }

    private void saveData() {
        getConfig().set("crates", null);
        for (CrateData c : crates.values()) {
            String path = "crates." + c.name;
            getConfig().set(path + ".displayName", c.displayName);
            getConfig().set(path + ".colorTheme", c.colorTheme);
            getConfig().set(path + ".material", c.blockMaterial.name());
            getConfig().set(path + ".keyItem", c.keyItem);
        }

        getConfig().set("placed", null);
        int i = 0;
        for (Map.Entry<Location, String> entry : placedCrates.entrySet()) {
            Location l = entry.getKey();
            String p = "placed." + (i++);
            getConfig().set(p + ".world", l.getWorld().getName());
            getConfig().set(p + ".x", l.getBlockX());
            getConfig().set(p + ".y", l.getBlockY());
            getConfig().set(p + ".z", l.getBlockZ());
            getConfig().set(p + ".crate", entry.getValue());
        }
        saveConfig();
    }

    private void loadData() {
        crates.clear();
        placedCrates.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("crates");
        if (sec != null) {
            for (String name : sec.getKeys(false)) {
                String path = "crates." + name;
                String displayName = getConfig().getString(path + ".displayName", name);
                String colorTheme = getConfig().getString(path + ".colorTheme", "KIRMIZI");
                Material mat = Material.matchMaterial(getConfig().getString(path + ".material", "CHEST"));
                if (mat == null) mat = Material.CHEST;
                ItemStack key = getConfig().getItemStack(path + ".keyItem");
                if (key == null) key = new ItemStack(Material.TRIPWIRE_HOOK);

                crates.put(name.toLowerCase(), new CrateData(displayName, mat, colorTheme, key, new ArrayList<>()));
            }
        }

        ConfigurationSection pSec = getConfig().getConfigurationSection("placed");
        if (pSec != null) {
            for (String key : pSec.getKeys(false)) {
                String path = "placed." + key;
                String worldName = getConfig().getString(path + ".world");
                if (worldName == null || Bukkit.getWorld(worldName) == null) continue;
                Location l = new Location(
                        Bukkit.getWorld(worldName),
                        getConfig().getInt(path + ".x"),
                        getConfig().getInt(path + ".y"),
                        getConfig().getInt(path + ".z")
                );
                String crateName = getConfig().getString(path + ".crate");
                if (crateName != null && crates.containsKey(crateName)) {
                    placedCrates.put(l, crateName);
                    spawnHologram(l, crates.get(crateName));
                }
            }
        }
    }

    private static class CrateData {
        private String name;
        private String displayName;
        private Material blockMaterial;
        private String colorTheme;
        private ItemStack keyItem;
        private List<ItemStack> rewards;

        public CrateData(String displayName, Material blockMaterial, String colorTheme, ItemStack keyItem, List<ItemStack> rewards) {
            this.name = displayName.toLowerCase();
            this.displayName = displayName;
            this.blockMaterial = blockMaterial;
            this.colorTheme = colorTheme;
            this.keyItem = keyItem;
            this.rewards = rewards;
        }
    }
}
