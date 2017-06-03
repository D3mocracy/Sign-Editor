package me.d3mocracy.signedit;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import net.md_5.bungee.api.ChatColor;

public class Main extends JavaPlugin implements Listener {
	private List<UUID> editors = new ArrayList<UUID>();
	private String prefix = ChatColor.YELLOW + "[" + ChatColor.GOLD + "SignEditor" + ChatColor.YELLOW + "] ";

	@Override
	public void onEnable() {
		ProtocolLibrary.getProtocolManager().addPacketListener(
				new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.UPDATE_SIGN) {
					@Override
					public void onPacketReceiving(PacketEvent event) {
						if (editors.contains(event.getPlayer().getUniqueId())) {
							event.setCancelled(true);
							Location l = event.getPacket().getBlockPositionModifier().read(0)
									.toLocation(event.getPlayer().getWorld());
							Block b = l.getBlock();
							Sign s = (Sign) b.getState();
							String[] lines = event.getPacket().getStringArrays().read(0);
							boolean changed = false;

							for (int i = 0; i < 4; i++) {
								if (!s.getLine(i).equals(lines[i])) {
									s.setLine(i, lines[i]);
									changed = true;
								}

							}
							if (changed) {
								event.getPlayer().sendMessage(prefix + "Changed!");
							}

							s.update();

						}
					}
				});
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = (Player) sender;

		if (sender instanceof Player) {
			if (label.equalsIgnoreCase("editsign")) {
				if (player.hasPermission("signedit.command")) {
					if (!editors.contains(player.getUniqueId())) {
						editors.add(player.getUniqueId());
						player.sendMessage(prefix + "You can edit signs now");
					} else {
						editors.remove(player.getUniqueId());
						player.sendMessage(prefix + "You can not edit signs anymore");
					}
				} else
					player.sendMessage(prefix + "You don't have enough permissions to use this command");
			}
		}
		return false;
	}

	@EventHandler
	public void onRightClickSing(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Action right = Action.RIGHT_CLICK_BLOCK;

		if (event.getAction() == right) {
			if (event.getClickedBlock().getType().name().contains("SIGN")) {
				if (player.hasPermission("signedit.edit")) {
					if (editors.contains(player.getUniqueId())) {
						openSignEditor(player, event.getClickedBlock().getLocation());
					}
				} else {
					player.sendMessage(prefix + "You don't have enough permissions to edit this sign");
				}
			}
		}
	}

	@EventHandler
	public void onLeft(PlayerQuitEvent event) {
		editors.remove(event.getPlayer());
	}

	public void openSignEditor(Player p, Location loc) {
		PacketContainer openSearch = new PacketContainer(PacketType.Play.Server.OPEN_SIGN_EDITOR);
		openSearch.getBlockPositionModifier().write(0, new BlockPosition(loc.toVector()));

		try {
			ProtocolLibrary.getProtocolManager().sendServerPacket(p, openSearch);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Cannot send packet " + openSearch, e);
		}
	}

}
