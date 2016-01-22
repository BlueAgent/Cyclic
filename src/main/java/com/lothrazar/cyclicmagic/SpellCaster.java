package com.lothrazar.cyclicmagic;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import com.lothrazar.cyclicmagic.item.ItemCyclicWand;
import com.lothrazar.cyclicmagic.spell.ISpell;
import com.lothrazar.cyclicmagic.util.UtilExperience;
import com.lothrazar.cyclicmagic.util.UtilSound;

public class SpellCaster {

	public final float MAXMANA = 1000;
	final int RECHARGE_EXP_COST = 30;
	final int RECHARGE_MANA_AMT = 150;
	
	public boolean isBlockedBySpellTImer(EntityPlayer player) {
		return isBlockedBySpellTImer(PlayerPowerups.get(player));
	}
	public boolean isBlockedBySpellTImer(PlayerPowerups props) { 
		return !(props.getSpellTimer() == 0);
	}
	public boolean tryCastCurrent(World world, EntityPlayer player, BlockPos pos, EnumFacing side) {
		return tryCast(SpellRegistry.caster.getPlayerCurrentISpell(player),world,player,pos,side);
	}

	public boolean tryCast(ISpell spell, World world, EntityPlayer player, BlockPos pos, EnumFacing side) {

		if (isBlockedBySpellTImer(player)) {
			return false;
		}

		if (spell.canPlayerCast(world, player, pos)) {

			if (spell.cast(world, player, pos, side)) {

				// succes should do things like: drain resources, play sounds
				// and particles
				spell.onCastSuccess(world, player, pos);

				PlayerPowerups props = PlayerPowerups.get(player);
				props.setSpellTimer(spell.getCastCooldown());
				return true;
			}
			return false;
			//else the spell was cast, but it had no result			
			// failure does not trigger here. it was cast just didnt work
			// so maybe just was no valid target, or position was blocked/in use
		}
		else {
			// not enough XP (resources)
			spell.onCastFailure(world, player, pos);
			return false;
		}
	}

	public void shiftLeft(EntityPlayer player) {

		PlayerPowerups props = PlayerPowerups.get(player);

		int left = props.prevId(ItemCyclicWand.getSpellCurrent(player.getHeldItem()) );

		ItemCyclicWand.setSpellCurrent(player.getHeldItem(),left);
		UtilSound.playSoundAt(player, UtilSound.orb );
	}

	public void shiftRight(EntityPlayer player) {

		PlayerPowerups props = PlayerPowerups.get(player);
		
		int right = props.nextId(ItemCyclicWand.getSpellCurrent(player.getHeldItem()));
	
		ItemCyclicWand.setSpellCurrent(player.getHeldItem(),right);
		UtilSound.playSoundAt(player, UtilSound.orb );
	}
 
	public void tickSpellTimer(EntityPlayer player) {
		PlayerPowerups props = PlayerPowerups.get(player);
		if (props.getSpellTimer() < 0) {
			props.setSpellTimer(0);
		}
		else if (props.getSpellTimer() > 0) {
			props.setSpellTimer(props.getSpellTimer() - 1);
		}
	}

	public ISpell getPlayerCurrentISpell(EntityPlayer player) {

		//PlayerPowerups props = PlayerPowerups.get(player);

		ISpell current = SpellRegistry.getSpellFromID(ItemCyclicWand.getSpellCurrent(player.getHeldItem()));

		if (current == null) {
			current = SpellRegistry.getDefaultSpell();
		}

		return current;
	}

	public void toggleUnlock(EntityPlayer player, int spell_id) {

		PlayerPowerups props = PlayerPowerups.get(player);
		
		props.toggleOneSpell(spell_id);
	}
	public void rechargeWithExp(EntityPlayer player) {
		PlayerPowerups props = PlayerPowerups.get(player);
		
		if(player.capabilities.isCreativeMode){ //always set full
			PlayerPowerups.get(player).setMana((int)MAXMANA);
		}
		else if(RECHARGE_EXP_COST < UtilExperience.getExpTotal(player) && props.getMana() + RECHARGE_MANA_AMT <= MAXMANA){

			props.rechargeManaBy(RECHARGE_MANA_AMT);

			UtilExperience.drainExp(player, RECHARGE_EXP_COST);
			UtilSound.playSoundAt(player, UtilSound.portal);
		} 
		else{
			UtilSound.playSoundAt(player, UtilSound.fizz);
		}
	}
}
