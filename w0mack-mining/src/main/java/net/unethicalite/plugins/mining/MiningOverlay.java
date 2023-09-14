package net.unethicalite.plugins.mining;

import com.google.inject.Singleton;
import com.openosrs.client.ui.overlay.components.table.TableAlignment;
import com.openosrs.client.ui.overlay.components.table.TableComponent;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

@Singleton
class MiningOverlay extends Overlay {
    private final Client client;
    private final MiningPlugin plugin;
    private final MiningConfig config;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    private MiningOverlay(Client client, MiningPlugin plugin, MiningConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        this.setPriority(OverlayPriority.HIGHEST);
        this.setPosition(OverlayPosition.BOTTOM_LEFT);
        this.getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG,OPTION_CONFIGURE, "w0mack Chopper"));

    }

    @Override
    public Dimension render(Graphics2D graphics2D) {

        if (!plugin.isScriptStarted()) {
            return null;
        }

        panelComponent.getChildren().clear();

        TableComponent tableComponent = new TableComponent();
        tableComponent.setColumnAlignments(TableAlignment.LEFT);
        tableComponent.setDefaultColor(Color.WHITE);

        tableComponent.addRow("w0mack Mining");
        tableComponent.addRow("Status: " + plugin.CurrentTaskStatus);
        long end  = System.currentTimeMillis() - plugin.start;

        DateFormat df = new SimpleDateFormat("HH ':' mm ':' ss ");
        df.setTimeZone(TimeZone.getTimeZone("GTM+0"));
//        tableComponent.addRow("Time running: " + df.format(new Date(end)));

//        int XPPerHour = (int) (plugin.CurrentXP / ((System.currentTimeMillis() - plugin.start) / 3600000.0D));
//        tableComponent.addRow("XP Gained: " + plugin.CurrentXP, "XP Per hr: " + MiningPlugin.convertToRSUnits(XPPerHour));

        if(!tableComponent.isEmpty()){
            panelComponent.getChildren().add(tableComponent);
        }
        panelComponent.setPreferredSize(new Dimension(250,100));
        panelComponent.setBackgroundColor(new Color(255, 255, 255, 80));

        return panelComponent.render(graphics2D);
    }
}