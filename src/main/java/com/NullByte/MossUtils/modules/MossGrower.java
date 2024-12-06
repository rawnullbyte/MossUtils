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

public class MossGrower extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = this.settings.createGroup("Render");
    private final SettingGroup sgSettings = this.settings.createGroup("Settings");
    private final Random random = new Random();
    private ScheduledExecutorService scheduler;
    private Set<BlockPos> harvestedPositions = new HashSet<>();

    private BlockPos lastMossPos;

    @Override
    public void onDeactivate() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        harvestedPositions.clear();
        super.onDeactivate();
    }

    @Override
    public void onActivate() {
        super.onActivate();
        lastMossPos = null;
        harvestedPositions.clear();

        scheduler = Executors.newScheduledThreadPool(1);

        double delayValue;
        if (delay == null) {
            delayValue = 1.0;
        } else {
            try {
                delayValue = Double.parseDouble(delay.toString());
            } catch (NumberFormatException e) {
                delayValue = 1.0;
            }
        }

        long initialDelayMillis = Math.max(0, (long) (delayValue * 1000));
        long periodMillis = Math.max(1, (long) (delayValue * 1000));

        mc.player.sendMessage(Text.literal("Delay is: " + delayValue + " seconds"), true);

        scheduler.scheduleAtFixedRate(
            this::growMoss,
            initialDelayMillis,
            periodMillis,
            TimeUnit.MILLISECONDS
        );
    }

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
        .name("Delay")
        .description("Delay between bone meal applications.")
        .defaultValue(3)
        .range(0.01d, 15.0d)
        .build()
    );

    private final Setting<Integer> distance_from_player = sgSettings.add(new IntSetting.Builder()
        .name("Distance")
        .description("How far from player should moss be grown.")
        .defaultValue(4)
        .range(1, 6)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("Color")
        .description("The color of the marker.")
        .defaultValue(Color.GREEN)
        .build()
    );

    public MossGrower() {
        super(AddonTemplate.CATEGORY, "Moss Grower", "Automatically grows moss blocks using bone meal.");
    }

    private void breakInstantBlocks(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos checkPos = pos.offset(dir);
            if (mc.world.getBlockState(checkPos).getBlock() == Blocks.GRASS || 
                mc.world.getBlockState(checkPos).getBlock() == Blocks.TALL_GRASS ||
                mc.world.getBlockState(checkPos).getBlock() == Blocks.MOSS_CARPET ||
                mc.world.getBlockState(checkPos).getBlock() == Blocks.AZALEA ||
                mc.world.getBlockState(checkPos).getBlock() == Blocks.FLOWERING_AZALEA) {
                BlockUtils.breakBlock(checkPos, true);
            }
        }
    }

    private BlockPos getFarthestMossBlock() {
        BlockPos playerPos = mc.player.getBlockPos();
        List<BlockPos> farthestMossList = new ArrayList<>();
        double maxDistance = 0;

        int range = distance_from_player.get();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.MOSS_BLOCK && 
                        !harvestedPositions.contains(pos) &&
                        (mc.world.getBlockState(pos.up()).isAir() || 
                         mc.world.getBlockState(pos.up()).getBlock() == Blocks.GRASS ||
                         mc.world.getBlockState(pos.up()).getBlock() == Blocks.TALL_GRASS ||
                         mc.world.getBlockState(pos.up()).getBlock() == Blocks.MOSS_CARPET ||
                         mc.world.getBlockState(pos.up()).getBlock() == Blocks.AZALEA ||
                         mc.world.getBlockState(pos.up()).getBlock() == Blocks.FLOWERING_AZALEA)) {
                        double distance = pos.getSquaredDistance(playerPos);
                        if (distance <= range * range) {
                            if (distance > maxDistance) {
                                maxDistance = distance;
                                farthestMossList.clear();
                                farthestMossList.add(pos);
                            } else if (distance == maxDistance) {
                                farthestMossList.add(pos);
                            }
                        }
                    }
                }
            }
        }
        
        if (farthestMossList.isEmpty()) return null;
        return farthestMossList.get(random.nextInt(farthestMossList.size()));
    }

    private void growMoss() {
        BlockPos mossPos = getFarthestMossBlock();
        if (mossPos == null) {
            mc.player.sendMessage(Text.literal("No accessible moss blocks found in range!"), true);
            return;
        }

        // Break any instant-break blocks around the moss first
        breakInstantBlocks(mossPos);

        // Find bone meal in inventory
        int boneMealSlot = -1;
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.BONE_MEAL) {
                boneMealSlot = i;
                break;
            }
        }

        if (boneMealSlot == -1) {
            mc.player.sendMessage(Text.literal("No bone meal found in inventory!"), true);
            return;
        }

        // Switch to bone meal
        int previousSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = boneMealSlot;

        // Use bone meal on moss
        BlockUtils.interact(new BlockHitResult(mossPos.toCenterPos(), Direction.UP, mossPos, false), Hand.MAIN_HAND, true);

        // Switch back to previous slot
        mc.player.getInventory().selectedSlot = previousSlot;

        // Add position to harvested set
        harvestedPositions.add(mossPos);
        
        lastMossPos = mossPos;
        mc.player.sendMessage(Text.literal("Growing moss at: " + mossPos), false);
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (lastMossPos != null) {
            Box marker = new Box(lastMossPos);
            event.renderer.box(marker, color.get(), color.get(), ShapeMode.Both, 0);
        }
    }
}
