package doublenegation.mods.compactores.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import doublenegation.mods.compactores.CompactOre;
import doublenegation.mods.compactores.CompactOreTexture;
import doublenegation.mods.compactores.CompactOres;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.gui.widget.Slider;
import net.minecraftforge.common.MinecraftForge;

import java.awt.image.BufferedImage;
import java.util.Optional;

public class TextureEditor {

    // command stuff

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(TextureEditor::onClientChat);
    }

    private static void onClientChat(ClientChatEvent event) {
        if (event.getMessage().replaceAll(" +", " ").equals("/compactores textureeditor")) {
            event.setCanceled(true);
            Minecraft.getInstance().gui.getChat().addRecentChat(event.getMessage());
            if(Minecraft.getInstance().hitResult != null && Minecraft.getInstance().hitResult.getType() == HitResult.Type.BLOCK
                    && Minecraft.getInstance().level != null) {
                Block block = Minecraft.getInstance().level.getBlockState(((BlockHitResult) Minecraft.getInstance().hitResult).getBlockPos()).getBlock();
                Optional<CompactOre> activeOre = CompactOres.compactOres().stream().filter(
                            ore -> ore.getBaseBlockRegistryName().equals(block.getRegistryName()) || ore.name().equals(block.getRegistryName()))
                        .findAny();
                if(activeOre.isPresent()) {
                    DelayedTickTaskExecutor.submitDelayed(() -> {
                        Minecraft.getInstance().setScreen(new EditorScreen(activeOre.get()));
                    }, Minecraft.getInstance(), 1);
                    return;
                }
            }
            LocalPlayer player = Minecraft.getInstance().player;
            if(player == null) return;
            player.sendMessage(new TranslatableComponent("commands.compactores.no_ore").withStyle(ChatFormatting.RED), player.getUUID());
        }
    }

    // actual ui

    private static class EditorScreen extends Screen { // NOTE: dimensions of screen = 320x240

        private final CompactOre ore;
        private int tickCounter = 0;
        private CompactOreTexture.TextureInfo rockTexture;
        private CompactOreTexture.TextureInfo oreTexture;
        private int generatorMode = 0; // if the value for maxOreLayerColorDiff < -1, then the constructor won't find anything, so use EXACT_MATCH in that case
        private int value;
        private Slider valueSlider;
        private CompactOreTexture.TextureInfo compactOreTexture;

        protected EditorScreen(CompactOre ore) {
            super(new TranslatableComponent("gui.compactores.textureeditor.title", ore.getBaseBlockRegistryName().toString()));
            this.ore = ore;
            value = ore.getMaxOreLayerColorDiff();
            for(int i = GeneratorMode.values().length - 1; i >= 0; i--) {
                if(value >= GeneratorMode.values()[i].baseValue) {
                    generatorMode = i;
                    break;
                }
            }
        }

        @Override
        protected void init() {
            super.init();
            addRenderableWidget(new Button(fromRight(0), fromTop(0), 20, 20, new TextComponent("X"), btn -> onClose()));
            rockTexture = CompactOreTexture.TextureInfo.generateForEditorRendering(ore.getBaseUnderlyingTexture(), -1);
            oreTexture = CompactOreTexture.TextureInfo.generateForEditorRendering(ore.getBaseOreTexture(), -1);
            compactOreTexture = CompactOreTexture.TextureInfo.generateForEditorRendering(ore.name(), value);
            GeneratorMode mode = GeneratorMode.values()[generatorMode];
            addRenderableWidget(new Button(fromLeft(20), fromTop(114), 280, 20,
                    new TranslatableComponent("gui.compactores.textureeditor.detector", new TranslatableComponent(mode.translationKey)), this::nextMode));
            valueSlider = addRenderableWidget(new Slider(fromLeft(20), fromTop(144), 280, 20,
                    new TranslatableComponent("gui.compactores.textureeditor.detector_param"),
                    new TextComponent(""), 0, mode.maxAdjust, value - mode.baseValue, false, true, btn -> {}));
            valueSlider.visible = mode.adjustable;
        }

        @Override
        public void tick() {
            super.tick();
            tickCounter++;
        }

        private void nextMode(Button button) {
            if(++generatorMode >= GeneratorMode.values().length) {
                generatorMode -= GeneratorMode.values().length;
            }
            GeneratorMode mode = GeneratorMode.values()[generatorMode];
            value = mode.adjustable ? mode.baseValue + mode.initialAdjust : mode.baseValue;
            compactOreTexture = CompactOreTexture.TextureInfo.generateForEditorRendering(ore.name(), value);
            button.setMessage(new TranslatableComponent("gui.compactores.textureeditor.detector", new TranslatableComponent(mode.translationKey)));
            valueSlider.dragging = false;
            valueSlider.maxValue = mode.maxAdjust;
            valueSlider.sliderValue = (double) mode.initialAdjust / mode.maxAdjust;
            valueSlider.visible = mode.adjustable;
            valueSlider.updateSlider();
        }

        @Override
        public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            // check for slider update
            GeneratorMode mode = GeneratorMode.values()[generatorMode];
            int newValue = mode.adjustable ? mode.baseValue + (int) Math.round(valueSlider.sliderValue * mode.maxAdjust) : mode.baseValue;
            if(value != newValue) {
                value = newValue;
                compactOreTexture = CompactOreTexture.TextureInfo.generateForEditorRendering(ore.name(), value);
            }
            // render
            renderDirtBackground(0);
            drawCenteredString(matrixStack, font, title, fromLeft(160), fromTop(10), 0xFFFFFF);
            renderTextureInfo(matrixStack, rockTexture, fromLeft(0), fromTop(30));
            drawString(matrixStack, font, new TranslatableComponent("gui.compactores.textureeditor.rock_texture"),
                    fromLeft(42), fromTop(36), 0xFFFFFF);
            drawString(matrixStack, font, ore.getBaseUnderlyingTexture().toString(), fromLeft(42), fromTop(46), 0xFFFFFF);
            renderTextureInfo(matrixStack, oreTexture, fromLeft(0), fromTop(72));
            drawString(matrixStack, font, new TranslatableComponent("gui.compactores.textureeditor.ore_texture"),
                    fromLeft(42), fromTop(78), 0xFFFFFF);
            drawString(matrixStack, font, ore.getBaseOreTexture().toString(), fromLeft(42), fromTop(88), 0xFFFFFF);
            renderTextureInfo(matrixStack, compactOreTexture, fromLeft(0), fromTop(174));
            drawString(matrixStack, font, new TranslatableComponent("gui.compactores.textureeditor.compact_ore_texture"),
                    fromLeft(42), fromTop(180), 0xFFFFFF);
            drawString(matrixStack, font, new TranslatableComponent("gui.compactores.textureeditor.update_config", "maxOreLayerColorDiff = " + value),
                    fromLeft(42), fromTop(190), 0xFFFFFF);
            super.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        private int fromTop(int dst) {
            return height / 2 - 120 + dst;
        }

        private int fromBottom(int dst) {
            return height / 2 + 120 - dst;
        }

        private int fromLeft(int dst) {
            return width / 2 - 160 + dst;
        }

        private int fromRight(int dst) {
            return width / 2 + 160 - dst;
        }

        private void renderTextureInfo(PoseStack matrixStack, CompactOreTexture.TextureInfo tex, int x, int y) {
            if(tex == null) return;
            renderBufferedImage(matrixStack, tex.getTextures().get(tickCounter % tex.getTextures().size()), x, y);
        }

        private void renderBufferedImage(PoseStack matrixStack, BufferedImage image, int x, int y) {
            for(int i = 0; i < image.getWidth(); i++) {
                for(int j = 0; j < image.getHeight(); j++) {
                    fill(matrixStack, x + i, y + j, x + i + 1, y + j + 1, image.getRGB(i, j));
                }
            }
        }

        private enum GeneratorMode {
            AUTO_RGB_CHANGE_DIFF("gui.compactores.textureeditor.detector.auto_rgb_change_diff", -2, false, 0, 0),
            EXACT_MATCH("gui.compactores.textureeditor.detector.exact_match", -1, false, 0, 0),
            RGB_DIFF_SUM("gui.compactores.textureeditor.detector.rgb_diff_sum", 0, true, 20, 765),
            RGB_CHANGE_DIFF("gui.compactores.textureeditor.detector.rgb_change_diff", 1000, true, 10, 180);
            private final String translationKey;
            private final int baseValue;
            private final boolean adjustable;
            private final int initialAdjust;
            private final int maxAdjust;
            GeneratorMode(String translationKey, int baseValue, boolean adjustable, int initialAdjust, int maxAdjust) {
                this.translationKey = translationKey;
                this.baseValue = baseValue;
                this.adjustable = adjustable;
                this.initialAdjust = initialAdjust;
                this.maxAdjust = maxAdjust;
            }
        }

    }

}
