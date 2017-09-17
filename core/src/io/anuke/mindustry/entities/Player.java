package io.anuke.mindustry.entities;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.util.Angles;

public class Player extends DestructibleEntity{
	public Weapon weapon;
	public float breaktime = 0;
	
	public Recipe recipe;
	public int rotation;
	
	private Vector2 direction = new Vector2();
	private float speed = 1f;
	
	public Player(){
		hitsize = 5;
		
		maxhealth = 100;
		heal();
	}
	
	@Override
	public void onDeath(){
		remove();
		Effects.effect("explosion", this);
		Effects.shake(4f, 5f);
		Effects.sound("die", this);
		
		Vars.control.setRespawnTime(respawnduration);
	}
	
	@Override
	public void draw(){
		Draw.rect("player", x, y, direction.angle()-90);
	}
	
	@Override
	public void update(){
		
		float speed = this.speed;
		
		if(Vars.debug)
			speed = 3f;
		
		if(health < maxhealth && Timers.get(this, 50))
			health ++;
		
		vector.set(0, 0);
		
		if(Inputs.keyDown("up"))
			vector.y += speed;
		if(Inputs.keyDown("down"))
			vector.y -= speed;
		if(Inputs.keyDown("left"))
			vector.x -= speed;
		if(Inputs.keyDown("right"))
			vector.x += speed;
		
		boolean shooting = Inputs.buttonDown(Buttons.LEFT) && recipe == null && !ui.hasMouse();
		
		if(shooting && Timers.get(this, "reload", weapon.reload)){
			weapon.shoot(this);
			Sounds.play(weapon.shootsound);
		}
		
		vector.limit(speed);
		
		move(vector.x*delta, vector.y*delta, 4);
		
		if(!shooting){
			direction.add(vector.scl(delta));
			direction.limit(speed*6);
		}else{
			float angle = Angles.mouseAngle(x, y);
			direction.lerp(vector.set(0, 1).setAngle(angle), 0.26f);
			if(MathUtils.isEqual(angle, direction.angle(), 0.05f)){
				direction.setAngle(angle);
			}
		}
	}
}