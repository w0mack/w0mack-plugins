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
import net.unethicalite.api.movement.pathfinder.Walker;
import net.unethicalite.api.movement.pathfinder.model.BankLocation;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.scene.Tiles;
import net.unethicalite.api.utils.MessageUtils;
import net.unethicalite.api.widgets.Widgets;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.dnd.DropTarget;
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

        if (!scriptStarted) {
            return 1000;
        }

        var local = Players.getLocal();
        if (local == null) {
            return -1;
        }
        int logID = 0;

        var tree = TileObjects
                .getSurrounding(startLocation, 30, config.tree().getNames())
                .stream()
                .min(Comparator.comparing(x -> x.distanceTo(local.getWorldLocation())))
                .orElse(null);
        var logs = Inventory.getFirst(x->x.getName().contains("logs"));

        if (logs == null){
            logs = Inventory.getFirst(x->x.getName().contains("Logs"));
        }

        if (config.bankLogs()) {
            if (Inventory.isFull()) {

                Movement.walkTo(BankLocation.getNearest());
                CurrentTaskStatus = "Running to bank!";

                TileObject bank = TileObjects.getFirstSurrounding(local.getWorldLocation(), 10, obj -> obj.hasAction("Bank"));

                if (bank != null) {
                    if (!Bank.isOpen()) {
                        return BankHelper.clickLocalBank();
                    }

                    if (Bank.isOpen()) {
                                log.info("FULL OF LOGS! TRYING TO DEPOSIT!");
                                Time.sleepTick();
                                if(!Bank.Inventory.getAll().isEmpty()) {
                                    System.out.println(logs.getId());
                                    CurrentTaskStatus = "Depositing Inventory!";
                                    Bank.depositAll(logs.getId());
                                    Time.sleepTick();
                                    Bank.close();
                                }
                    }
                    return -3;
                }
                return -1;
            }
        } else {
            if(logs != null && !local.isAnimating()){
                if(Inventory.isFull()) {
                    List<Item> matchingLogs = Inventory.getAll(logs.getName());
                    for (Item item : matchingLogs) {
                        CurrentTaskStatus="Dropping Logs!";
                        item.drop();
                        Time.sleepTick(); // Adjust the sleep duration as needed
                    }
                }
                tree.interact("Chop down");
                return 500;
            }
        }

        if (local.isMoving() || local.isAnimating()) {
            return 333;
        }

        if(local.isMoving()){
            CurrentTaskStatus="Walking to " + tree.getName();
            return 222;
        }

        if (tree == null) {
            //CurrentTaskStatus="Waiting on " + tree.getName();
            Walker.walkTo(startLocation);
            //log.debug("Could not find any trees");
            return 1000;
        }
        tree.interact("Chop down");
        CurrentTaskStatus = "Cutting " + tree.getName();
        return 1000;
    }
    @Subscribe
    private void onGameTick(GameTick e) {

    }

    @Provides
    ChopperConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ChopperConfig.class);
    }

}
