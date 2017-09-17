package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.scene.actions.Actions.*;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.entities.Weapon;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.resource.*;
import io.anuke.mindustry.ui.*;
import io.anuke.ucore.core.*;
import io.anuke.ucore.function.VisibilityProvider;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Textures;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.*;
import io.anuke.ucore.util.Mathf;

public class UI extends SceneModule{
	Table itemtable, weapontable, tools, loadingtable, desctable;
	SettingsDialog prefs;
	KeybindDialog keys;
	Dialog about, menu, restart, tutorial, levels, upgrades, load;
	Tooltip tooltip;

	VisibilityProvider play = () -> !GameState.is(State.menu);
	VisibilityProvider nplay = () -> GameState.is(State.menu);

	public UI() {
		Dialog.setShowAction(()-> sequence(Actions.moveToAligned(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight(), Align.center),
						parallel(Actions.moveToAligned(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2, Align.center, 0.09f, Interpolation.fade), 
								
								Actions.fadeIn(0.09f, Interpolation.fade))));
		
		Dialog.setHideAction(()-> sequence(
					parallel(Actions.moveBy(0, -Gdx.graphics.getHeight()/2, 0.08f, Interpolation.fade), 
							Actions.fadeOut(0.08f, Interpolation.fade))));
		
		skin.font().setUseIntegerPositions(false);
		skin.font().getData().setScale(Vars.fontscale);
		TooltipManager.getInstance().animations = false;
		
		Dialog.closePadR = -1;
		Dialog.closePadT = 4;
		
		Textures.load("sprites/");
		Textures.repeatWrap("conveyort", Gdx.app.getType() == ApplicationType.WebGL ? "back-web" : "back");
		
		Colors.put("description", Color.WHITE);
		Colors.put("turretinfo", Color.ORANGE);
		Colors.put("missingitems", Color.SCARLET);
	}
	
	void drawBackground(){
		
		Batch batch = scene.getBatch();
		Draw.color();
		int w = (int)screen.x;
		int h = (int)screen.y;
		
		Draw.color(Hue.lightness(0.6f));
		
		int tw = w/64+1;
		
		float scale = Unit.dp.inPixels(1f);
		
		Texture texture = Textures.get(Gdx.app.getType() == ApplicationType.WebGL ? "back-web" : "back");
		
		batch.draw(texture, 
				0, 0, w, h, 0, 0, (float)w/h/scale * h/texture.getHeight()/4f, -1f/scale * h/texture.getHeight()/4f);
		
		for(int x = 0; x < tw; x ++){
			float offset = (Timers.time()*2*(x%2-0.5f))/32f;
			batch.draw(Textures.get("conveyort"), x*64*scale, 0, 32*scale, h*scale, 0, offset, 1, h/32 + offset);
		}
		
		Draw.color();
		
		Draw.tscl(Unit.dp.inPixels(1.5f));
		
		Draw.text("[#111111]-( Mindustry )-", w/2, h-Unit.dp.inPixels(16));
		Draw.text("[#f1de60]-( Mindustry )-", w/2, h-Unit.dp.inPixels(10));
		
		Draw.tscl(Unit.dp.inPixels(0.5f));
	}

	@Override
	public void update(){

		if(nplay.visible()){
			scene.getBatch().getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			scene.getBatch().begin();
			
			drawBackground();
			
			scene.getBatch().end();
		}
		
		super.update();
	}

	@Override
	public void init(){
		
		load = new LoadDialog();
		
		upgrades = new UpgradeDialog();
		
		levels = new LevelDialog();
		
		prefs = new SettingsDialog();
		
		menu = new MenuDialog();
		
		prefs.sliderPref("difficulty", "Difficulty", 1, 0, 2, i -> {
			return i == 0 ? "Easy" : i == 1 ? "Normal" : "Hard";
		});
		
		prefs.screenshakePref();
		prefs.volumePrefs();
		
		prefs.checkPref("tutorial", "Show tutorial Window", true);
		prefs.checkPref("fps", "Show FPS", false);
		prefs.checkPref("noshadows", "Disable shadows", false);

		keys = new KeybindDialog();

		about = new TextDialog("About", aboutText);
		
		for(Cell<?> cell : about.content().getCells())
			cell.left();
		
		tutorial = new TutorialDialog();
		
		restart = new Dialog("The core was destroyed.", "dialog");
		
		restart.shown(()->{
			restart.content().clearChildren();
			if(control.isHighScore()){
				restart.content().add("[YELLOW]New highscore!").pad(6);
				restart.content().row();
			}
			restart.content().add("You lasted until wave [GREEN]" + control.getWave() + "[].").pad(6);
			restart.pack();
		});
		
		restart.getButtonTable().addButton("Back to menu", ()->{
			restart.hide();
			GameState.set(State.menu);
			control.reset();
		});
		
		weapontable = fill();
		weapontable.bottom();
		weapontable.setVisible(play);
		
		if(android){
			weapontable.remove();
		}
		
		build.begin(scene);

		new table(){{
			abottom();
			aright();
			
			new table("button"){{
				visible(()->player.recipe != null);
				desctable = get();
				fillX();
			}}.end().uniformX();
			
			row();

			new table("button"){{
				
				int rows = 3;
				int maxcol = 0;
				float size = 46;
				
				Stack stack = new Stack();
				ButtonGroup<ImageButton> group = new ButtonGroup<>();
				Array<Recipe> recipes = new Array<Recipe>();
				
				for(Section sec : Section.values()){
					recipes.clear();
					Recipe.getBy(sec, recipes);
					maxcol = Math.max((int)((float)recipes.size/rows+1), maxcol);
				}
				
				for(Section sec : Section.values()){
					recipes.clear();
					Recipe.getBy(sec, recipes);
					
					Table table = new Table();
					
					ImageButton button = new ImageButton("icon-"+sec.name(), "toggle");
					button.clicked(()->{
						if(!table.isVisible() && player.recipe != null){
							player.recipe = null;
						}
					});
					add(button).fill().height(54).padTop(-10).units(Unit.dp);
					button.getImageCell().size(40).padBottom(4).units(Unit.dp);
					group.add(button);
					
					table.pad(4);
					
					int i = 0;
					
					for(Recipe r : recipes){
						ImageButton image = new ImageButton(Draw.region(r.result.name()), "select");
						
						image.clicked(()->{
							if(Inventory.hasItems(r.requirements)){
								player.recipe = r;
								updateRecipe();
							}
						});
						
						table.add(image).size(size+8).pad(4).units(Unit.dp);
						image.getImageCell().size(size).units(Unit.dp);
						
						image.update(()->{
							
							boolean has = Inventory.hasItems(r.requirements);
							image.setDisabled(!has);
							image.setChecked(player.recipe == r && has);
							//image.setTouchable(has ? Touchable.enabled : Touchable.disabled);
							image.getImage().setColor(has ? Color.WHITE : Color.GRAY);
						});
						
						if(i % rows == rows-1)
							table.row();
						
						i++;
					}
					
					//additional padding
					for(int j = 0; j < maxcol - (int)((float)recipes.size/rows+1); j ++){
						table.row();
						table.add().size(size);
					}
					
					table.setVisible(()-> button.isChecked());
					
					stack.add(table);
				}
				
				
				row();
				add(stack).colspan(3);
				get().pad(10f);
				
				get().padLeft(0f);
				get().padRight(0f);
				
				end();
			}}.right().bottom().uniformX();
			
			row();
			
			if(!android){
				new button("Upgrades", ()->{
					upgrades.show();
				}).uniformX().fillX();
			}
			get().setVisible(play);

		}}.end();

		new table(){{
			atop();
			aleft();
			
			defaults().size(66).units(Unit.dp);
			
			//TODO menu buttons!
			new imagebutton("icon-menu", 40, ()->{
				showMenu();
			});
			
			new imagebutton("icon-settings", 40, ()->{
				prefs.show();
			});

			new imagebutton("icon-pause", 40, ()->{
				//TODO pause
			});
			
			row();
			
			itemtable = new table("button").end().top().left().colspan(3).fillX().size(-1).get();

			get().setVisible(play);
			
			Label fps = new Label(()->(Settings.getBool("fps") ? (Gdx.graphics.getFramesPerSecond() + " FPS") : ""));
			row();
			add(fps).colspan(3).size(-1);
			
		}}.end();

		//wave table...
		new table(){{
			atop();
			aright();

			new table(){{
				get().background("button");

				new label(()->"[orange]Wave " + control.getWave()).scale(fontscale*2f).left();

				row();

				new label(()-> control.getEnemiesRemaining() > 0 ?
						control.getEnemiesRemaining() + " Enemies remaining" : "New wave in " + (int) (control.getWaveCountdown() / 60f))
				.minWidth(150);

				get().pad(Unit.dp.inPixels(12));
			}};

			get().setVisible(play);
		}}.end();
		
		
		//+- table
		//TODO refactor to make this less messy?
		new table(){{
			aleft();
			abottom();
			int base = baseCameraScale;
			int min = base-zoomScale*2;
			int max = base+zoomScale;
			new button("+", ()->{
				if(Core.cameraScale < max){
					control.setCameraScale(Core.cameraScale+zoomScale);
				}
			}).size(Unit.dp.inPixels(40));
			
			new button("-", ()->{
				if(Core.cameraScale > min){
					control.setCameraScale(Core.cameraScale-zoomScale);
				}
			}).size(Unit.dp.inPixels(40));
			
			get().setVisible(play);
		}}.end();
	
		//menu table
		new table(){{
			
			new table("button"){{
				defaults().size(220, 50);
				
				new button("Play", () -> {
					levels.show();
				});
				
				if(Gdx.app.getType() != ApplicationType.WebGL){
					row();
				
					new button("Load Game", () -> {
						load.show();
					});
				}

				row();

				new button("Settings", () -> {
					prefs.show(scene);
				});

				row();
				
				if(!android){
					new button("Controls", () -> {
						keys.show(scene);
					});
					
					row();
				}

				new button("About", () -> {
					about.show(scene);
				});
				
				row();
				
				if(Gdx.app.getType() != ApplicationType.WebGL && !android){
					new button("Exit", () -> {
						Gdx.app.exit();
					});
				}
				
				get().pad(Unit.dp.inPixels(20));
			}};

			get().setVisible(nplay);
		}}.end();
		
		if(debug){
			new table(){{
				atop();
				new table("button"){{
					new label("[red]DEBUG MODE").scale(1);
				}}.end();
			}}.end();
		}
		
		new table(){{
			new table(){{
				get().background("button");
				
				new label("Respawning in"){{
					get().update(()->{
						get().setText("[yellow]Respawning in " + (int)(control.getRespawnTime()/60));
					});
					
					get().setFontScale(0.75f);
				}};
				
				visible(()->{
					return control.getRespawnTime() > 0 && !GameState.is(State.menu);
				});
			}};
		}}.end();
		
		loadingtable = new table("loadDim"){{
			new table("button"){{
				new label("[yellow]Loading..."){{
					get().setName("namelabel");
				}}.scale(1).pad(Unit.dp.inPixels(10));
			}}.end();
		}}.end().get();
		
		loadingtable.setVisible(false);
		
		tools = new Table();
		tools.addIButton("icon-cancel", Unit.dp.inPixels(42), ()->{
			player.recipe = null;
		});
		tools.addIButton("icon-rotate", Unit.dp.inPixels(42), ()->{
			player.rotation++;

			player.rotation %= 4;
		});
		tools.addIButton("icon-check", Unit.dp.inPixels(42), ()->{
			AndroidInput.place();
		});
		
		scene.add(tools);
		
		tools.setVisible(()->{
			return !GameState.is(State.menu) && android && player.recipe != null;
		});
		
		tools.update(()->{
			tools.setPosition(AndroidInput.mousex, Gdx.graphics.getHeight()-AndroidInput.mousey-15*Core.cameraScale, Align.top);
		});

		updateItems();

		build.end();
	}
	
	void updateRecipe(){
		Recipe recipe = player.recipe;
		desctable.clear();
		
		desctable.defaults().left();
		desctable.left();
		desctable.pad(12);
		
		desctable.add(recipe.result.formalName);
		desctable.row();
		desctable.addImage(Draw.region(recipe.result.name)).size(8*5).padTop(4);
		desctable.row();
		
		desctable.add().pad(2);
		
		Table requirements = new Table();
		
		desctable.row();
		
		desctable.add(requirements);
		desctable.left();
		
		for(ItemStack stack : recipe.requirements){
			ItemStack fs = stack;
			requirements.addImage(Draw.region("icon-"+stack.item.name())).size(8*3);
			Label reqlabel = new Label("");
			
			reqlabel.update(()->{
				int current = Inventory.getAmount(fs.item);
				String text = Mathf.clamp(current, 0, stack.amount) + "/" + stack.amount;
				
				reqlabel.setColor(current < stack.amount ? Colors.get("missingitems") : Color.WHITE);
				
				reqlabel.setText(text);
			});
			
			requirements.add(reqlabel);
			requirements.row();
		}
		
		desctable.row();
		
		if(recipe.result.description() != null){
			Label label = new Label(recipe.result.description());
			label.setWrap(true);
			desctable.add(label).width(170).padTop(4);
		}
	}
	
	public void updateWeapons(){
		weapontable.clearChildren();
		
		for(Weapon weapon : control.getWeapons()){
			ImageButton button = new ImageButton(Draw.region("weapon-"+weapon.name()), "static");
			button.getImageCell().size(40);
			button.setDisabled(true);
			
			if(weapon != player.weapon)
				button.setColor(Color.GRAY);
			
			weapontable.add(button).size(48, 52);
			
			Table tiptable = new Table();
			String description = weapon.description;
				
			tiptable.background("button");
			tiptable.add("[PURPLE]" + weapon.name(), 0.75f).left().padBottom(2f);
				
			tiptable.row();
			tiptable.row();
			tiptable.add("[ORANGE]" + description).left();
			tiptable.pad(10f);
			
			Tooltip tip = new Tooltip(tiptable);
			
			tip.setInstant(true);

			button.addListener(tip);
			
		}
	}
	
	public void showLoading(){
		showLoading("[yellow]Loading..");
	}
	
	public void showLoading(String text){
		loadingtable.<Label>find("namelabel").setText(text);
		loadingtable.setVisible(true);
		loadingtable.toFront();
	}
	
	public void hideLoading(){
		loadingtable.setVisible(false);
	}
	
	public void showPrefs(){
		prefs.show();
	}
	
	public void showControls(){
		keys.show();
	}
	
	public void showMenu(){
		menu.show();
	}
	
	public void hideMenu(){
		menu.hide();
		
		if(scene.getKeyboardFocus() != null && scene.getKeyboardFocus() instanceof Dialog){
			((Dialog)scene.getKeyboardFocus()).hide();
		}
	}
	
	public void showTutorial(){
		tutorial.show();
	}
	
	public void showRestart(){
		restart.show();
	}
	
	public void hideTooltip(){
		if(tooltip != null)
			tooltip.hide();
	}

	public void updateItems(){
		itemtable.clear();
		itemtable.left();

		for(Item stack : Inventory.getItemTypes()){
			Image image = new Image(Draw.region("icon-" + stack.name()));
			Label label = new Label("" + Inventory.getAmount(stack));
			label.setFontScale(fontscale*2f);
			itemtable.add(image).size(32).units(Unit.dp);
			itemtable.add(label).left();
			itemtable.row();
		}
	}
	
}