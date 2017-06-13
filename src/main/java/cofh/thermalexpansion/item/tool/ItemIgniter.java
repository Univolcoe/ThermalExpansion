package cofh.thermalexpansion.item.tool;

import codechicken.lib.raytracer.RayTracer;
import cofh.lib.util.helpers.BlockHelper;
import cofh.lib.util.helpers.MathHelper;
import cofh.lib.util.helpers.ServerHelper;
import net.minecraft.block.BlockTNT;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import java.util.List;

public class ItemIgniter extends ItemEnergyContainerBase {

	public int range = 32;

	public ItemIgniter() {

		super("igniter");
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {

		ItemStack stack = player.getHeldItem(hand);
		if (!player.capabilities.isCreativeMode && extractEnergy(stack, energyPerUse, true) != energyPerUse) {
			return new ActionResult<>(EnumActionResult.FAIL, stack);
		}
		RayTraceResult traceResult = player.isSneaking() ? RayTracer.retrace(player, true) : RayTracer.retrace(player, range, true);

		if (traceResult != null) {
			boolean success = false;
			BlockPos pos = traceResult.getBlockPos();
			BlockPos offsetPos = traceResult.getBlockPos().offset(traceResult.sideHit);

			world.playSound(null, offsetPos.getX() + 0.5, offsetPos.getY() + 0.5, offsetPos.getZ() + 0.5, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 0.2F, MathHelper.RANDOM.nextFloat() * 0.4F + 0.8F);

			if (ServerHelper.isServerWorld(world)) {
				IBlockState hitState = world.getBlockState(pos);
				if (hitState.getBlock() == Blocks.TNT) {
					world.setBlockToAir(pos);
					((BlockTNT) hitState.getBlock()).explode(world, pos, hitState.withProperty(BlockTNT.EXPLODE, true), player);
				} else {
					AxisAlignedBB axisalignedbb = BlockHelper.getAdjacentAABBForSide(traceResult);
					List<EntityCreeper> list = world.getEntitiesWithinAABB(EntityCreeper.class, axisalignedbb);
					if (!list.isEmpty()) {
						for (EntityCreeper creeper : list) {
							creeper.ignite();
						}
						success = true;
					} else {
						IBlockState offsetState = world.getBlockState(offsetPos);
						if (offsetState.getBlock() != Blocks.FIRE && (offsetState.getBlock().isAir(offsetState, world, offsetPos) || offsetState.getMaterial().isReplaceable())) {
							success = world.setBlockState(offsetPos, Blocks.FIRE.getDefaultState());
						}
					}
				}
				if (success) {
					player.openContainer.detectAndSendChanges();
					((EntityPlayerMP) player).updateCraftingInventory(player.openContainer, player.openContainer.getInventory());

					if (!player.capabilities.isCreativeMode) {
						extractEnergy(stack, energyPerUse, false);
					}
				}
			}
			player.swingArm(hand);
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}

}
