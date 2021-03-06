/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (C) 2014-2018 Sam Bassett (aka Lothrazar)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.lothrazar.cyclicmagic.item.dynamite;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SuppressWarnings("unused")
public class ExplosionBlockSafe extends Explosion {

  /** whether or not the explosion sets fire to blocks around it */
  private final boolean isFlaming;
  /** whether or not this explosion spawns smoke particles */
  private final boolean isSmoking;
  private final Random explosionRNG;
  private final World world;
  private final double explosionX;
  private final double explosionY;
  private final double explosionZ;
  private final Entity exploder;
  private final float explosionSize;
  private final List<BlockPos> affectedBlockPositions;
  private final Map<EntityPlayer, Vec3d> playerKnockbackMap;
  private final Vec3d position;

  @SideOnly(Side.CLIENT)
  public ExplosionBlockSafe(World worldIn, Entity entityIn, double x, double y, double z, float size, List<BlockPos> affectedPositions) {
    this(worldIn, entityIn, x, y, z, size, false, true, affectedPositions);
  }

  @SideOnly(Side.CLIENT)
  public ExplosionBlockSafe(World worldIn, Entity entityIn, double x, double y, double z, float size, boolean flaming, boolean smoking, List<BlockPos> affectedPositions) {
    this(worldIn, entityIn, x, y, z, size, flaming, smoking);
    this.affectedBlockPositions.addAll(affectedPositions);
  }

  public ExplosionBlockSafe(World worldIn, Entity entityIn, double x, double y, double z, float size, boolean flaming, boolean smoking) {
    super(worldIn, entityIn, x, y, z, size, flaming, smoking);
    this.explosionRNG = new Random();
    this.affectedBlockPositions = Lists.<BlockPos> newArrayList();
    this.playerKnockbackMap = Maps.<EntityPlayer, Vec3d> newHashMap();
    this.world = worldIn;
    this.exploder = entityIn;
    this.explosionSize = size;
    this.explosionX = x;
    this.explosionY = y;
    this.explosionZ = z;
    this.isFlaming = flaming;
    this.isSmoking = smoking;
    this.position = new Vec3d(explosionX, explosionY, explosionZ);
  }

  /**
   * Does the first part of the explosion (destroy blocks)
   */
  @Override
  public void doExplosionA() {
    Set<BlockPos> set = Sets.<BlockPos> newHashSet();
    int i = 16;
    for (int j = 0; j < 16; ++j) {
      for (int k = 0; k < 16; ++k) {
        for (int l = 0; l < 16; ++l) {
          if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
            double d0 = j / 15.0F * 2.0F - 1.0F;
            double d1 = k / 15.0F * 2.0F - 1.0F;
            double d2 = l / 15.0F * 2.0F - 1.0F;
            double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
            d0 = d0 / d3;
            d1 = d1 / d3;
            d2 = d2 / d3;
            float f = this.explosionSize * (0.7F + this.world.rand.nextFloat() * 0.6F);
            double d4 = this.explosionX;
            double d6 = this.explosionY;
            double d8 = this.explosionZ;
            for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
              BlockPos blockpos = new BlockPos(d4, d6, d8);
              IBlockState iblockstate = this.world.getBlockState(blockpos);
              if (iblockstate.getMaterial() != Material.AIR) {
                float f2 = this.exploder != null ? this.exploder.getExplosionResistance(this, this.world, blockpos, iblockstate) : iblockstate.getBlock().getExplosionResistance(world, blockpos, (Entity) null, this);
                f -= (f2 + 0.3F) * 0.3F;
              }
              if (f > 0.0F && (this.exploder == null || this.exploder.canExplosionDestroyBlock(this, this.world, blockpos, iblockstate, f))) {
                set.add(blockpos);
              }
              d4 += d0 * 0.30000001192092896D;
              d6 += d1 * 0.30000001192092896D;
              d8 += d2 * 0.30000001192092896D;
            }
          }
        }
      }
    }
    this.affectedBlockPositions.addAll(set);//ONLY THIS line is different. dont affect blocks
    float f3 = this.explosionSize * 2.0F;
    int k1 = MathHelper.floor(this.explosionX - f3 - 1.0D);
    int l1 = MathHelper.floor(this.explosionX + f3 + 1.0D);
    int i2 = MathHelper.floor(this.explosionY - f3 - 1.0D);
    int i1 = MathHelper.floor(this.explosionY + f3 + 1.0D);
    int j2 = MathHelper.floor(this.explosionZ - f3 - 1.0D);
    int j1 = MathHelper.floor(this.explosionZ + f3 + 1.0D);
    List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this.exploder, new AxisAlignedBB(k1, i2, j2, l1, i1, j1));
    net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(this.world, this, list, f3);
    Vec3d vec3d = new Vec3d(this.explosionX, this.explosionY, this.explosionZ);
    for (int k2 = 0; k2 < list.size(); ++k2) {
      Entity entity = list.get(k2);
      if (!entity.isImmuneToExplosions()) {
        double d12 = entity.getDistance(this.explosionX, this.explosionY, this.explosionZ) / f3;
        if (d12 <= 1.0D) {
          double d5 = entity.posX - this.explosionX;
          double d7 = entity.posY + entity.getEyeHeight() - this.explosionY;
          double d9 = entity.posZ - this.explosionZ;
          double d13 = MathHelper.sqrt(d5 * d5 + d7 * d7 + d9 * d9);
          if (d13 != 0.0D) {
            d5 = d5 / d13;
            d7 = d7 / d13;
            d9 = d9 / d13;
            double d14 = this.world.getBlockDensity(vec3d, entity.getEntityBoundingBox());
            double d10 = (1.0D - d12) * d14;
            if (entity instanceof EntityPlayer == false) {//special: do not harm players at all
              entity.attackEntityFrom(DamageSource.causeExplosionDamage(this), ((int) ((d10 * d10 + d10) / 2.0D * 7.0D * f3 + 1.0D)));
            }
            double d11 = d10;
            if (entity instanceof EntityLivingBase) {
              d11 = EnchantmentProtection.getBlastDamageReduction((EntityLivingBase) entity, d10);
            }
            entity.motionX += d5 * d11;
            entity.motionY += d7 * d11;
            entity.motionZ += d9 * d11;
            if (entity instanceof EntityPlayer) {
              EntityPlayer entityplayer = (EntityPlayer) entity;
              if (!entityplayer.isSpectator() && (!entityplayer.isCreative() || !entityplayer.capabilities.isFlying)) {
                this.playerKnockbackMap.put(entityplayer, new Vec3d(d5 * d10, d7 * d10, d9 * d10));
              }
            }
          }
        }
      }
    }
  }

  /**
   * Does the second part of the explosion (sound, particles, drop spawn)
   */
  @Override
  public void doExplosionB(boolean spawnParticles) {
    this.world.playSound((EntityPlayer) null, this.explosionX, this.explosionY, this.explosionZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, (1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F) * 0.7F);
    if (this.explosionSize >= 2.0F && this.isSmoking) {
      this.world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, this.explosionX, this.explosionY, this.explosionZ, 1.0D, 0.0D, 0.0D, new int[0]);
    }
    else {
      this.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, this.explosionX, this.explosionY, this.explosionZ, 1.0D, 0.0D, 0.0D, new int[0]);
    }
    if (this.isSmoking) {
      for (BlockPos blockpos : this.affectedBlockPositions) {
        IBlockState iblockstate = this.world.getBlockState(blockpos);
        Block block = iblockstate.getBlock();
        if (spawnParticles) {
          double d0 = blockpos.getX() + this.world.rand.nextFloat();
          double d1 = blockpos.getY() + this.world.rand.nextFloat();
          double d2 = blockpos.getZ() + this.world.rand.nextFloat();
          double d3 = d0 - this.explosionX;
          double d4 = d1 - this.explosionY;
          double d5 = d2 - this.explosionZ;
          double d6 = MathHelper.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
          d3 = d3 / d6;
          d4 = d4 / d6;
          d5 = d5 / d6;
          double d7 = 0.5D / (d6 / this.explosionSize + 0.1D);
          d7 = d7 * (this.world.rand.nextFloat() * this.world.rand.nextFloat() + 0.3F);
          d3 = d3 * d7;
          d4 = d4 * d7;
          d5 = d5 * d7;
          this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, (d0 + this.explosionX) / 2.0D, (d1 + this.explosionY) / 2.0D, (d2 + this.explosionZ) / 2.0D, d3, d4, d5, new int[0]);
          this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d0, d1, d2, d3, d4, d5, new int[0]);
        }
        //block safe means: no fire or block destruction
        //              if (iblockstate.getMaterial() != Material.AIR)
        //              {
        //                  if (block.canDropFromExplosion(this))
        //                  {
        //                      block.dropBlockAsItemWithChance(this.world, blockpos, this.world.getBlockState(blockpos), 1.0F / this.explosionSize, 0);
        //                  }
        //
        //                  block.onBlockExploded(this.world, blockpos, this);
        //              }
      }
    }
    //      if (this.isFlaming)
    //      {
    //          for (BlockPos blockpos1 : this.affectedBlockPositions)
    //          {
    //              if (this.world.getBlockState(blockpos1).getMaterial() == Material.AIR && this.world.getBlockState(blockpos1.down()).isFullBlock() && this.explosionRNG.nextInt(3) == 0)
    //              {
    //                  this.world.setBlockState(blockpos1, Blocks.FIRE.getDefaultState());
    //              }
    //          }
    //      }
  }

  public Entity getExploder() {
    return exploder;
  }
}
