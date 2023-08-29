package net.unethicalite.plugins.chopper;

import com.google.inject.Provides;
import com.openosrs.client.util.WeaponMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.movement.pathfinder.GlobalCollisionMap;
import net.unethicalite.api.movement.pathfinder.model.BankLocation;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.scene.Tiles;
import net.unethicalite.api.utils.MessageUtils;
import net.unethicalite.api.widgets.Widgets;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Extension
@PluginDescriptor(
        name = "w0mack Chopper",
        description = "Chops trees",
        enabledByDefault = false
)
@Slf4j
public class ChopperPlugin extends LoopedPlugin {
    @Inject
    private ChopperConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ChopperOverlay chopperOverlay;

    @Inject
    private GlobalCollisionMap collisionMap;

    private int fmCooldown = 0;

    @Getter(AccessLevel.PROTECTED)
    private List<Tile> fireArea;

    private WorldPoint startLocation = null;

    @Getter(AccessLevel.PROTECTED)
    private boolean scriptStarted;

    @Inject
    private Client client;

    public String CurrentTaskStatus = "Initializing...";
    public int CurrentXP = 0, startXP = 0;

    public long start;

    @Override
    protected void startUp() {
        overlayManager.add(chopperOverlay);
        startXP = client.getSkillExperience(Skill.WOODCUTTING);
        start = System.currentTimeMillis();
    }

    @Override
    public void stop() {
        super.stop();
        overlayManager.remove(chopperOverlay);
    }

    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event) {
        if (!event.getGroup().contains("w0mack-chopper") || !event.getKey().toLowerCase().contains("start")) {
            return;
        }

        if (scriptStarted) {
            scriptStarted = false;
        } else {
            var local = Players.getLocal();
            if (local == null) {
                return;
            }
            startLocation = local.getWorldLocation();
            //fireArea = generateFireArea(3);
            this.scriptStarted = true;
            log.info("Script started");
        }
    }

    public static String convertToRSUnits(int number) {
        String postfix = "";
        int divisor = 1;

        if (number >= 1_000_000) {
            divisor = 1_000_000;
            postfix = "M";
        } else if (number >= 1_000) {
            divisor = 1_000;
            postfix = "K";
        }

        int formattedNumber = (int) Math.ceil((double) number / divisor);

        // Using the NumberFormat class to add commas
        NumberFormat nf = NumberFormat.getInstance();
        return nf.format(formattedNumber) + postfix;
    }

    @Override
    protected int loop() {
        var local = Players.getLocal();
        if (local == null) {
            return -1;
        }

        var logs = Inventory.getFirst(x->x.getName().contains("logs"));

        var tree = TileObjects
                .getSurrounding(startLocation, 10, config.tree().getNames())
                .stream()
                .min(Comparator.comparing(x -> x.distanceTo(local.getWorldLocation())))
                .orElse(null);

        if (config.bankLogs()) {
            if (Inventory.isFull()) {
                //MessageUtils.addMessage("Inventory is full!", ChatColorType.HIGHLIGHT);
                Movement.walkTo(BankLocation.getNearest());
                CurrentTaskStatus = "Running to bank!";
                //MessageUtils.addMessage("Walking to the closest bank!", ChatColorType.HIGHLIGHT);
                TileObject bank = TileObjects.getFirstSurrounding(local.getWorldLocation(), 10, obj -> obj.hasAction("Bank"));
                int logID = logs.getId();

                if (tree.getName().equals("Tree") || tree.getName().equals("Evergreen tree")) return logID=ItemID.LOGS;
                if (tree.getName().equals("Oak tree")) return logID=ItemID.OAK_LOGS;
                if (tree.getName().equals("Teak tree")) return logID=ItemID.TEAK_LOGS;
                if (tree.getName().equals("Maple tree")) return logID=ItemID.MAPLE_LOGS;
                if (tree.getName().equals("Mahogany tree")) return  logID=ItemID.MAHOGANY_LOGS;
                if (tree.getName().equals("Yew tree")) return logID=ItemID.YEW_LOGS;
                if (tree.getName().equals("Magic tree")) return logID=ItemID.MAGIC_LOGS;
                if (tree.getName().equals("Redwood tree")) return logID=ItemID.REDWOOD_LOGS;

                if (bank != null) {

                    bank.interact("Bank");
                    if (Bank.isOpen()) {
                                log.info("FULL OF LOGS! TRYING TO DEPOSIT!");
                                Time.sleepTick();
                                if(!Bank.Inventory.getAll().isEmpty()) {
                                    CurrentTaskStatus = "Depositing Inventory!";
                                    Bank.depositAll(logID);
                                    Time.sleepTick();
                                    Bank.close();
                                }
                    }


                    return -3;
                }
                MessageUtils.addMessage("Can't find the closest bank! Good bye!", ChatColorType.HIGHLIGHT);
                return -1;
            }
        } else {
            if (logs != null && !local.isAnimating())
            {
                logs.drop();
                return 500;
            }
        }

        if (local.isMoving() || local.isAnimating()) {
            return 333;
        }

        if (tree == null) {
            log.debug("Could not find any trees");
            return 1000;
        }

        tree.interact("Chop down");
        CurrentTaskStatus = "Cutting " + tree.getName();
        return 1000;
    }

    @Subscribe
    private void onGameTick(GameTick e) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        //log.info("ticked");
        CurrentXP = Math.abs(startXP - client.getSkillExperience(Skill.WOODCUTTING));
    }

    @Provides
    ChopperConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ChopperConfig.class);
    }

}
