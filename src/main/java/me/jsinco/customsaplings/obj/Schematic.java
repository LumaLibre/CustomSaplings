package me.jsinco.customsaplings.obj;

// Credit: https://gist.github.com/Crypnotic/3177b30fc82b8c58fb376239ca400074

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;


public record Schematic(Clipboard clipboard) {

    public void paste(org.bukkit.Location target) {
        if (target.getWorld() == null) {
            throw new IllegalArgumentException("Target world for pasting a schematic cannot be null!");
        }
        World world = BukkitAdapter.adapt(target.getWorld());
        Location location = BukkitAdapter.adapt(target);

        try (EditSession session = WorldEdit.getInstance().newEditSession(world)) {
            Operation operation = new ClipboardHolder(clipboard).createPaste(session)
                    .to(location.toVector().toBlockPoint())
                    .ignoreAirBlocks(true)
                    .build();

            Operations.complete(operation);
        } catch (WorldEditException exception) {
            exception.printStackTrace();
        }
    }

    public static Optional<Schematic> load(File file) {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            return Optional.empty();
        }

        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return Optional.of(new Schematic(reader.read()));
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return Optional.empty();
    }
}
