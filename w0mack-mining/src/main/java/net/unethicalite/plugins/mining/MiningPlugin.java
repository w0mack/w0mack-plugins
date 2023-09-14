package net.unethicalite.plugins.mining;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.pathfinder.GlobalCollisionMap;
import net.unethicalite.api.movement.pathfinder.model.BankLocation;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.utils.MessageUtils;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;

@Extension
@PluginDescriptor(
        name = "w0mack Mining",
        description = "Mines rocks",
        enabledByDefault = false
)
@Slf4j
public class MiningPlugin extends LoopedPlugin {
    @Inject
    private MiningConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MiningOverlay chopperOverlay;

    @Inject
    private GlobalCollisionMap collisionMap;


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
        startXP = client.getSkillExperience(Skill.MINING);
        start = System.currentTimeMillis();
    }

    @Override
    public void stop() {
        super.stop();
        overlayManager.remove(chopperOverlay);
    }

    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event) {
        if (!event.getGroup().contains("w0mack-mining") || !event.getKey().toLowerCase().contains("start")) {
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

        var rock = TileObjects
                .getSurrounding(startLocation, 10, config.rock().getNames())
                .stream()
                .min(Comparator.comparing(x -> x.distanceTo(local.getWorldLocation())))
                .orElse(null);

        if (config.bankOre()) {
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
            var ore = Inventory.getFirst(x->x.getName().contains("ore"));
            if(!config.bankOre())
            {
                int oreCount = Inventory.getCount(ore.getId());
                while (Inventory.getCount(ore.getId()) > 0) {
                    CurrentTaskStatus = "Dropping Ore!";
                    ore.drop();
                    System.out.println("Dropping ore...");
                }
                return 500;
            }
//            if(ore != null && !local.isAnimating()){
//                CurrentTaskStatus = "Dropping ore!";
//                ore.drop();
//                return 500;
//            }
        }

        if (local.isMoving() || local.isAnimating()) {
            return 333;
        }

        if (rock == null) {
            log.debug("Could not find any trees");
            return 1000;
        }


        rock.interact("Mine");
        CurrentTaskStatus = "Mining " + rock.getName();
        return 1000;
    }

    @Subscribe
    private void onGameTick(GameTick e) {
//        if (client.getGameState() != GameState.LOGGED_IN) {
//            return;
//        }
        //log.info("ticked");
        CurrentXP = Math.abs(startXP - client.getSkillExperience(Skill.WOODCUTTING));
    }

    @Provides
    MiningConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MiningConfig.class);
    }

}
