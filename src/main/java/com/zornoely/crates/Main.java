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
    private final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&8[&5ZornoEly&8] &7");

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("kasa") != null) {
            getCommand("kasa").setExecutor(this);
        }
        loadData();
        getLogger().info("ZornoCrates Gelişmiş Kasa Sistemi aktif edildi!");
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
        getLogger().info("ZornoCrates devre dışı bırakıldı!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bu komutu sadece oyuncular kullanabilir.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&l--- &5&lZornoEly Crate Yardım &8&l---"));
            player.sendMessage(ChatColor.LIGHT_PURPLE + "/kasa olustur <isim> " + ChatColor.WHITE + "- Yeni kasa tanımlar");
            player.sendMessage(ChatColor.LIGHT_PURPLE + "/kasa al <isim> " + ChatColor.WHITE + "- Kasa sandığını alırsın");
            player.sendMessage(ChatColor.LIGHT_PURPLE + "/kasa anahtarver <oyuncu> <isim> <adet> " + ChatColor.WHITE + "- Anahtar verir");
            player.sendMessage(ChatColor.LIGHT_PURPLE + "/kasa yonet <isim> " + ChatColor.WHITE + "- Kasa ayarları GUI");
            return true;
        }

        if (args[0].equalsIgnoreCase("olustur")) {
            if (args.length < 2) {
                player.sendMessage(PREFIX + ChatColor.RED + "Kullanım: /kasa olustur <isim>");
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
                km.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&8[&5" + args[1].toUpperCase() + " KASA ANAHTARI&8]"));
                defaultKey.setItemMeta(km);
            }

            CrateData crate = new CrateData(args[1], Material.CHEST, "MOR", defaultKey, new ArrayList<>());
            crates.put(name, crate);
            saveData();
            player.sendMessage(PREFIX + ChatColor.GREEN + "'" + args[1] + "' kasası başarıyla oluşturuldu!");
            return true;
        }

        if (args[0].equalsIgnoreCase("al")) {
            if (args.length < 2) {
                player.sendMessage(PREFIX + ChatColor.RED + "Kullanım: /kasa al <isim>");
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
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5&l" + crate.displayName + " Kasası"));
                meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&', "&7Yere koyarak kasayı aktif et!")));
                item.setItemMeta(meta);
            }
            player.getInventory().addItem(item);
            player.sendMessage(PREFIX + ChatColor.GREEN + "Kasa sandığı eline verildi!");
            return true;
        }

        if (args[0].equalsIgnoreCase("anahtarver")) {
            if (args.length < 3) {
                player.sendMessage(PREFIX + ChatColor.RED + "Kullanım: /kasa anahtarver <oyuncu> <isim> [adet]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Oyuncu bulunamadı!");
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
            player.sendMessage(PREFIX + ChatColor.GREEN + target.getName() + " adlı oyuncuya " + amount + " adet anahtar gönderildi.");
            return true;
        }

        if (args[0].equalsIgnoreCase("yonet")) {
            if (args.length < 2) {
                player.sendMessage(PREFIX + ChatColor.RED + "Kullanım: /kasa yonet <isim>");
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
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&8Yönetim: &5" + crate.displayName));

        ItemStack colorItem = new ItemStack(Material.NAME_TAG);
        ItemMeta cm = colorItem.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(ChatColor.LIGHT_PURPLE + "Tema: " + ChatColor.WHITE + crate.colorTheme);
            cm.setLore(Collections.singletonList(ChatColor.GRAY + "Değiştirmek için tıkla."));
            colorItem.setItemMeta(cm);
        }
        inv.setItem(10, colorItem);

        ItemStack blockItem = new ItemStack(crate.blockMaterial);
        ItemMeta bm = blockItem.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(ChatColor.LIGHT_PURPLE + "Kasa Bloğu: " + ChatColor.WHITE + crate.blockMaterial.name());
            bm.setLore(Collections.singletonList(ChatColor.GRAY + "Elindeki blok ile değiştirmek için tıkla."));
            blockItem.setItemMeta(bm);
        }
        inv.setItem(12, blockItem);

        ItemStack keyItem = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta km = keyItem.getItemMeta();
        if (km != null) {
            km.setDisplayName(ChatColor.LIGHT_PURPLE + "Anahtarı Güncelle");
            km.setLore(Collections.singletonList(ChatColor.GRAY + "Elindeki eşyayı anahtar yap."));
            keyItem.setItemMeta(km);
        }
        inv.setItem(14, keyItem);

        ItemStack rewardItem = new ItemStack(Material.EMERALD);
        ItemMeta rm = rewardItem.getItemMeta();
        if (rm != null) {
            rm.setDisplayName(ChatColor.GREEN + "Ödülleri Düzenle");
            rm.setLore(Arrays.asList(ChatColor.GRAY + "Tıklayarak ödül ekleme", ChatColor.GRAY + "menüsünü aç."));
            rewardItem.setItemMeta(rm);
        }
        inv.setItem(16, rewardItem);

        player.openInventory(inv);
    }

    private void openRewardsGUI(Player player, CrateData crate) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', "&8Ödüller: &5" + crate.displayName));
        for (int i = 0; i < crate.rewards.size() && i < 45; i++) {
            if (crate.rewards.get(i) != null) {
                inv.setItem(i, crate.rewards.get(i));
            }
        }
        
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        if (im != null) {
            im.setDisplayName(ChatColor.YELLOW + "Bilgi");
            im.setLore(Arrays.asList(ChatColor.GRAY + "Envantere koyduğun eşyalar", ChatColor.GRAY + "bu kasanın ödülü olur."));
            info.setItemMeta(im);
        }
        inv.setItem(49, info);
        player.openInventory(inv);
    }

    private void openPreviewGUI(Player player, CrateData crate) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&5&l" + crate.displayName + " Ödülleri"));
        for (int i = 0; i < crate.rewards.size() && i < 27; i++) {
            if (crate.rewards.get(i) != null) {
                inv.setItem(i, crate.rewards.get(i));
            }
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.contains("Yönetim:")) {
            event.setCancelled(true);
            String rawTitle = ChatColor.stripColor(title);
            String crateName = rawTitle.replace("Yönetim:", "").trim().toLowerCase();
            CrateData crate = crates.get(crateName);
            if (crate == null) return;

            if (event.getRawSlot() == 10) {
                if (crate.colorTheme.equals("MOR")) crate.colorTheme = "ALTIN";
                else if (crate.colorTheme.equals("ALTIN")) crate.colorTheme = "MAVİ";
                else crate.colorTheme = "MOR";

                saveData();
                player.sendMessage(PREFIX + ChatColor.LIGHT_PURPLE + "Tema güncellendi: " + crate.colorTheme);
                openManageGUI(player, crate);
            } else if (event.getRawSlot() == 12) {
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem.getType() == Material.AIR || !handItem.getType().isBlock()) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Elinde geçerli yerleştirilebilir bir blok olmalı!");
                    return;
                }
                crate.blockMaterial = handItem.getType();
                saveData();
                player.sendMessage(PREFIX + ChatColor.GREEN + "Kasa blok materyali güncellendi: " + crate.blockMaterial.name());
                openManageGUI(player, crate);
            } else if (event.getRawSlot() == 14) {
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem.getType() == Material.AIR) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Elinde yeni anahtar olacak bir eşya olmalı!");
                    return;
                }
                crate.keyItem = handItem.clone();
                saveData();
                player.sendMessage(PREFIX + ChatColor.GREEN + "Kasa anahtarı güncellendi!");
            } else if (event.getRawSlot() == 16) {
                openRewardsGUI(player, crate);
            }
        } else if (title.contains("Ödüller:")) {
            String rawTitle = ChatColor.stripColor(title);
            String crateName = rawTitle.replace("Ödüller:", "").trim().toLowerCase();
            CrateData crate = crates.get(crateName);
            if (crate == null) return;

            if (event.getRawSlot() == 49) {
                event.setCancelled(true);
                return;
            }

            Bukkit.getScheduler().runTaskLater(this, () -> {
                Inventory inv = event.getInventory();
                List<ItemStack> newRewards = new ArrayList<>();
                for (int i = 0; i < 45; i++) {
                    ItemStack item = inv.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        newRewards.add(item.clone());
                    }
                }
                crate.rewards = newRewards;
                saveData();
            }, 2L);
        } else if (title.contains("Ödülleri")) {
            event.setCancelled(true);
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
                    event.getPlayer().sendMessage(PREFIX + ChatColor.GREEN + "Kasa başarıyla yerleştirildi ve korumaya alındı!");
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
            event.getPlayer().sendMessage(PREFIX + ChatColor.YELLOW + "Kasa kaldırıldı.");
            event.getBlock().setType(Material.AIR);
            
            CrateData crate = crates.get(crateName);
            if (crate != null) {
                ItemStack item = new ItemStack(crate.blockMaterial);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5&l" + crate.displayName + " Kasası"));
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
            openPreviewGUI(player, crate);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (!isSimilarKey(hand, crate.keyItem)) {
                player.sendMessage(PREFIX + ChatColor.RED + "Bu kasayı açmak için gereken anahtara sahip değilsin!");
                return;
            }

            if (crate.rewards.isEmpty()) {
                player.sendMessage(PREFIX + ChatColor.RED + "Bu kasanın henüz tanımlanmış bir ödülü yok!");
                return;
            }

            hand.setAmount(hand.getAmount() - 1);
            player.sendMessage(PREFIX + ChatColor.LIGHT_PURPLE + "🎁 " + crate.displayName + " kasası açılıyor...");
            
            Random random = new Random();
            ItemStack reward = crate.rewards.get(random.nextInt(crate.rewards.size())).clone();
            player.getInventory().addItem(reward);
            
            player.sendTitle(ChatColor.translateAlternateColorCodes('&', "&5&lZORNOELY KASA"), ChatColor.LIGHT_PURPLE + "Ödülün Envanterine Eklendi!", 10, 40, 10);
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
        Location baseLoc = loc.clone().add(0.5, 1.2, 0.5);

        String[] lines = {
            ChatColor.translateAlternateColorCodes('&', "&5&l" + crate.displayName + " Kasası"),
            "",
            ChatColor.translateAlternateColorCodes('&', "&7Sol-Tık: &dİçeriği Gör"),
            ChatColor.translateAlternateColorCodes('&', "&7Sağ-Tık: &dKasayı Aç")
        };

        for (int i = 0; i < lines.length; i++) {
            Location lineLoc = baseLoc.clone().add(0, (lines.length - 1 - i) * 0.25, 0);
            final int index = i;
            ArmorStand stand = loc.getWorld().spawn(lineLoc, ArmorStand.class, s -> {
                s.setGravity(false);
                s.setVisible(false);
                s.setCustomNameVisible(true);
                s.setCustomName(lines[index]);
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
            getConfig().set(path + ".rewards", c.rewards);
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

    @SuppressWarnings("unchecked")
    private void loadData() {
        crates.clear();
        placedCrates.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("crates");
        if (sec != null) {
            for (String name : sec.getKeys(false)) {
                String path = "crates." + name;
                String displayName = getConfig().getString(path + ".displayName", name);
                String colorTheme = getConfig().getString(path + ".colorTheme", "MOR");
                Material mat = Material.matchMaterial(getConfig().getString(path + ".material", "CHEST"));
                if (mat == null) mat = Material.CHEST;
                ItemStack key = getConfig().getItemStack(path + ".keyItem");
                if (key == null) key = new ItemStack(Material.TRIPWIRE_HOOK);
                
                List<ItemStack> rewards = (List<ItemStack>) getConfig().getList(path + ".rewards");
                if (rewards == null) rewards = new ArrayList<>();

                crates.put(name.toLowerCase(), new CrateData(displayName, mat, colorTheme, key, rewards));
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
