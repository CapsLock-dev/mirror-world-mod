package capslab.mirrorworld.utils.playerdata;

import com.mojang.datafixers.DataFixer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class MirrorPlayerDataStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("MirrorPlayerData");
    private final File playerDir;
    private final DataFixer fixerUpper;

    public MirrorPlayerDataStorage(Path playerDir, DataFixer fixerUpper) {
        this.playerDir = playerDir.toFile();
        this.playerDir.mkdirs();
        this.fixerUpper = fixerUpper;
    }

    public void save(ServerPlayer player) {
        try (ProblemReporter.ScopedCollector scopedCollector =
                     new ProblemReporter.ScopedCollector(player.problemPath(), LOGGER)) {
            TagValueOutput tagValueOutput = TagValueOutput.createWithContext(scopedCollector, player.registryAccess());
            player.saveWithoutId(tagValueOutput);

            Path dir = playerDir.toPath();
            Path tmp = Files.createTempFile(dir, player.getStringUUID() + "-", ".dat");
            CompoundTag tag = tagValueOutput.buildResult();
            NbtIo.writeCompressed(tag, tmp);

            Path dest = dir.resolve(player.getStringUUID() + ".dat");
            Path old = dir.resolve(player.getStringUUID() + ".dat_old");
            Util.safeReplaceFile(dest, tmp, old);
        } catch (Exception e) {
            LOGGER.warn("Failed to save mirror player data for {}", player.getGameProfile().name(), e);
        }
    }

    public Optional<CompoundTag> load(ServerPlayer player) {
        File file = new File(playerDir, player.getStringUUID() + ".dat");
        if (!file.exists() || !file.isFile()) return Optional.empty();
        try {
            CompoundTag tag = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
            int version = NbtUtils.getDataVersion(tag);
            return Optional.of(DataFixTypes.PLAYER.updateToCurrentVersion(fixerUpper, tag, version));
        } catch (Exception e) {
            LOGGER.warn("Failed to load mirror player data for {}", player.getGameProfile().name(), e);
            return Optional.empty();
        }
    }
}