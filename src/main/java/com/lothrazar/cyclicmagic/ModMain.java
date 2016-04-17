package com.lothrazar.cyclicmagic;

import org.apache.logging.log4j.Logger;
import com.lothrazar.cyclicmagic.event.EventExtendedInventory;
import com.lothrazar.cyclicmagic.gui.GuiHandler;
import com.lothrazar.cyclicmagic.proxy.CommonProxy;
import com.lothrazar.cyclicmagic.registry.*;
import com.lothrazar.cyclicmagic.util.Const;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityVillager.EmeraldForItems;
import net.minecraft.entity.passive.EntityVillager.ITradeList;
import net.minecraft.entity.passive.EntityVillager.PriceInfo;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerCareer;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;

@Mod(modid = Const.MODID, useMetadata = true, canBeDeactivated = false, updateJSON = "https://raw.githubusercontent.com/PrinceOfAmber/CyclicMagic/master/update.json", guiFactory = "com.lothrazar." + Const.MODID + ".gui.IngameConfigHandler")
public class ModMain {

	@SidedProxy(clientSide = "com.lothrazar." + Const.MODID + ".proxy.ClientProxy", serverSide = "com.lothrazar." + Const.MODID + ".proxy.CommonProxy")
	public static CommonProxy						proxy;
	@Instance(value = Const.MODID)
	public static ModMain								instance;
	public static Logger								logger;
	private static Configuration				config;
	public static SimpleNetworkWrapper	network;
	public final static CreativeTabs		TAB	= new CreativeTabs(Const.MODID) {
		                                        @Override
		                                        public Item getTabIconItem() {
			                                        return ItemRegistry.chest_sack;
		                                        }
	                                        };

	@EventHandler
	public void onPreInit(FMLPreInitializationEvent event) {

		logger = event.getModLog();

		config = new Configuration(event.getSuggestedConfigurationFile());
		
		config.load();
		syncConfig();

		network = NetworkRegistry.INSTANCE.newSimpleChannel(Const.MODID);
		
		EventRegistry.register();

		ReflectionRegistry.register();

		ExtraButtonRegistry.register();

		PacketRegistry.register(network);
		
		
		 registerVillagers();
     
     //.init(trades );
   //  (new VillagerCareer(prof, "fisherman")).init(VanillaTrades.trades[0][1]);
    // (new VillagerCareer(prof, "shepherd")).init(VanillaTrades.trades[0][2]);
    // (new VillagerCareer(prof, "fletcher")).init(VanillaTrades.trades[0][3]);
 
     
	}

	private void registerVillagers() {
		//TOO TEST: /summon Villager ~ ~ ~ {Profession:5}
		//kill: /kill @e[type=Villager]
		//CONFIRMED: IT does spawn randomly when using default villager eggs
		
			/*
		//PROBLEM WITH TEXTURE
		in EntityVillager:
    public int getProfession()
    {
        return Math.max(((Integer)this.dataWatcher.get(PROFESSION)).intValue() % 5, 0);
    
    }
    //that %5 needs to be removed, it forces max of 4 profs
    */
		
		VillagerProfession prof = new VillagerProfession(Const.MODRES+"sage", Const.MODRES+"textures/entity/villager/sage.png")/*{
			@Override
      public ResourceLocation getSkin() {

				System.out.println("getSkin"+this.getSkin().getResourceDomain()+"_"+this.getSkin().getResourcePath());
				return super.getSkin();
			}
		}
		*/;
     
		
		VillagerRegistry.instance().register(prof);
		
	//	VillagerProfession test = prof = net.minecraftforge.fml.common.registry.VillagerRegistry.instance().getRegistry().getValue(new ResourceLocation(Const.MODRES+"sage"));
		 
		//System.out.println("test isNull :"+ (test == null));
		
		 
		 final EntityVillager.ITradeList[][] trades = {
				 {
				 new EmeraldForItems(Items.ender_pearl, new PriceInfo(8, 16))
				 },
				 {
				 new EmeraldForItems(Items.beetroot, new PriceInfo(5, 10))
				 },
				 {
				 new EmeraldForItems(Items.wheat_seeds, new PriceInfo(64, 64))
				 },
				 {
				 new EmeraldForItems(Items.poisonous_potato, new PriceInfo(3, 4))
				 },
				 {
				 new EmeraldForItems(Items.spider_eye, new PriceInfo(4, 6))
				 }
		 };
		 
		 VillagerCareer c = new VillagerCareer(prof, "sage_career"){
			 
			 @Override
       public ITradeList[][] getTrades()
       {
           return trades;
       }
		 };
		 
		 
	}

	@EventHandler
	public void onInit(FMLInitializationEvent event) {

		PotionRegistry.register();
		ItemRegistry.register();
		BlockRegistry.register();
		SpellRegistry.register();
		MobSpawningRegistry.register();
		WorldGenRegistry.register();
		FuelRegistry.register();
		SoundRegistry.register();

		if (StackSizeRegistry.enabled) {
			StackSizeRegistry.register();
		}
		if (RecipeAlterRegistry.enabled) {
			RecipeAlterRegistry.register();
		}
		if (RecipeNewRegistry.enabled) {
			RecipeNewRegistry.register();
		}

		proxy.register();
		proxy.registerEvents();

		TileEntityRegistry.register();

		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

		ProjectileRegistry.register(event);
	}

	@EventHandler
	public void onPostInit(FMLPostInitializationEvent event) {

		// registers all plantable crops. the plan is to work with non vanilla data
		// also
		DispenserBehaviorRegistry.register();

	}

	@EventHandler
	public void onServerStarting(FMLServerStartingEvent event) {
		CommandRegistry.register(event);
	}

	public static Configuration getConfig() {
		return config;
	}

	public static void syncConfig() {
		// hit on startup and on change event from
		Configuration c = getConfig();
		WorldGenRegistry.syncConfig(c);
		PotionRegistry.syncConfig(c);
		EventRegistry.syncConfig(c);
		BlockRegistry.syncConfig(c);
		ItemRegistry.syncConfig(c);
		FuelRegistry.syncConfig(c);
		MobSpawningRegistry.syncConfig(c);
		RecipeAlterRegistry.syncConfig(c);
		RecipeNewRegistry.syncConfig(c);
		DispenserBehaviorRegistry.syncConfig(c);
		StackSizeRegistry.syncConfig(c);
		SpellRegistry.syncConfig(c);
		ExtraButtonRegistry.syncConfig(c);
		CommandRegistry.syncConfig(c);

		c.save();
	}

	/* TODO LIST
	 * 
	 * reachplace.name spell name
	 * 
	 * rebalance emerald armor numbers plan.
	 * 
	 * 
	 * remove multi tool
	 * -> instead add a transforming tool? changes between
	 * all the types for emerald
	 * 
	 * all block spells use block sound.
	 * 
	 * All spells that do not use wand inventory moved to other
	 * item.:
	 * : push pull rotate 
	 * 
	 * 
	 * ROTATE: STAIRS: allow switch frop top to bottom
	 * 
	 * PLAY BLOCCK SOUND on rotate spell...+others
	 * 
	 * achievemnets give exp
	 * 
	 * more achieves - inspire by consoles and also my own
	 * 
	 * 
	 * 
	 * add potion brewing!! work with the real brew stands to make new custom potions
	 * 
	 * 
	 * 
	 * add the villager trades that were removed in snapshots//old versions
	 * 
	 * add storage inventory pages - same way the crafting table works
	 * 
	 * BUG: enderman drop block: does it make doubel?
	 * 
	 *UNCRAFITNG: add more slots - horizontal
	 * test/fix hopper interaction
	 * 
	 * config to logspam every enabled feature on startup
	 * 
	 * refactor seed logic into util
	 * 
	 * refactor noteblock/sign into util
	 * 
	 * try to auto detect home biomes of saplings for modded compat
	 * 
	 * 
	 * 
	 * SPELL: bring back ghost - let it put you in new location but only if air
	 * blocks
	 *  
	 * 
	 * 1. text message if we use a build spell but invo is empty
	 * - max and regen in nbt, not config
	 * 
	 * 4. chest give failure message text (only useable on a container)
	 * 
	 * 
	 * //IDEA: make boats float
	 * https://www.reddit.com/r/minecraftsuggestions/comments/4d4ob1/
	 * make_boats_float_again/
	 * 
	 * 
	 * https://www.reddit.com/r/minecraftsuggestions/comments/4chlpo/
	 * add_a_control_option_for_elytra_automatically/
	 * 
	 * 
	 * 
	 */
}
