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
import java.awt.dnd.DropTarget;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;

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

    private long randomDelay = 0;

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
                .getSurrounding(startLocation, 10, config.tree().getNames())
                .stream()
                .min(Comparator.comparing(x -> x.distanceTo(local.getWorldLocation())))
                .orElse(null);

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
                                    Item log1 = Bank.Inventory.getFirst(x->x.getName().contains("logs"));
                                    CurrentTaskStatus = "Depositing Inventory!";
                                    Bank.depositAll(log1.getId());
                                    Time.sleepTick();
                                    Bank.close();
                                }
                    }
                    return -3;
                }

                return -1;
            }
        } else {
            var logs = Inventory.getFirst(x->x.getName().contains("logs"));
            if(logs != null && !local.isAnimating()){
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
//        if (client.getGameState() != GameState.LOGGED_IN) {
//            return;
//        }
        if (config.neverLog())
        {
            randomDelay = randomDelay();
            Executors.newSingleThreadExecutor()
                    .submit(this::pressKey);
        }
        //log.info("ticked");
        CurrentXP = Math.abs(startXP - client.getSkillExperience(Skill.WOODCUTTING));
    }
    private long randomDelay()
    {
        return (long) clamp(Math.round(ThreadLocalRandom.current().nextGaussian() * 8000));
    }

    private double clamp(double value)
    {
        return Math.max(1, Math.min(13000, value));
    }

    private void pressKey()
    {
        KeyEvent keyPress = new KeyEvent(client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), BUTTON1_DOWN_MASK, KeyEvent.VK_BACK_SPACE);
        client.getCanvas().dispatchEvent(keyPress);
        KeyEvent keyRelease = new KeyEvent(client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_BACK_SPACE);
        client.getCanvas().dispatchEvent(keyRelease);
        KeyEvent keyTyped = new KeyEvent(client.getCanvas(), KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_BACK_SPACE);
        client.getCanvas().dispatchEvent(keyTyped);
    }

    @Provides
    ChopperConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ChopperConfig.class);
    }

}
