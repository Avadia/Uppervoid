package com.geekpower14.uppervoid.block;

import com.geekpower14.uppervoid.arena.ArenaStatisticsHelper;
import com.google.gson.JsonArray;
import net.samagames.api.SamaGamesAPI;
import net.samagames.tools.ParticleEffect;
import net.samagames.tools.SimpleBlock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.UUID;

public class BlockGroup
{
    private static final SimpleBlock VOID = new SimpleBlock(Material.AIR);

    private final SimpleBlock blockFine;
    private final SimpleBlock blockWarning;
    private final SimpleBlock blockCritical;

    public BlockGroup(JsonArray group)
    {
        String[] blockFineData = group.get(0).getAsString().split(", ");
        this.blockFine = new SimpleBlock(Material.matchMaterial(blockFineData[0]), Integer.valueOf(blockFineData[1]));

        String[] blockWarningData = group.get(1).getAsString().split(", ");
        this.blockWarning = new SimpleBlock(Material.matchMaterial(blockWarningData[0]), Integer.valueOf(blockWarningData[1]));

        String[] blockCriticalData = group.get(2).getAsString().split(", ");
        this.blockCritical = new SimpleBlock(Material.matchMaterial(blockCriticalData[0]), Integer.valueOf(blockCriticalData[1]));
    }

    public boolean isThis(Block block)
    {
        if (block == null)
            return false;

        if (this.is(block, this.blockFine))
            return true;
        else if (this.is(block, this.blockWarning))
            return true;
        else if (this.is(block, this.blockCritical))
            return true;

        return false;
    }

    public boolean damage(UUID damager, Block block, int damage)
    {
        boolean result = false;

        for (int i = 0; i < damage; i++)
            if (this.setNext(damager, block))
                result = true;

        if(result)
        {
            ParticleEffect.VILLAGER_HAPPY.display(0.2F, 0.1F, 0.2F, 10F, 1, block.getLocation().add(0.5D, 1.1D, 0.5D), 50);
            ParticleEffect.BLOCK_CRACK.display(new ParticleEffect.BlockData(block.getType(), block.getData()), 0.2F, 0.3F, 0.2F, 10F, 5, block.getLocation().add(0.5D, 1.1D, 0.5D), 50);
        }

        return result;
    }

    public boolean repair(UUID damager, Block block)
    {
        boolean result = this.setPrevious(block);

        if (result)
        {
            ParticleEffect.VILLAGER_HAPPY.display(0.2F, 0.1F, 0.2F, 10F, 1, block.getLocation().add(0.5D, 1.1D, 0.5D), 50);
            ParticleEffect.HEART.display(0.2F, 0.3F, 0.2F, 10F, 5, block.getLocation().add(0.5D, 1.1D, 0.5D), 50);
        }

        return result;
    }

    private boolean setPrevious(Block block)
    {
        if (block == null)
            return false;

        if (this.is(block, this.blockFine))
        {
            return true;
        }
        else if (this.is(block, this.blockWarning))
        {
            this.set(block, this.blockFine);
            return true;
        }
        else if (this.is(block, this.blockCritical))
        {
            this.set(block, this.blockWarning);
            return true;
        }

        return false;
    }

    private boolean setNext(UUID damager, Block block)
    {
        if (block == null)
            return false;

        if (this.is(block, this.blockFine))
        {
            this.set(block, this.blockWarning);
            return true;
        }
        else if (this.is(block, this.blockWarning))
        {
            this.set(block, this.blockCritical);
            return true;
        }
        else if (this.is(block, this.blockCritical))
        {
            this.set(block, VOID);
            this.set(block.getRelative(BlockFace.DOWN), VOID);

            if (damager != null)
                ((ArenaStatisticsHelper) SamaGamesAPI.get().getGameManager().getGameStatisticsHelper()).increaseBlocks(damager);

            return true;
        }

        return false;
    }

    @SuppressWarnings("deprecation")
    private void set(Block block, SimpleBlock simpleBlock)
    {
        block.setType(simpleBlock.getType());
        block.setData(simpleBlock.getData());
    }

    @SuppressWarnings("deprecation")
    private boolean is(Block block, SimpleBlock modal)
    {
        return block.getType() == modal.getType() && block.getData() == modal.getData();
    }
}
