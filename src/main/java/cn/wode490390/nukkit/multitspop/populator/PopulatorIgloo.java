package cn.wode490390.nukkit.multitspop.populator;

import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.mob.EntityZombieVillager;
import cn.nukkit.entity.passive.EntityVillager;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.biome.EnumBiome;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.generator.populator.type.Populator;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.wode490390.nukkit.multitspop.MultiTemplateStructurePopulator;
import cn.wode490390.nukkit.multitspop.block.BlockTypes;
import cn.wode490390.nukkit.multitspop.block.actor.BlockActorId;
import cn.wode490390.nukkit.multitspop.loot.IglooChest;
import cn.wode490390.nukkit.multitspop.scheduler.ActorSpawnTask;
import cn.wode490390.nukkit.multitspop.scheduler.LootSpawnTask;
import cn.wode490390.nukkit.multitspop.template.ReadOnlyLegacyStructureTemplate;
import cn.wode490390.nukkit.multitspop.template.ReadableStructureTemplate;
import cn.wode490390.nukkit.multitspop.template.StructurePlaceSettings;

import java.util.function.Consumer;

public class PopulatorIgloo extends Populator {

    protected static final ReadableStructureTemplate IGLOO = new ReadOnlyLegacyStructureTemplate().load(MultiTemplateStructurePopulator.loadNBT("structures/igloo/igloo_top_no_trapdoor.nbt"));
    protected static final ReadableStructureTemplate IGLOO_WITH_TRAPDOOR = new ReadOnlyLegacyStructureTemplate().load(MultiTemplateStructurePopulator.loadNBT("structures/igloo/igloo_top_trapdoor.nbt"));

    protected static final ReadableStructureTemplate LADDER = new ReadOnlyLegacyStructureTemplate().load(MultiTemplateStructurePopulator.loadNBT("structures/igloo/igloo_middle.nbt"));
    protected static final ReadableStructureTemplate LABORATORY = new ReadOnlyLegacyStructureTemplate().load(MultiTemplateStructurePopulator.loadNBT("structures/igloo/igloo_bottom.nbt"));

    protected static final StructurePlaceSettings SETTINGS_LADDER = new StructurePlaceSettings().setIgnoreAir(true);

    protected static final int SPACING = 32;
    protected static final int SEPARATION = 8;

    @Override
    public void populate(ChunkManager level, int chunkX, int chunkZ, NukkitRandom random, FullChunk chunk) {
        int biome = chunk.getBiomeId(7, 7);
        if ((biome == EnumBiome.ICE_PLAINS.id || biome == EnumBiome.COLD_TAIGA.id)
                && chunkX == (((chunkX < 0 ? (chunkX - SPACING + 1) : chunkX) / SPACING) * SPACING) + random.nextBoundedInt(SPACING - SEPARATION)
                && chunkZ == (((chunkZ < 0 ? (chunkZ - SPACING + 1) : chunkZ) / SPACING) * SPACING) + random.nextBoundedInt(SPACING - SEPARATION)) {
            ReadableStructureTemplate template;
            boolean hasLaboratory = random.nextBoolean();

            if (hasLaboratory) {
                template = IGLOO_WITH_TRAPDOOR;
            } else {
                template = IGLOO;
            }

            BlockVector3 size = template.getSize();
            int sumY = 0;
            int blockCount = 0;

            for (int x = 0; x < size.getX(); x++) {
                for (int z = 2; z < size.getZ() + 2; z++) {
                    int y = chunk.getHighestBlockAt(x, z);

                    int id = chunk.getBlockId(x, y, z);
                    while (BlockTypes.isPlant[id] && y > 64) {
                        id = chunk.getBlockId(x, --y, z);
                    }

                    sumY += Math.max(64, y);
                    blockCount++;
                }
            }

            BlockVector3 vec = new BlockVector3(chunkX << 4, sumY / blockCount, (chunkZ << 4) + 2);
            template.placeInChunk(chunk, random, vec, StructurePlaceSettings.DEFAULT);

            if (hasLaboratory) {
                template = LADDER;
                int yOffset = template.getSize().getY();
                vec.x += 2;
                vec.z += 4;

                for (int i = 0; i < random.nextBoundedInt(8) + 3; ++i) {
                    vec.y -= yOffset;

                    template.placeInChunk(chunk, random, vec, SETTINGS_LADDER);
                }

                template = LABORATORY;
                vec.x -= 2;
                vec.z -= 6;
                vec.y -= template.getSize().getY();

                template.placeInChunk(chunk, random, vec, new StructurePlaceSettings()
                        .setBlockActorProcessor(getBlockActorProcessor(chunk, random)));
            }

            MultiTemplateStructurePopulator.debug(this.getClass().getSimpleName(), vec.x, vec.y, vec.z);
        }
    }

    protected static Consumer<CompoundTag> getBlockActorProcessor(FullChunk chunk, NukkitRandom random) {
        return nbt -> {
            if (nbt.getString("id").equals(BlockActorId.STRUCTURE_BLOCK)) {
                switch (nbt.getString("metadata")) {
                    case "chest":
                        ListTag<CompoundTag> itemList = new ListTag<>("Items");
                        IglooChest.get().create(itemList, random);

                        Server.getInstance().getScheduler().scheduleDelayedTask(new LootSpawnTask(chunk.getProvider().getLevel(),
                                new BlockVector3(nbt.getInt("x"), nbt.getInt("y") - 1, nbt.getInt("z")), itemList), 2);
                        break;
                    case "Villager":
                        Server.getInstance().getScheduler().scheduleDelayedTask(new ActorSpawnTask(chunk.getProvider().getLevel(),
                                Entity.getDefaultNBT(new Vector3(nbt.getInt("x") + 0.5, nbt.getInt("y"), nbt.getInt("z") + 0.5))
                                        .putString("id", String.valueOf(EntityVillager.NETWORK_ID))), 2);
                        break;
                    case "Zombie Villager":
                        Server.getInstance().getScheduler().scheduleDelayedTask(new ActorSpawnTask(chunk.getProvider().getLevel(),
                                Entity.getDefaultNBT(new Vector3(nbt.getInt("x") + 0.5, nbt.getInt("y"), nbt.getInt("z") + 0.5))
                                        .putString("id", String.valueOf(EntityZombieVillager.NETWORK_ID))), 2);
                        break;
                }
            }
        };
    }

    public static void init() {
        //NOOP
    }
}
