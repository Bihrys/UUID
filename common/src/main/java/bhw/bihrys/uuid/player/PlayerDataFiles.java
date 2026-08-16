package bhw.bihrys.uuid.player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class PlayerDataFiles {
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private PlayerDataFiles() {
    }

    public static boolean hasAnyData(Path playerData, Path stats, Path advancements) {
        return Files.exists(playerData) || Files.exists(stats) || Files.exists(advancements);
    }

    public static void replace(
            Path donorPlayerData,
            Path receiverPlayerData,
            Path donorStats,
            Path receiverStats,
            Path donorAdvancements,
            Path receiverAdvancements
    ) throws IOException {
        copyOrDelete(donorPlayerData, receiverPlayerData);
        copyOrDelete(donorStats, receiverStats);
        copyOrDelete(donorAdvancements, receiverAdvancements);
    }

    public static void backup(
            Path backupDirectory,
            UUID sourceUuid,
            UUID targetUuid,
            String initiatorName,
            Path playerData,
            Path stats,
            Path advancements
    ) throws IOException {
        String stamp = LocalDateTime.now().format(BACKUP_TIME);
        Path backupRoot = backupDirectory.resolve(stamp + "_source-" + sourceUuid + "_from-" + targetUuid);

        Files.createDirectories(backupRoot);
        Files.writeString(backupRoot.resolve("README.txt"),
                "UUID Swap backup\n" +
                        "sourceUuid=" + sourceUuid + "\n" +
                        "targetUuid=" + targetUuid + "\n" +
                        "initiator=" + initiatorName + "\n" +
                        "time=" + stamp + "\n");

        backupOne(playerData, backupRoot.resolve("playerdata").resolve(sourceUuid + ".dat"));
        backupOne(stats, backupRoot.resolve("stats").resolve(sourceUuid + ".json"));
        backupOne(advancements, backupRoot.resolve("advancements").resolve(sourceUuid + ".json"));
    }

    private static void backupOne(Path source, Path backup) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        Files.createDirectories(backup.getParent());
        Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private static void copyOrDelete(Path donor, Path receiver) throws IOException {
        if (Files.exists(donor)) {
            Files.createDirectories(receiver.getParent());
            Files.copy(donor, receiver, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        } else if (Files.exists(receiver)) {
            Files.delete(receiver);
        }
    }
}
