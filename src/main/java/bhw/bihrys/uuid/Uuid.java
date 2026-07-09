package bhw.bihrys.uuid;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import bhw.bihrys.uuid.command.UuidSwapCommand;
import bhw.bihrys.uuid.config.UuidConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Uuid implements ModInitializer {
	public static final String MOD_ID = "uuid";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing UUID Swap Mod");

		UuidConfig.load();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			UuidSwapCommand.register(dispatcher)
		);
	}
}
