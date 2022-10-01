package basemod;

import basemod.interfaces.ImGuiSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.patches.whatmod.WhatMod;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import imgui.ImGui;
import imgui.ImGuiTextFilter;
import imgui.ImVec2;
import imgui.flag.ImGuiDataType;
import imgui.flag.ImGuiSliderFlags;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *
 * Handles the creation of the ModBadge and settings panel for BaseMod
 *
 */
public class BaseModInit implements PostInitializeSubscriber, ImGuiSubscriber {
	public static final String MODNAME = "BaseMod";
	public static final String AUTHOR = "t-larson, test447, FlipskiZ, Haashi, Blank The Evil, kiooeht, robojumper, Skrelpoid";
	public static final String DESCRIPTION = "Modding API and Dev console.";

	private InputProcessor oldInputProcessor;

	public static final float BUTTON_X = 350.0f;
	public static final float BUTTON_Y = 650.0f;
	public static final float BUTTON_LABEL_X = 475.0f;
	public static final float BUTTON_LABEL_Y = 700.0f;
	public static final float BUTTON_ENABLE_X = 350.0f;
	public static final float BUTTON_ENABLE_Y = 600.0f;
	public static final float AUTOCOMPLETE_BUTTON_ENABLE_X = 350.0f;
	public static final float AUTOCOMPLETE_BUTTON_ENABLE_Y = 550.0f;
	public static final float AUTOCOMPLETE_LABEL_X = 350.0f;
	public static final float AUTOCOMPLETE_LABEL_Y = 425.0f;
	private static final String AUTOCOMPLETE_INFO = "Press L_Shift + Up/Down to scroll through suggestions.\nPress Tab or Right to complete the current command.\nPress Left to delete the last token.";
	public static final float WHATMOD_BUTTON_X = 350.0f;
	public static final float WHATMOD_BUTTON_Y = 350.0f;
	public static final float FIXES_BUTTON_X = 350.0f;
	public static final float FIXES_BUTTON_Y = 300.0f;

	@Override
	public void receivePostInitialize() {
		// BaseMod post initialize handling
		ModPanel settingsPanel = new ModPanel();

		ModLabel buttonLabel = new ModLabel("", BUTTON_LABEL_X, BUTTON_LABEL_Y, settingsPanel, (me) -> {
			if (me.parent.waitingOnEvent) {
				me.text = "Press key";
			} else {
				me.text = "Change console hotkey (" + Keys.toString(DevConsole.toggleKey) + ")";
			}
		});
		settingsPanel.addUIElement(buttonLabel);

		ModButton consoleKeyButton = new ModButton(BUTTON_X, BUTTON_Y, settingsPanel, (me) -> {
			me.parent.waitingOnEvent = true;
			oldInputProcessor = Gdx.input.getInputProcessor();
			Gdx.input.setInputProcessor(new InputAdapter() {
				@Override
				public boolean keyUp(int keycode) {
					DevConsole.toggleKey = keycode;
					BaseMod.setString("console-key", Keys.toString(keycode));
					me.parent.waitingOnEvent = false;
					Gdx.input.setInputProcessor(oldInputProcessor);
					return true;
				}
			});
		});
		settingsPanel.addUIElement(consoleKeyButton);

		ModLabeledToggleButton enableConsole = new ModLabeledToggleButton("Enable dev console",
				BUTTON_ENABLE_X, BUTTON_ENABLE_Y, Settings.CREAM_COLOR, FontHelper.charDescFont,
				DevConsole.enabled, settingsPanel, (label) -> {}, (button) -> {
					DevConsole.enabled = button.enabled;
					BaseMod.setBoolean("console-enabled", button.enabled);
				});
		settingsPanel.addUIElement(enableConsole);
		
		
		final ModLabel autoCompleteInfo = new ModLabel(AutoComplete.enabled ? AUTOCOMPLETE_INFO : "", AUTOCOMPLETE_LABEL_X, AUTOCOMPLETE_LABEL_Y, settingsPanel, (me) -> {} );
		settingsPanel.addUIElement(autoCompleteInfo);
		
		ModLabeledToggleButton enableAutoComplete = new ModLabeledToggleButton("Enable Autocompletion",
				AUTOCOMPLETE_BUTTON_ENABLE_X, AUTOCOMPLETE_BUTTON_ENABLE_Y, Settings.CREAM_COLOR, FontHelper.charDescFont,
				AutoComplete.enabled, settingsPanel, (label) -> {}, (button) -> {
					AutoComplete.enabled = button.enabled;
					AutoComplete.resetAndSuggest();
					BaseMod.setString("autocomplete-enabled", button.enabled ? "true" : "false");
					autoCompleteInfo.text = AutoComplete.enabled ? AUTOCOMPLETE_INFO : "";
				});
		settingsPanel.addUIElement(enableAutoComplete);

		ModLabeledToggleButton enableWhatMod = new ModLabeledToggleButton(
				"Enable mod name in tooltips",
				FontHelper.colorString("Must restart game to take effect.", "r"),
				WHATMOD_BUTTON_X, WHATMOD_BUTTON_Y, Settings.CREAM_COLOR, FontHelper.charDescFont,
				WhatMod.enabled, settingsPanel, (label) -> {},
				(button) -> {
					WhatMod.enabled = button.enabled;
					BaseMod.setBoolean("whatmod-enabled", button.enabled);
				}
		);
		settingsPanel.addUIElement(enableWhatMod);

		ModLabeledToggleButton enabledFixes = new ModLabeledToggleButton(
				"Enable base game fixes",
				"BaseMod makes some gameplay changes to facilitate modded gameplay. Disabling this option disables those changes so you can have a purer vanilla experience.",
				FIXES_BUTTON_X, FIXES_BUTTON_Y, Settings.CREAM_COLOR, FontHelper.charDescFont,
				BaseMod.fixesEnabled, settingsPanel, (label) -> {},
				(button) -> {
					BaseMod.fixesEnabled = button.enabled;
					BaseMod.setBoolean("basemod-fixes", button.enabled);
				}
		);
		settingsPanel.addUIElement(enabledFixes);

		Texture badgeTexture = ImageMaster.loadImage("img/BaseModBadge.png");
		BaseMod.registerModBadge(badgeTexture, MODNAME, AUTHOR, DESCRIPTION, settingsPanel);
		
		// Couldn't find a better place to put these. If they're not right here, please move them to a different classes' receivePostInitialize()
		BaseMod.initializeUnderscoreCardIDs();
		BaseMod.initializeUnderscorePotionIDs();
		BaseMod.initializeUnderscoreEventIDs();
		BaseMod.initializeUnderscoreRelicIDs();
		BaseMod.initializeEncounters();
	}

	private boolean firstTime = true;
	private final ImBoolean SHOW_DEMO_WINDOW = new ImBoolean(false);
	private final ImInt iData = new ImInt();

	@Override
	public void receiveImGui() {
		if (ImGui.begin("BaseMod")) {
			ImGui.checkbox("Show Demo Window", SHOW_DEMO_WINDOW);
			combatPanel();
		}
		ImGui.end();

		cardSearchWindow();

		actionQueueWindow();

		if (firstTime) {
			firstTime = false;
			ImGui.setWindowFocus("BaseMod");
		}

		if (SHOW_DEMO_WINDOW.get()) {
			ImGui.showDemoWindow(SHOW_DEMO_WINDOW);
		}
	}

	private void combatPanel() {
		ImGui.beginDisabled(AbstractDungeon.player == null);
		if (AbstractDungeon.player == null) ImGui.setNextItemOpen(false);
		if (ImGui.collapsingHeader("Player")) {
			playerInfo(AbstractDungeon.player);
		}
		ImGui.endDisabled();

		ImGui.beginDisabled(!inCombat());
		if (!inCombat()) ImGui.setNextItemOpen(false);
		if (ImGui.collapsingHeader("Monsters")) {
			Boolean openAction = null;
			if (ImGui.button("Open All")) {
				openAction = true;
			}
			ImGui.sameLine();
			if (ImGui.button("Close All")) {
				openAction = false;
			}

			int i = 0;
			for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
				monsterInfo(i, m, openAction);
				++i;
			}
		}
		ImGui.endDisabled();
	}

	private void playerInfo(AbstractPlayer p) {
		// act 4 keys
		if (ImGui.checkbox("emerald", Settings.hasEmeraldKey)) {
			Settings.hasEmeraldKey = !Settings.hasEmeraldKey;
		}
		ImGui.sameLine();
		if (ImGui.checkbox("ruby", Settings.hasRubyKey)) {
			Settings.hasRubyKey = !Settings.hasRubyKey;
		}
		ImGui.sameLine();
		if (ImGui.checkbox("sapphire", Settings.hasSapphireKey)) {
			Settings.hasSapphireKey = !Settings.hasSapphireKey;
		}
		// gold
		iData.set(p.gold);
		ImGui.inputInt("Gold", iData, 1, 100);
		if (iData.get() < 0) {
			iData.set(0);
		}
		if (iData.get() != p.gold) {
			p.gold = iData.get();
			p.displayGold = p.gold;
		}
		// max hp
		iData.set(p.maxHealth);
		ImGui.inputInt("Max HP", iData, 1, 10);
		if (iData.get() < 1) {
			iData.set(1);
		}
		if (iData.get() != p.maxHealth) {
			p.maxHealth = iData.get();
			p.currentHealth = Integer.min(p.currentHealth, p.maxHealth);
			p.healthBarUpdatedEvent();
			ReflectionHacks.setPrivate(p, AbstractCreature.class, "healthBarAnimTimer", 0.2f);
		}
		// hp
		creatureHP(p);
		// block
		creatureBlock(p);
		// powers
		creaturePowers(p);
		//deck
		ArrayList<AbstractCard> cards = AbstractDungeon.player.masterDeck.group;
		if (ImGui.treeNode(String.format("Deck (%d)###deck", cards.size()))) {
			if (ImGui.beginTable("deck cards", 4)) {
				ImGui.tableSetupColumn("index", ImGuiTableColumnFlags.WidthFixed);
				ImGui.tableSetupColumn("card name");
				ImGui.tableSetupColumn("upgrade", ImGuiTableColumnFlags.WidthFixed);
				ImGui.tableSetupColumn("discard", ImGuiTableColumnFlags.WidthFixed);

				for (int i=0; i<cards.size(); ++i) {
					AbstractCard card = cards.get(i);
					ImGui.tableNextRow();
					// index
					ImGui.tableSetColumnIndex(0);
					ImGui.text(Integer.toString(i+1));
					// name
					ImGui.tableSetColumnIndex(1);
					ImGui.text(card.name);
					// upgrade
					ImGui.tableSetColumnIndex(2);
					ImGui.beginDisabled(!card.canUpgrade());
					if (ImGui.button("Upgrade##upgrade" + i)) {
						card.upgrade();
					}
					ImGui.endDisabled();
					// remove
					ImGui.tableSetColumnIndex(3);
					if (ImGui.button("Remove##remove" + i)) {
						AbstractDungeon.player.masterDeck.removeCard(card);
					}
				}
				ImGui.endTable();
			}
			ImGui.treePop();
		}
		// hand
		cards = AbstractDungeon.player.hand.group;
		if (ImGui.treeNode(String.format("Hand (%d)###hand", cards.size()))) {
			if (ImGui.button("Draw Card")) {
				addToTop(new DrawCardAction(1));
			}
			ImGui.sameLine();
			if (ImGui.button("Discard All")) {
				addToTop(new DiscardAction(p, p, cards.size(), false));
			}
			if (ImGui.beginTable("hand cards", 4)) {
				ImGui.tableSetupColumn("index", ImGuiTableColumnFlags.WidthFixed);
				ImGui.tableSetupColumn("card name");
				ImGui.tableSetupColumn("upgrade", ImGuiTableColumnFlags.WidthFixed);
				ImGui.tableSetupColumn("discard", ImGuiTableColumnFlags.WidthFixed);

				for (int i=0; i<cards.size(); ++i) {
					AbstractCard card = cards.get(i);
					ImGui.tableNextRow();
					// index
					ImGui.tableSetColumnIndex(0);
					ImGui.text(Integer.toString(i+1));
					// name
					ImGui.tableSetColumnIndex(1);
					ImGui.text(card.name);
					// upgrade
					ImGui.tableSetColumnIndex(2);
					ImGui.beginDisabled(!card.canUpgrade());
					if (ImGui.button("Upgrade##upgrade" + i)) {
						card.upgrade();
						card.superFlash();
						card.applyPowers();
					}
					ImGui.endDisabled();
					// discard
					ImGui.tableSetColumnIndex(3);
					if (ImGui.button("Discard##discard" + i)) {
						addToTop(new DiscardSpecificCardAction(card));
					}
				}
				ImGui.endTable();
			}
			ImGui.treePop();
		}
	}

	private void monsterInfo(int i, AbstractMonster c, Boolean openAction) {
		if (c == null || c.isDeadOrEscaped()) return;
		if (openAction != null) {
			ImGui.setNextItemOpen(openAction);
		}
		if (ImGui.treeNode(i, c.name)) {
			creatureHP(c);
			creatureBlock(c);
			creaturePowers(c);
			ImGui.treePop();
		}
	}

	private void creatureHP(AbstractCreature c) {
		iData.set(c.currentHealth);
		ImGui.sliderInt("HP", iData.getData(), 1, c.maxHealth);
		if (iData.get() != c.currentHealth) {
			c.currentHealth = iData.get();
			c.healthBarUpdatedEvent();
			ReflectionHacks.setPrivate(c, AbstractCreature.class, "healthBarAnimTimer", 0.2f);
		}
		if (c instanceof AbstractMonster) {
			ImGui.sameLine();
			if (ImGui.button("Kill")) {
				addToTop(new InstantKillAction(c));
			}
		}
	}

	private void creatureBlock(AbstractCreature c) {
		iData.set(c.currentBlock);
		ImGui.dragScalar("Block", ImGuiDataType.S32, iData, 0.25f, 0, 999, "%d", ImGuiSliderFlags.AlwaysClamp);
		if (iData.get() != c.currentBlock) {
			if (c.currentBlock <= 0) {
				ReflectionHacks.privateMethod(AbstractCreature.class, "gainBlockAnimation").invoke(c);
			}
			c.currentBlock = iData.get();
		}
	}

	private void creaturePowers(AbstractCreature c) {
		if (!c.powers.isEmpty() && ImGui.treeNode("Powers")) {
			if (ImGui.beginTable("powers", 3)) {
				ImGui.tableSetupColumn("amount", ImGuiTableColumnFlags.WidthFixed);
				ImGui.tableSetupColumn("power name");
				ImGui.tableSetupColumn("remove", ImGuiTableColumnFlags.WidthFixed);

				int i = 0;
				for (AbstractPower p : c.powers) {
					ImGui.tableNextRow();
					// amount
					ImGui.tableSetColumnIndex(0);
					ImGui.text(Integer.toString(p.amount));
					// name
					ImGui.tableSetColumnIndex(1);
					ImGui.text(p.name);
					// remove
					ImGui.tableSetColumnIndex(2);
					if (ImGui.button("Remove##remove" + i)) {
						addToTop(new RemoveSpecificPowerAction(c, c, p));
					}
					++i;
				}
				ImGui.endTable();
			}
			ImGui.treePop();
		}
	}

	private void actionQueueWindow() {
		if (ImGui.begin("Action Queue")) {
			if (AbstractDungeon.actionManager.actions.isEmpty()) {
				ImGui.text("Empty");
			}

			if (ImGui.beginTable("action queue", 3)) {
				if (AbstractDungeon.actionManager.currentAction != null) {
					if (actionRow(-1, AbstractDungeon.actionManager.currentAction)) {
						AbstractDungeon.actionManager.currentAction.isDone = true;
					}
				}
				List<AbstractGameAction> remove = new ArrayList<>();
				int i = 0;
				for (AbstractGameAction action : AbstractDungeon.actionManager.actions) {
					if (actionRow(i, action)) {
						remove.add(action);
					}
					++i;
				}
				AbstractDungeon.actionManager.actions.removeAll(remove);
				ImGui.endTable();
			}
		}
		ImGui.end();
	}

	private boolean actionRow(int i, AbstractGameAction action) {
		ImGui.tableNextRow();
		// name
		ImGui.tableSetColumnIndex(0);
		ImGui.text(action.getClass().getSimpleName());
		// duration
		ImGui.tableSetColumnIndex(1);
		float duration = ReflectionHacks.getPrivate(action, AbstractGameAction.class, "duration");
		ImGui.text(Float.toString(duration));
		// stop
		ImGui.tableSetColumnIndex(2);
		return ImGui.button("Remove##remove" + i);
	}

	private String cardModIDFilter = "##ALL";
	private final ImGuiTextFilter cardFilter = new ImGuiTextFilter();
	private String selectedCardId = null;
	private final ImInt cardCount = new ImInt(1);
	private final ImInt cardUpgrades = new ImInt(0);

	private void cardSearchWindow() {
		if (ImGui.begin("Card Search")) {
			ArrayList<AbstractCard> allCards = CardLibrary.getAllCards();
			List<String> modIDs = allCards.stream()
					.map(c -> getModID(c.cardID))
					.distinct()
					.collect(Collectors.toList());
			modIDs.add(0, "##ALL");
			// modid
			ImGui.pushItemWidth(90);
			if (ImGui.beginCombo("##modid", cardModIDFilter)) {
				for (String modID : modIDs) {
					boolean isSelected = cardModIDFilter.equals(modID);
					if (ImGui.selectable(modID, isSelected)) {
						cardModIDFilter = modID;
					}

					if (isSelected) {
						ImGui.setItemDefaultFocus();
					}
				}
				ImGui.endCombo();
			}
			ImGui.popItemWidth();
			ImGui.sameLine();
			// filter
			cardFilter.draw("##");
			// card search
			if (ImGui.beginListBox("##all cards", -Float.MIN_VALUE, 7 * ImGui.getTextLineHeightWithSpacing())) {
				ImVec2 textSize = new ImVec2();
				for (AbstractCard card : allCards) {
					if ((cardFilter.passFilter(card.name) || cardFilter.passFilter(card.cardID)) && ("##ALL".equals(cardModIDFilter) || getModID(card.cardID).equals(cardModIDFilter))) {
						boolean isSelected = selectedCardId != null && selectedCardId.equals(card.cardID);
						if (ImGui.selectable(String.format("%s###%s", card.name, card.cardID), isSelected)) {
							selectedCardId = card.cardID;
						}
						if (!Objects.equals(card.name, card.cardID)) {
							String text = String.format("id: %s", card.cardID);
							ImGui.calcTextSize(textSize, text);
							ImGui.sameLine(ImGui.getWindowContentRegionMaxX() - textSize.x);
							ImGui.text(text);
						}

						if (isSelected) {
							ImGui.setItemDefaultFocus();
						}
					}
				}
				ImGui.endListBox();
			}
			// card count
			ImGui.pushItemWidth(90);
			ImGui.inputInt("Count", cardCount);
			if (cardCount.get() < 1) {
				cardCount.set(1);
			}
			ImGui.sameLine();
			// upgrades
			ImGui.inputInt("Upgrades", cardUpgrades);
			ImGui.popItemWidth();
			ImGui.beginDisabled(selectedCardId == null || !inCombat());
			// add buttons
			if (ImGui.button("Add to hand")) {
				addToTop(new MakeTempCardInHandAction(getCardAndUpgrade(selectedCardId, cardUpgrades.get()), cardCount.get(), true));
			}
			ImGui.sameLine();
			ImGui.endDisabled();
			ImGui.beginDisabled(selectedCardId == null || AbstractDungeon.player == null);
			if (ImGui.button("Add to deck")) {
				UnlockTracker.markCardAsSeen(selectedCardId);
				for (int i=0; i<cardCount.get(); ++i){
					AbstractDungeon.effectList.add(
							new ShowCardAndObtainEffect(
									getCardAndUpgrade(selectedCardId, cardUpgrades.get()),
									Settings.WIDTH / 2.0f,
									Settings.HEIGHT / 2.0f
							)
					);
				}
			}
			ImGui.endDisabled();
		}
		ImGui.end();
	}

	private String getModID(String cardID) {
		int idx = cardID.indexOf(':');
		if (idx < 0) return "Base game";
		return cardID.substring(0, idx);
	}

	private AbstractCard getCardAndUpgrade(String cardId, int upgrades) {
		AbstractCard ret = CardLibrary.getCard(cardId);
		if (ret != null) {
			ret = ret.makeCopy();
			for (int i=0; i<upgrades; ++i) {
				ret.upgrade();
			}
		}
		return ret;
	}

	private void addToTop(AbstractGameAction action) {
		AbstractDungeon.actionManager.addToTop(action);
	}

	private boolean inCombat() {
		return AbstractDungeon.currMapNode != null && AbstractDungeon.getCurrRoom() != null && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT;
	}
}