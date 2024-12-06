package com.NullByte.MossUtils.addon.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import com.NullByte.MossUtils.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;

public class MossBreaker extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = this.settings.createGroup("Render");
    private final SettingGroup sgSettings = this.settings.createGroup("Settings");
    private final Random random = new Random();
    private ScheduledExecutorService scheduler;
    private Set<BlockPos> brokenPositions = new HashSet<>();

    private BlockPos targetMossPos;
    private long lastBreakTime;

    @Override
    public void onDeactivate() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        brokenPositions.clear();
        super.onDeactivate();
    }

    @Override
    public void onActivate() {
        super.onActivate();
        targetMossPos = null;
        lastBreakTime = 0;
        brokenPositions.clear();

        scheduler = Executors.newScheduledThreadPool(1);

        double delayValue = delay.get();
        long periodMillis = Math.max(1, (long)(delayValue * 1000));

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (mc.player != null && mc.world != null) {
                    breakMoss();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, periodMillis, TimeUnit.MILLISECONDS);
    }

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
        .name("Delay")
        .description("Delay between breaking moss blocks.")
        .defaultValue(0.5)
        .range(0.1, 5.0)
        .build()
    );

    private final Setting<Integer> distance_from_player = sgSettings.add(new IntSetting.Builder()
        .name("Distance")
        .description("How far from player to search for moss.")
        .defaultValue(4)
        .range(1, 6)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("Color")
        .description("The color of the target block highlight.")
        .defaultValue(new SettingColor(255, 0, 0, 100))
        .build()
    );

    public MossBreaker() {
        super(AddonTemplate.CATEGORY, "Moss Breaker", "Automatically finds and breaks moss blocks.");
    }

    private void breakInstantBlocks(BlockPos pos) {
        if (mc.world == null) return;

        for (Direction dir : Direction.values()) {
            BlockPos checkPos = pos.offset(dir);
            if (mc.world.getBlockState(checkPos).getBlock() == Blocks.GRASS ||
                mc.world.getBlockState(checkPos).getBlock() == Blocks.TALL_GRASS ||
                mc.world.getBlockState(checkPos).getBlock() == Blocks.AZALEA ||
                mc.world.getBlockState(checkPos).getBlock() == Blocks.FLOWERING_AZALEA) {
                BlockUtils.breakBlock(checkPos, true);
            }
        }
    }

    private BlockPos findMossBlock() {
        if (mc.player == null || mc.world == null) return null;

        BlockPos playerPos = mc.player.getBlockPos();
        List<BlockPos> mossList = new ArrayList<>();

        int range = distance_from_player.get();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.MOSS_BLOCK &&
                        !brokenPositions.contains(pos)) {
                        double distance = pos.getSquaredDistance(playerPos);
                        if (distance <= range * range) {
                            mossList.add(pos);
                        }
                    }
                }
            }
        }

        if (mossList.isEmpty()) return null;
        return mossList.get(random.nextInt(mossList.size()));
    }

    private void breakMoss() {
        if (mc.player == null || mc.world == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBreakTime < delay.get() * 1000) return;
        lastBreakTime = currentTime;

        BlockPos mossPos = findMossBlock();
        if (mossPos == null) {
            mc.player.sendMessage(Text.literal("No moss blocks found in range!"), true);
            return;
        }

        // Break any instant-break blocks around the moss first
        breakInstantBlocks(mossPos);

        // Break the moss block
        BlockUtils.breakBlock(mossPos, true);

        // Add to broken positions
        brokenPositions.add(mossPos);

        targetMossPos = mossPos;
        mc.player.sendMessage(Text.literal("Breaking moss at: " + mossPos), true);
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (targetMossPos != null) {
            Box marker = new Box(targetMossPos);
            event.renderer.box(marker, color.get(), color.get(), ShapeMode.Both, 0);
        }
    }
}
