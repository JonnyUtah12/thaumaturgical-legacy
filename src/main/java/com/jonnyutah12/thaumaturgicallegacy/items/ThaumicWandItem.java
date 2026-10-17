package com.jonnyutah12.thaumaturgicallegacy.items;

import com.jonnyutah12.thaumaturgicallegacy.blockentity.VisTapBlockEntity;
import com.jonnyutah12.thaumaturgicallegacy.accessory.WornAccessories;
import com.jonnyutah12.thaumaturgicallegacy.event.ResearchEvents;
import com.jonnyutah12.thaumaturgicallegacy.magic.Spell;
import com.jonnyutah12.thaumaturgicallegacy.magic.SpellCatalog;
import com.jonnyutah12.thaumaturgicallegacy.magic.SpellDefinition;
import com.jonnyutah12.thaumaturgicallegacy.magic.VisStorage;
import com.jonnyutah12.thaumaturgicallegacy.magic.research.ResearchCatalog;
import com.jonnyutah12.thaumaturgicallegacy.registration.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Thaumic Wand: the casting tool of the first playable magic system.
 *
 * <ul>
 * <li>Right-click: cast the active spell (costs vis, item cooldown applies).</li>
 * <li>Shift + right-click: cycle the active spell.</li>
 * <li>Right-click a Vis Tap while holding the wand: channel stored vis from
 * the tap into the wand.</li>
 * </ul>
 *
 * Vis and active-spell state are item data components; the numeric rules live
 * in the pure {@code magic} package so they are unit-testable without a game
 * bootstrap.
 */
public class ThaumicWandItem extends Item {

    /** Vis capacity of the basic wand. */
    public static final int CAPACITY = 50;
    /** Cooldown (ticks) after cycling spells, prevents accidental double-casts. */
    private static final int CYCLE_COOLDOWN_TICKS = 5;
    /** Cooldown (ticks) shown when trying to cast an unaffordable spell. */
    private static final int EMPTY_COOLDOWN_TICKS = 10;

    public ThaumicWandItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (player.isCrouching()) {
            cycleSpell(stack, player);
            return InteractionResultHolder.success(stack);
        }

        Spell spell = activeSpell(stack);
        SpellDefinition definition = SpellCatalog.definition(spell);
        // Research gate (TL-017): spells beyond Firebolt must be known to the
        // player. Defensive check — the cycle below only selects known spells,
        // but a stack can carry an ordinal from an older world or commands.
        if (!ResearchEvents.isSpellKnown(player, spell)) {
            player.displayClientMessage(
                    Component.translatable("message.thaumaturgicallegacy.spell_locked"), true);
            player.getCooldowns().addCooldown(this, EMPTY_COOLDOWN_TICKS);
            return InteractionResultHolder.success(stack);
        }
        VisStorage vis = readVis(stack);
        // Worn accessories (vis amulet) discount the cost; the query is
        // provider-independent (TL-019, docs/architecture/ACCESSORY_SYSTEM.md).
        int cost = WornAccessories.visCostFor(player, definition.visCost());
        if (!vis.withdraw(cost)) {
            player.displayClientMessage(
                    Component.translatable("message.thaumaturgicallegacy.wand_empty"), true);
            player.getCooldowns().addCooldown(this, EMPTY_COOLDOWN_TICKS);
            return InteractionResultHolder.success(stack);
        }

        cast((ServerLevel) level, player, definition);
        writeVis(stack, vis);
        player.getCooldowns().addCooldown(this, definition.cooldownTicks());
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof VisTapBlockEntity tap) {
            ItemStack stack = context.getItemInHand();
            if (!level.isClientSide) {
                VisStorage vis = readVis(stack);
                if (vis.isFull()) {
                    context.getPlayer().displayClientMessage(
                            Component.translatable("message.thaumaturgicallegacy.wand_full"), true);
                } else {
                    int transferred = tap.draw(vis.remaining());
                    if (transferred <= 0) {
                        context.getPlayer().displayClientMessage(
                                Component.translatable("message.thaumaturgicallegacy.tap_empty"), true);
                    } else {
                        vis.deposit(transferred);
                        writeVis(stack, vis);
                        context.getPlayer().displayClientMessage(
                                Component.translatable("message.thaumaturgicallegacy.tap_charged", transferred), true);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        VisStorage vis = readVis(stack);
        tooltipComponents.add(Component
                .translatable("tooltip.thaumaturgicallegacy.vis", vis.amount(), vis.capacity())
                .withStyle(ChatFormatting.AQUA));
        Spell spell = activeSpell(stack);
        tooltipComponents.add(Component
                .translatable("tooltip.thaumaturgicallegacy.spell",
                        Component.translatable(spell.descriptionId()).getString())
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component
                .translatable("tooltip.thaumaturgicallegacy.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    // --- spell application -----------------------------------------------------

    private static void cast(ServerLevel level, Player player, SpellDefinition definition) {
        Vec3 start = player.getEyePosition(1.0F);
        HitResult hit = player.pick(definition.range(), 1.0F, false);
        Vec3 end = hit.getLocation();

        Vec3 direction = end.subtract(start);
        double length = direction.length();
        int steps = Math.max(1, (int) (length * 4.0));
        for (int i = 1; i <= steps; i++) {
            Vec3 point = start.add(direction.scale((double) i / steps));
            level.sendParticles(particleFor(definition.spell()),
                    point.x, point.y, point.z, 0, 0.0, 0.0, 0.0, 0.0);
        }
        level.sendParticles(particleFor(definition.spell()),
                end.x, end.y, end.z, 8, 0.12, 0.12, 0.12, 0.04);

        if (hit.getType() == HitResult.Type.MISS || !(hit instanceof EntityHitResult entityHit)) {
            return;
        }
        if (!(entityHit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (definition.damage() > 0.0F) {
            target.hurt(player.damageSources().indirectMagic(player, player), definition.damage());
        }
        if (definition.fireSeconds() > 0) {
            target.igniteForSeconds(definition.fireSeconds());
        }
        if (definition.slowTicks() > 0) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    definition.slowTicks(), definition.slowAmplifier()));
        }
    }

    private static net.minecraft.core.particles.ParticleOptions particleFor(Spell spell) {
        return switch (spell) {
            case FIREBOLT -> net.minecraft.core.particles.ParticleTypes.FLAME;
            case FROST_LANCE -> net.minecraft.core.particles.ParticleTypes.SNOWFLAKE;
        };
    }

    // --- component access ------------------------------------------------------

    public static Spell activeSpell(ItemStack stack) {
        return Spell.byOrdinal(stack.getOrDefault(ModDataComponents.ACTIVE_SPELL.get(), 0));
    }

    private static void cycleSpell(ItemStack stack, Player player) {
        // Only research-known spells participate in the cycle (TL-017).
        List<Spell> known = knownSpells(player);
        if (known.size() < 2) {
            player.displayClientMessage(
                    Component.translatable("message.thaumaturgicallegacy.no_other_spells"), true);
            return;
        }
        int index = known.indexOf(activeSpell(stack));
        Spell next = known.get((index + 1) % known.size());
        stack.set(ModDataComponents.ACTIVE_SPELL.get(), next.ordinal());
        player.getCooldowns().addCooldown(stack.getItem(), CYCLE_COOLDOWN_TICKS);
        player.displayClientMessage(
                Component.translatable("message.thaumaturgicallegacy.spell_selected",
                        Component.translatable(next.descriptionId())),
                true);
    }

    /** Catalog order filtered by the player's research; Firebolt is always in. */
    private static List<Spell> knownSpells(Player player) {
        java.util.Set<Spell> unlocked = ResearchCatalog.spellsUnlockedBy(
                ResearchEvents.knowledge(player).known());
        List<Spell> known = new java.util.ArrayList<>();
        for (Spell spell : Spell.values()) {
            if (spell == Spell.FIREBOLT || unlocked.contains(spell)) {
                known.add(spell);
            }
        }
        return known;
    }

    private static VisStorage readVis(ItemStack stack) {
        VisStorage storage = new VisStorage(CAPACITY);
        storage.setAmount(stack.getOrDefault(ModDataComponents.VIS_STORED.get(), 0));
        return storage;
    }

    private static void writeVis(ItemStack stack, VisStorage storage) {
        stack.set(ModDataComponents.VIS_STORED.get(), storage.amount());
    }
}
