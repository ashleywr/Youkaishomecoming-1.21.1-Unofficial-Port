package dev.xkmc.youkaishomecoming.compat.curios;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import dev.xkmc.youkaishomecoming.content.item.character.TouhouHatItem;
import dev.xkmc.youkaishomecoming.init.data.GLModConfig;
import dev.xkmc.youkaishomecoming.init.data.GLTagGen;
import dev.xkmc.youkaishomecoming.init.registrate.GLItems;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.TriState;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioCanEquipEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class CuriosCompat {

    /**
     * Master switch for Curios accessory recognition. When false, this mod's
     * hats / hairbands / accessories are recognized only in vanilla equipment slots
     * even if Curios is loaded. Capability registration is unaffected so Curios's
     * own UI keeps working; we just stop our gameplay code from reacting to items
     * placed in Curios slots.
     */
    private static boolean enabled() {
        return GLModConfig.SERVER.curiosSupportEnabled.get();
    }

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> CURIOS_HEAD =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("curios", "head"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> CURIOS_BACK =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("curios", "back"));

    /**
     * Force-allow this mod's accessories into their tagged Curios slots even when a
     * third-party mod (e.g. GoetyDelight) attaches a narrow validator to the head slot
     * that would otherwise reject them.
     */
    @SubscribeEvent
    public static void onCurioCanEquip(CurioCanEquipEvent event) {
        if (!enabled()) return;
        if (event.getEquipResult() == TriState.TRUE) return;
        String id = event.getSlotContext().identifier();
        ItemStack stack = event.getStack();
        if ("head".equals(id) && stack.is(CURIOS_HEAD)) {
            event.setEquipResult(TriState.TRUE);
        } else if ("back".equals(id) && stack.is(CURIOS_BACK)) {
            event.setEquipResult(TriState.TRUE);
        }
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (var entry : GLItems.HAT_ITEMS) {
            event.registerItem(CuriosCapability.ITEM,
                    (stack, ctx) -> new HatCurio(stack),
                    entry.get());
        }
        event.registerItem(CuriosCapability.ITEM,
                (stack, ctx) -> new SimpleCurio(stack),
                dev.xkmc.youkaishomecoming.init.registrate.YHItems.CAMELLIA.get());
    }

    public static boolean hasCurioItem(Player player, Item item) {
        if (!enabled()) return false;
        return CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(item))
                .isPresent();
    }

    public static boolean hasCurioHairband(Player player) {
        if (!enabled()) return false;
        return CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(s -> s.getItem() instanceof dev.xkmc.youkaishomecoming.content.item.character.ReimuHairbandItem))
                .isPresent();
    }

    public static boolean hasCurioKoishiHat(Player player) {
        if (!enabled()) return false;
        return CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(s -> s.getItem() instanceof dev.xkmc.youkaishomecoming.content.item.character.KoishiHatItem))
                .isPresent();
    }

    public static TouhouHatItem findCurioHat(Player player) {
        if (!enabled()) return null;
        return CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(s -> s.getItem() instanceof TouhouHatItem))
                .map(r -> (TouhouHatItem) r.stack().getItem())
                .orElse(null);
    }

    public static boolean anyCurioHatSupports(Player player, net.minecraft.world.item.DyeColor color) {
        if (!enabled()) return false;
        return CuriosApi.getCuriosInventory(player)
                .map(h -> {
                    var list = h.findCurios(s -> s.getItem() instanceof TouhouHatItem);
                    for (var result : list) {
                        if (((TouhouHatItem) result.stack().getItem()).support(color)) return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    public static boolean hasCurio(LivingEntity le, Item item, boolean checkRender) {
        if (!enabled()) return false;
        return CuriosApi.getCuriosInventory(le)
                .flatMap(h -> h.findFirstCurio(item))
                .map(result -> !checkRender || result.slotContext().visible())
                .orElse(false);
    }

    public static boolean hasAnyWings(LivingEntity le) {
        if (!enabled()) return false;
        return CuriosApi.getCuriosInventory(le)
                .flatMap(h -> h.findFirstCurio(s -> s.is(GLTagGen.TOUHOU_WINGS)))
                .isPresent();
    }

    public static void applyDanmakuFromCurios(Player player,
            dev.xkmc.danmakuapi.api.DanmakuDamageEvent event) {
        if (!enabled()) return;
        CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(s -> s.getItem() instanceof TouhouHatItem))
                .ifPresent(result -> {
                    ItemStack curioStack = result.stack();
                    TouhouHatItem hat = (TouhouHatItem) curioStack.getItem();
                    event.setSource(hat.modifyDamageType(curioStack, player, event.getBullet(), event.getSource()));
                });
    }

    public static void applyOnHurtFromCurios(Player player,
            dev.xkmc.l2damagetracker.contents.attack.DamageData.DefenceMax data) {
        if (!enabled()) return;
        CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(s -> s.getItem() instanceof TouhouHatItem))
                .ifPresent(result -> {
                    ItemStack curioStack = result.stack();
                    ((TouhouHatItem) curioStack.getItem()).onHurtTarget(curioStack, data.getSource(), data.getTarget());
                });
    }

    private record SimpleCurio(ItemStack stack) implements ICurio {
        @Override
        public ItemStack getStack() {
            return stack;
        }
    }

    private record HatCurio(ItemStack stack) implements ICurio {

        @Override
        public ItemStack getStack() {
            return stack;
        }

        @Override
        public void curioTick(SlotContext slotContext) {
            if (!enabled()) return;
            if (slotContext.entity() instanceof Player player && !slotContext.cosmetic()) {
                if (stack.getItem() instanceof TouhouHatItem hat) {
                    hat.tickFromCurio(stack, player.level(), player);
                }
            }
        }

        @Override
        public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id) {
            Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create();
            if (!enabled()) return map;
            if (stack.getItem() instanceof TouhouHatItem hat) {
                hat.addCurioModifiers(map, slotContext, id);
            }
            return map;
        }
    }
}
