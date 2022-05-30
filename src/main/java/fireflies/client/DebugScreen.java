package fireflies.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import fireflies.Fireflies;
import fireflies.client.particle.FireflyDustParticle;
import fireflies.client.particle.FireflyParticleManager;
import fireflies.entity.FireflyEntity;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT, modid = Fireflies.MODID)
public class DebugScreen extends Screen { // https://github.com/codingminecraft/IntegratingImGui , https://github.com/mjwells2002/imgui-mc , https://github.com/gurachan/fabric-kotlin-graphics , https://github.com/SpaiR/imgui-java/issues/20 , https://github.com/jihuayu/forge-imgui , https://github.com/AlexApps99/MinecraftImgui
    private static final ImGuiImplGlfw IMGUI_GLFW = new ImGuiImplGlfw();
    private static final ImGuiImplGl3 IMGUI_GL3 = new ImGuiImplGl3();
    private static final HashSet<Integer> KEY_BUFFER = new HashSet<>();

    @Nullable
    public static FireflyEntity selectedFirefly;
    private static DebugScreen instance;
    private static ImGuiIO io;

    private DebugScreen() {
        super(StringTextComponent.EMPTY);
        ImGui.createContext();

        io = ImGui.getIO();
        io.setIniFilename(null);
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        IMGUI_GLFW.init(Minecraft.getInstance().getMainWindow().getHandle(), false);
        IMGUI_GL3.init();
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (event.getKey() == GLFW.GLFW_KEY_RIGHT_CONTROL && event.getAction() == GLFW.GLFW_PRESS) {
            final Minecraft mc = Minecraft.getInstance();
            if (mc.world != null) {
                mc.displayGuiScreen(getInstance());
            }
        }
    }

    @Override
    public void render(MatrixStack pMatrixStack, int pMouseX, int pMouseY, float pPartialTicks) {
        if (this.minecraft == null || this.minecraft.world == null) return;
        IMGUI_GLFW.newFrame();
        ImGui.newFrame();
        ImGui.styleColorsClassic();
        ImGui.showDemoWindow();
        ImGui.setNextWindowSize(666, 420, ImGuiCond.FirstUseEver);
        ImGui.begin("Fireflies debug screen");

        ImGui.beginChild("Fireflies selection", 200, 0, true, ImGuiWindowFlags.HorizontalScrollbar);
        for (Entity entity : this.minecraft.world.getAllEntities()) {
            if (!(entity instanceof FireflyEntity)) continue;
            final FireflyEntity firefly = (FireflyEntity) entity;

            final boolean flag = firefly.equals(selectedFirefly);
            if (ImGui.selectable(firefly.getName().getString() + " - " + firefly.getPosition().getCoordinatesAsString(), flag)) {
                selectedFirefly = flag ? null : firefly;
            }
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip(firefly.toString());
            }
        }
        ImGui.endChild();

        ImGui.sameLine();

        if (selectedFirefly != null && selectedFirefly.isAddedToWorld()) {
            ImGui.beginGroup();

            textValue("hasIllumerin", selectedFirefly.hasIllumerin());

            ImGui.separator();

            textValue("abdomenParticle", selectedFirefly.particleManager.abdomenParticle);
            textValue("abdomenParticlePositionOffset", selectedFirefly.particleManager.abdomenParticlePositionOffset);
            textValue("getAbdomenParticlePos", Arrays.toString(selectedFirefly.particleManager.getAbdomenParticlePos()));
            textValue("canSpawnDustParticles", selectedFirefly.particleManager.canSpawnDustParticles());
            if (ImGui.treeNode("Dust particles properties")) {
                ImGui.pushItemWidth(300);
                ImGui.sliderFloat("DUST_SPAWN_CHANCE", FireflyParticleManager.DUST_SPAWN_CHANCE, 0, 1f);
                ImGui.sliderFloat("DUST_FALL_SPEED", FireflyParticleManager.DUST_FALL_SPEED, 0, .1f);
                ImGui.sliderFloat2("SCALE (min/max)", FireflyDustParticle.SCALE, 0, 0.1f);
                ImGui.sliderInt2("AGE (min/max) (ticks)", FireflyDustParticle.AGE, 0, 200);
                ImGui.sliderFloat2("ROT_SPEED (min/max)", FireflyDustParticle.ROT_SPEED, 0, 1f);
                ImGui.popItemWidth();

                ImGui.treePop();
            }
            if (ImGui.button("spawnDustParticle()")) {
                selectedFirefly.particleManager.spawnDustParticle();
            }
            if (ImGui.button("spawnAbdomenParticle()")) {
                selectedFirefly.particleManager.spawnAbdomenParticle();
            }
            if (ImGui.button("destroyAbdomenParticle()")) {
                selectedFirefly.particleManager.destroyAbdomenParticle();
            }

            ImGui.separator();

            textValue("curAnimation", selectedFirefly.abdomenAnimationManager.curAnimation);
            textValue("prevAnimation", selectedFirefly.abdomenAnimationManager.prevAnimation);

            if (ImGui.treeNode("setAnimation()")) {
                for (AbdomenAnimation animation : AbdomenAnimationManager.ANIMATIONS.values()) {
                    final boolean flag = Objects.equals(animation.name, selectedFirefly.abdomenAnimationManager.curAnimation);
                    if (ImGui.selectable(animation.name, flag)) {
                        selectedFirefly.abdomenAnimationManager.setAnimation(flag ? null : animation.name);
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip(animation.toString());
                    }
                }
                ImGui.treePop();
            }

            textValue("animationProperties", selectedFirefly.abdomenAnimationManager.abdomenAnimationProperties);
            if (ImGui.button("resetAnimationProperties()")) {
                selectedFirefly.abdomenAnimationManager.resetAbdomenAnimationProperties();
            }

            ImGui.endGroup();
        }

        ImGui.end();
        ImGui.render();
        IMGUI_GL3.renderDrawData(ImGui.getDrawData());
    }

    private static void textValue(String s, @Nullable Object o) {
        ImGui.text(s + ":");
        ImGui.sameLine(250);
        ImGui.text(o == null ? "null" : o.toString());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (io.getWantTextInput()) {
            io.addInputCharacter(chr);
        }
        super.charTyped(chr, keyCode);
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double amount) {
        if (io.getWantCaptureMouse()) {
            io.setMouseWheel((float) amount);
        }
        super.mouseScrolled(x, y, amount);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (io.getWantCaptureKeyboard() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            io.setKeysDown(keyCode, true);
            KEY_BUFFER.add(keyCode);
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            this.closeScreen();
        }

        super.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        io.setKeysDown(keyCode, false);
        KEY_BUFFER.remove(keyCode);

        super.keyReleased(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public void onClose() {
        // When Minecraft closes the screen, clear the key buffer.
        for (int keyCode : KEY_BUFFER) {
            io.setKeysDown(keyCode, false);
        }
        KEY_BUFFER.clear();
        super.onClose();
    }

    public static DebugScreen getInstance() {
        if (instance == null) instance = new DebugScreen();
        return instance;
    }
}
