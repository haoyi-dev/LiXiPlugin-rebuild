package me.typical.lixiplugin.util;

import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.config.types.MainConfig;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class EffectUtil {

    public static void playLixiEffect(Player player) {
        MainConfig.EffectsConfig effects = LXPlugin.getInstance()
                .getConfigManager()
                .getConfig(MainConfig.class)
                .getEffects();
        try {
            String[] soundParts = effects.getSound().split(":");
            if (soundParts.length == 2) {
                Sound sound = Sound.sound(
                        Key.key(soundParts[0], soundParts[1]),
                        Sound.Source.PLAYER,
                        effects.getVolume(),
                        effects.getPitch()
                );
                player.playSound(sound);
            }
        } catch (Exception e) {
            MessageUtil.warn("Failed to play sound: " + effects.getSound());
        }
        if (effects.isParticles()) {
            Location loc = player.getLocation().add(0, 1, 0);
            Particle.DustOptions redDust = new Particle.DustOptions(Color.RED, 1.5f);
            Particle.DustOptions goldDust = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f);
            for (int i = 0; i < 20; i++) {
                double angle = 2 * Math.PI * i / 20;
                double x = Math.cos(angle) * 0.5;
                double z = Math.sin(angle) * 0.5;

                Location particleLoc = loc.clone().add(x, 0, z);
                if (i % 2 == 0) {
                    player.spawnParticle(Particle.REDSTONE, particleLoc, 1, 0, 0, 0, 0, redDust);
                } else {
                    player.spawnParticle(Particle.REDSTONE, particleLoc, 1, 0, 0, 0, 0, goldDust);
                }
            }
            player.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 0.02);
        }
    }
}
