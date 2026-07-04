package ru.raidmine.minibaritone;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Client entrypoint. Registers /mb commands and ticks the navigator.
 */
public final class MiniBaritoneClient implements ClientModInitializer {
    public static final String MOD_ID = "raidmine_minibaritone";
    private static final Navigator NAVIGATOR = new Navigator();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(NAVIGATOR::tick);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            ClientCommandManager.literal("mb")
                .then(ClientCommandManager.literal("goto")
                    .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                        .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
                            .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
                                .executes(context -> {
                                    int x = IntegerArgumentType.getInteger(context, "x");
                                    int y = IntegerArgumentType.getInteger(context, "y");
                                    int z = IntegerArgumentType.getInteger(context, "z");
                                    NAVIGATOR.goTo(new BlockPos(x, y, z));
                                    return 1;
                                })))))
                .then(ClientCommandManager.literal("stop")
                    .executes(context -> {
                        NAVIGATOR.stop("Остановлено.");
                        return 1;
                    }))
                .then(ClientCommandManager.literal("status")
                    .executes(context -> {
                        send(NAVIGATOR.getStatusText());
                        return 1;
                    }))
                .then(ClientCommandManager.literal("here")
                    .executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player == null) {
                            return 0;
                        }
                        BlockPos pos = client.player.getBlockPos();
                        send("Ты сейчас на: " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
                        return 1;
                    }))
        ));

        send("MiniBaritone загружен. Команды: /mb goto x y z, /mb stop, /mb status, /mb here");
    }

    public static void send(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§6[MiniBaritone] §f" + message), false);
        }
    }
}
