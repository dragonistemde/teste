package com.powertrade;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Ferraria: bônus (e requisitos) para espadas, machados e armaduras fabricados pelo jogador. */
public final class Forging {
    private Forging() {}

    public static void forge(ServerPlayer player, ItemStack stack) {
        boolean weapon = stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES);
        EquipmentSlotGroup slot = null;
        String slotName = null;
        if (stack.is(ItemTags.HEAD_ARMOR)) {
            slot = EquipmentSlotGroup.HEAD;
            slotName = "head";
        } else if (stack.is(ItemTags.CHEST_ARMOR)) {
            slot = EquipmentSlotGroup.CHEST;
            slotName = "chest";
        } else if (stack.is(ItemTags.LEG_ARMOR)) {
            slot = EquipmentSlotGroup.LEGS;
            slotName = "legs";
        } else if (stack.is(ItemTags.FOOT_ARMOR)) {
            slot = EquipmentSlotGroup.FEET;
            slotName = "feet";
        }
        if (!weapon && slot == null) return;

        MinecraftServer server = PowerLogic.server(player);
        if (server == null) return;
        PowerData data = PowerData.get(server);
        int ferraria = data.getLevel(player.getUUID(), Skill.FERRARIA);

        // Requisito de ferraria por material.
        int need = requiredSmithing(stack);
        if (ferraria < need) {
            int max = stack.getMaxDamage();
            if (max > 0) {
                stack.setDamageValue((int) (max * 0.6));
            }
            player.sendSystemMessage(Component.literal("Sua Ferraria (nv " + ferraria + ") é baixa para este material (precisa nv "
                    + need + "): o item saiu defeituoso.").withStyle(ChatFormatting.RED));
            return;
        }

        ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        boolean changed = false;

        if (weapon) {
            int lvl = data.getLevel(player.getUUID(), Skill.FORJA_ARMAS);
            if (lvl > 0) {
                mods = mods.withModifierAdded(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(id("forge_weapon"), Skill.FORJA_ARMAS.value(lvl),
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND);
                changed = true;
            }
        } else {
            int lvl = data.getLevel(player.getUUID(), Skill.FORJA_ARMADURAS);
            if (lvl > 0) {
                double armor = Skill.FORJA_ARMADURAS.value(lvl);
                mods = mods.withModifierAdded(Attributes.ARMOR,
                        new AttributeModifier(id("forge_armor_" + slotName), armor,
                                AttributeModifier.Operation.ADD_VALUE),
                        slot);
                mods = mods.withModifierAdded(Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(id("forge_tough_" + slotName), armor * 0.25,
                                AttributeModifier.Operation.ADD_VALUE),
                        slot);
                changed = true;
            }
        }

        if (changed) {
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, mods);
        }

        int max = stack.getMaxDamage();
        if (max > 0 && ferraria > 0) {
            stack.set(DataComponents.MAX_DAMAGE, (int) (max * (1.0 + Skill.FERRARIA.value(ferraria))));
            changed = true;
        }

        if (changed) {
            MutableComponent name = Component.literal("Forjado ").withStyle(ChatFormatting.GOLD);
            name.append(stack.getHoverName().copy().withStyle(ChatFormatting.GOLD));
            name.withStyle(s -> s.withItalic(false));
            stack.set(DataComponents.CUSTOM_NAME, name);
        }
    }

    private static int requiredSmithing(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.startsWith("netherite")) return 15;
        if (path.startsWith("diamond")) return 8;
        if (path.startsWith("iron") || path.startsWith("chainmail") || path.startsWith("golden")) return 3;
        return 0;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(PowerTradeMod.MOD_ID, path);
    }
}
