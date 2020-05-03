package cn.wode490390.nukkit.multitspop;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.level.ChunkPopulateEvent;
import cn.nukkit.event.level.LevelLoadEvent;
import cn.nukkit.event.level.LevelUnloadEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.level.generator.populator.type.Populator;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.plugin.PluginBase;
import cn.wode490390.nukkit.multitspop.populator.PopulatorIgloo;
import cn.wode490390.nukkit.multitspop.populator.PopulatorOceanRuin;
import cn.wode490390.nukkit.multitspop.populator.PopulatorPillagerOutpost;
import cn.wode490390.nukkit.multitspop.scheduler.ChunkPopulationTask;
import cn.wode490390.nukkit.multitspop.util.MetricsLite;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class MultiTemplateStructurePopulator extends PluginBase implements Listener {

    private static final boolean DEBUG = false;

    private static MultiTemplateStructurePopulator INSTANCE;

    private final Map<Level, List<Populator>> populators = Maps.newHashMap();

    @Override
    public void onLoad() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        try {
            new MetricsLite(this, 7389);
        } catch (Throwable ignore) {

        }

        PopulatorIgloo.init();
        PopulatorPillagerOutpost.init();
        PopulatorOceanRuin.init();

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onLevelLoad(LevelLoadEvent event) {
        List<Populator> populators = Lists.newArrayList();
        Level level = event.getLevel();
        Generator generator = level.getGenerator();
        if (generator.getId() != Generator.TYPE_FLAT && generator.getDimension() == Level.DIMENSION_OVERWORLD) {
            PopulatorIgloo igloo = new PopulatorIgloo();
            populators.add(igloo);

            PopulatorPillagerOutpost outpost = new PopulatorPillagerOutpost();
            populators.add(outpost);

            PopulatorOceanRuin ruin = new PopulatorOceanRuin();
            populators.add(ruin);
        }
        this.populators.put(level, populators);
    }

    @EventHandler
    public void onChunkPopulate(ChunkPopulateEvent event) {
        Level level = event.getLevel();
        List<Populator> populators = this.populators.get(level);
        if (populators != null) {
            this.getServer().getScheduler().scheduleAsyncTask(this, new ChunkPopulationTask(level, event.getChunk(), populators));
        }
    }

    @EventHandler
    public void onLevelUnload(LevelUnloadEvent event) {
        this.populators.remove(event.getLevel());
    }

    public static CompoundTag loadNBT(String path) {
        try (InputStream inputStream = MultiTemplateStructurePopulator.class.getClassLoader().getResourceAsStream(path)) {
            return NBTIO.readCompressed(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static MultiTemplateStructurePopulator getInstance() {
        return INSTANCE;
    }

    public static void debug(Object... objs) {
        if (DEBUG) {
            try {
                StringJoiner joiner = new StringJoiner(" ");
                for (Object obj : objs) {
                    joiner.add(String.valueOf(obj));
                }
                INSTANCE.getLogger().warning(joiner.toString());
            } catch (Throwable ignore) {

            }
        }
    }
}
