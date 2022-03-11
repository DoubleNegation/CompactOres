package doublenegation.mods.compactores;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class SimpleChoiceMessageScreen extends Utils.ReturningScreen {

    private final Component message;
    private final Component buttonTextFirst;
    private final Component buttonTextSecond;
    private final Button.OnPress onFirstOption;
    private final Button.OnPress onSecondOption;

    private MultiLineLabel textRenderer = MultiLineLabel.EMPTY;

    public SimpleChoiceMessageScreen(String i18nBase, Button.OnPress onFirstOption, Button.OnPress onSecondOption, Object... messageParams) {
        super(new TranslatableComponent(i18nBase + ".title"));
        message = new TranslatableComponent(i18nBase + ".message", messageParams);
        buttonTextFirst = new TranslatableComponent(i18nBase + ".option1");
        buttonTextSecond = new TranslatableComponent(i18nBase + ".option2");
        this.onFirstOption = onFirstOption;
        this.onSecondOption = onSecondOption;
    }

    @Override
    protected void init() {
        super.init();
        textRenderer = MultiLineLabel.create(this.font, message, this.width - 50);
        int i = 50 + (textRenderer.getLineCount() + 1) * 9;
        this.addWidget(new Button(this.width / 2 - 150, i + 5, 300, 20, buttonTextFirst, onFirstOption));
        this.addWidget(new Button(this.width / 2 - 150, i + 30, 300, 20, buttonTextSecond, onSecondOption));
    }

    @Override
    public void render(PoseStack p_230430_1_, int p_render_1_, int p_render_2_, float p_render_3_) {
        this.renderBackground(p_230430_1_);
        drawCenteredString(p_230430_1_, this.font, this.title, this.width / 2, 30, 0xFFFFFF);
        textRenderer.renderCentered(p_230430_1_, this.width / 2, 50);
        super.render(p_230430_1_, p_render_1_, p_render_2_, p_render_3_);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

}
