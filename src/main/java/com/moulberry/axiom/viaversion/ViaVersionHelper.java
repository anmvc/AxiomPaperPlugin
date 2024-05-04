package com.moulberry.axiom.viaversion;

import com.moulberry.axiom.buffer.BlockBuffer;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.BiMappings;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.SharedConstants;
import net.minecraft.core.IdMapper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ViaVersionHelper {

    private static final Int2ObjectOpenHashMap<IdMapper<BlockState>> blockRegistryCache = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<String> blockRegistryErrorCache = new Int2ObjectOpenHashMap<>();

    public static IdMapper<BlockState> getBlockRegistryForVersion(IdMapper<BlockState> mapper, int playerVersion) {
        if (blockRegistryErrorCache.containsKey(playerVersion)) {
            throw new RuntimeException(blockRegistryErrorCache.get(playerVersion));
        }
        if (blockRegistryCache.containsKey(playerVersion)) {
            return blockRegistryCache.get(playerVersion);
        }

        List<ProtocolPathEntry> path = Via.getManager().getProtocolManager().getProtocolPath(playerVersion,
            SharedConstants.getProtocolVersion());

        if (path == null) {
            blockRegistryErrorCache.put(playerVersion, "Failed to find protocol path");
            throw new RuntimeException("Failed to find protocol path");
        }

        for (int i = path.size()-1; i >= 0; i--) {
            ProtocolPathEntry protocolPathEntry = path.get(i);

            MappingData mappingData = protocolPathEntry.protocol().getMappingData();

            if (mappingData == null) {
                blockRegistryErrorCache.put(playerVersion, "Failed to load mapping data (" + protocolPathEntry + ")");
                throw new RuntimeException("Failed to load mapping data (" + protocolPathEntry + ")");
            }

            Mappings blockStateMappings = mappingData.getBlockStateMappings();

            if (blockStateMappings == null) {
                blockRegistryErrorCache.put(playerVersion, "Failed to load BlockState mappings (" + protocolPathEntry + ")");
                throw new RuntimeException("Failed to load BlockState mappings (" + protocolPathEntry + ")");
            }

            mapper = ViaVersionHelper.applyMappings(mapper, blockStateMappings);
        }

        blockRegistryCache.put(playerVersion, mapper);
        return mapper;
    }

    public static IdMapper<BlockState> applyMappings(IdMapper<BlockState> registry, Mappings mappings) {
        IdMapper<BlockState> newBlockRegistry = new IdMapper<>();

        // Add empty mappings for non-existent blocks
        int size = mappings.mappedSize();
        for (int i = 0; i < size; i++) {
            newBlockRegistry.addMapping(BlockBuffer.EMPTY_STATE, i);
        }

        // Map blocks
        for (int i = 0; i < registry.size(); i++) {
            BlockState blockState = registry.byId(i);

            if (blockState != null) {
                int newId = mappings.getNewId(i);
                if (newId >= 0) {
                    newBlockRegistry.addMapping(blockState, newId);
                }
            }
        }

        // Ensure block -> id is correct for the empty state
        int newEmptyStateId = mappings.getNewId(registry.getId(BlockBuffer.EMPTY_STATE));
        if (newEmptyStateId >= 0) {
            newBlockRegistry.addMapping(BlockBuffer.EMPTY_STATE, newEmptyStateId);
        }

        return newBlockRegistry;
    }

}
