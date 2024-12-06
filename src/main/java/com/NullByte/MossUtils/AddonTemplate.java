package com.NullByte.MossUtils.addon;

import com.NullByte.MossUtils.addon.commands.CommandExample;
import com.NullByte.MossUtils.addon.modules.MossGrower;
import com.NullByte.MossUtils.addon.modules.MossBreaker;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
//import meteordevelopment.meteorclient.commands.Commands;
//import meteordevelopment.meteorclient.systems.hud.Hud;
//import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Moss");
    //public static final HudGroup HUD_GROUP = new HudGroup("Example");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Moss Utils");

        // Modules
        Modules.get().add(new MossGrower());
        Modules.get().add(new MossBreaker());
        // Commands
        //Commands.add(new CommandExample());

        // HUD
        //Hud.get().register(HudExample.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.NullByte.MossUtils.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("NullByte", "MossUtils");
    }
}
