package bhw.bihrys.uuid;

import bhw.bihrys.uuid.command.UuidSwapCommand;
import bhw.bihrys.uuid.config.UuidConfig;
import bhw.bihrys.uuid.pet.PetOwnerSwapHandler;
import bhw.bihrys.uuid.player.PlayerDataManager;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Uuid.MOD_ID)
public final class Uuid {
    public static final String MOD_ID = "uuid";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Uuid() {
        UuidConfig.load();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(PetOwnerSwapHandler.class);
        LOGGER.info("Initialized UUID Swap for Forge 1.20.1");
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        UuidSwapCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerDataManager.onPlayerLoggedOut(event.getEntity());
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PlayerDataManager.onServerTick(event.getServer());
        }
    }
}
