package basemod.patches.imgui;

import basemod.BaseMod;
import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.helpers.input.InputAction;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiMouseCursor;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import javassist.CtBehavior;
import org.lwjgl.glfw.GLFWErrorCallback;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwInit;

public class ImGuiPatches
{
	private static ImGuiImplGlfw imGuiGlfw;
	private static ImGuiImplGl3 imGuiGl3;

	@SpirePatch2(
			clz = CardCrawlGame.class,
			method = "create"
	)
	public static class Create
	{
		public static void Prefix()
		{
			GLFWErrorCallback.createPrint(System.err).set();
			if (!glfwInit()) {
				throw new IllegalStateException("Unable to init GLFW");
			}
			ImGui.createContext();
			ImGuiIO io = ImGui.getIO();
			io.setIniFilename("imgui.ini");
			ImGui.getIO().setConfigWindowsMoveFromTitleBarOnly(true);
			ImGui.getIO().addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
			ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);

			long windowHandle = ReflectionHacks.privateMethod(Lwjgl3Window.class, "getWindowHandle")
					.invoke(((Lwjgl3Graphics) Gdx.graphics).getWindow());

			imGuiGlfw = new ImGuiImplGlfw();
			imGuiGl3 = new ImGuiImplGl3();
			imGuiGlfw.init(windowHandle, true);
			imGuiGl3.init();
		}
	}

	@SpirePatch2(
			clz = CardCrawlGame.class,
			method = "render"
	)
	public static class Render
	{
		private static boolean enabled = false;

		public static void Prefix()
		{
			SuppressHotkey.suppressedKeys.clear();

			if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
				SuppressHotkey.suppressedKeys.add(Input.Keys.E);
				enabled = !enabled;
				GameCursor.hidden = enabled;
			}
		}

		public static void Postfix()
		{
			imGuiGlfw.newFrame();
			ImGui.newFrame();

			if (enabled) {
				GameCursor.hidden = true;

				BaseMod.publishImGui();
			} else {
				ImGui.setMouseCursor(ImGuiMouseCursor.None);
			}

			ImGui.render();
			imGuiGl3.renderDrawData(ImGui.getDrawData());

			if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
				ImGui.updatePlatformWindows();
				ImGui.renderPlatformWindowsDefault();
			}
		}
	}

	@SpirePatch2(
			clz = CardCrawlGame.class,
			method = "dispose"
	)
	public static class Dispose
	{
		public static void Postfix()
		{
			imGuiGl3.dispose();
			imGuiGlfw.dispose();
			ImGui.destroyContext();
		}
	}

	@SpirePatch2(
			clz = InputHelper.class,
			method = "updateFirst"
	)
	public static class StopInput
	{
		private static boolean wasImGuiCaptured = false;

		@SpireInsertPatch(
				locator = Locator.class
		)
		public static SpireReturn<Void> Insert()
		{
			ImGuiIO io = ImGui.getIO();
			if (io.getWantCaptureMouse()) {
				wasImGuiCaptured = true;
				InputHelper.touchDown = false;
				InputHelper.touchUp = false;
				InputHelper.justClickedLeft = false;
				InputHelper.justClickedRight = false;
				InputHelper.justReleasedClickLeft = false;
				InputHelper.justReleasedClickRight = false;
				return SpireReturn.Return();
			}
			if (wasImGuiCaptured) {
				wasImGuiCaptured = false;
			}
			return SpireReturn.Continue();
		}

		private static class Locator extends SpireInsertLocator
		{
			@Override
			public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
				Matcher finalMatcher = new Matcher.MethodCallMatcher(Input.class, "isButtonPressed");
				return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
			}
		}
	}

	@SpirePatch2(
			clz = InputAction.class,
			method = "isJustPressed"
	)
	public static class SuppressHotkey
	{
		static List<Integer> suppressedKeys = new ArrayList<>();

		public static SpireReturn<Boolean> Prefix(int ___keycode)
		{
			if (ImGui.getIO().getWantCaptureKeyboard() || suppressedKeys.contains(___keycode)) {
				return SpireReturn.Return(false);
			}
			return SpireReturn.Continue();
		}
	}
}
