package beatstreets;

import java.util.HashMap;
import java.util.Map;

/**
 * The data of a single attack, as loaded from attacks.json by the original.
 * Accessed through the static {@link #ATTACKS} map.
 */
public class Attack {

  /** All attacks, mirroring the ATTACKS dictionary loaded from attacks.json. */
  public static final Map<String, Attack> ATTACKS = new HashMap<>();

  static {
    ATTACKS.put("punch", new Attack()
        .sprite("rpunch").strength(1).animTime(18).frameTime(6).frames(3)
        .hitFrames(2).staminaDamageMultiplier(1.5).comboNext(0, "secondpunch")
        .initialSound("punch_whoosh", 4).hitSound("punch_hit", 4));
    ATTACKS.put("secondpunch", new Attack()
        .sprite("lpunch").strength(1).animTime(18).frameTime(6).frames(3)
        .hitFrames(2).staminaDamageMultiplier(1.5).comboNext(0, "uppercut")
        .initialSound("punch_whoosh", 4).hitSound("punch_hit", 4));
    ATTACKS.put("uppercut", new Attack()
        .sprite("uppercut").strength(3).animTime(18).frameTime(6).frames(3)
        .hitFrames(2).staminaDamageMultiplier(3)
        .initialSound("punch_whoosh", 4).hitSound("punch_hit", 4));
    ATTACKS.put("kick", new Attack()
        .sprite("lowkick").strength(2).animTime(16).frameTime(8).frames(2)
        .hitFrames(1).recoveryTime(5).staminaCost(60)
        .initialSound("kick_whoosh", 4).hitSound("kick_hit", 4));
    ATTACKS.put("highkick", new Attack()
        .sprite("highkick").strength(2).animTime(16).frameTime(8).frames(2)
        .hitFrames(1).recoveryTime(5).staminaCost(60)
        .initialSound("kick_whoosh", 4).hitSound("kick_hit", 4));
    ATTACKS.put("flyingkick", new Attack()
        .sprite("flying_kick").strength(3).animTime(80).frameTime(6).frames(8)
        .hitFrames(2, 3, 4, 5).recoveryTime(8).flyingKick(true).staminaCost(300)
        .initialSound("flyingkick_whoosh", 1).hitSound("flyingkick_hit", 2));
    ATTACKS.put("elbow", new Attack()
        .sprite("elbow").strength(1).animTime(15).frames(1)
        .hitFrames(0).recoveryTime(5).rearAttack(true).staminaDamageMultiplier(3)
        .stunTimeMultiplier(2)
        .initialSound("elbow_whoosh", 4).hitSound("elbow_hit", 4));
    ATTACKS.put("stick", new Attack()
        .sprite("attack_stick").strength(4).animTime(24).frameTime(8).frames(3)
        .hitFrames(2).recoveryTime(4).reach(100)
        .initialSound("stick_whoosh", 4).hitSound("stick_hit", 2));
    ATTACKS.put("chain", new Attack()
        .sprite("attack_chain").strength(4).animTime(24).frameTime(8).frames(3)
        .hitFrames(2).recoveryTime(4).reach(130)
        .initialSound("stick_whoosh", 4).hitSound("chain_hit", 4));
    ATTACKS.put("barrel", new Attack()
        .sprite("throw_barrel").strength(5).animTime(24).frameTime(12).frames(2)
        .hitFrames(1).recoveryTime(4).throwAttack(true).staminaDamageMultiplier(10)
        .initialSound("barrel_throw", 1).hitSound("barrel_hit", 1));
    ATTACKS.put("vax_lpunch", new Attack()
        .sprite("lpunch").strength(1).animTime(18).frameTime(6).frames(3)
        .hitFrames(2).recoveryTime(50)
        .initialSound("punch_whoosh", 4).hitSound("punch_hit", 4));
    ATTACKS.put("vax_rpunch", new Attack()
        .sprite("rpunch").strength(1).animTime(18).frameTime(6).frames(3)
        .hitFrames(2).recoveryTime(50)
        .initialSound("punch_whoosh", 4).hitSound("punch_hit", 4));
    ATTACKS.put("vax_pound", new Attack()
        .sprite("pound").strength(2).animTime(30).frameTime(15).frames(2)
        .hitFrames(1).recoveryTime(60)
        .initialSound("kick_whoosh", 4).hitSound("kick_hit", 4));
    ATTACKS.put("hoodie_rpunch", new Attack()
        .sprite("rpunch").strength(1).animTime(18).frameTime(6).frames(3)
        .hitFrames(2).recoveryTime(50)
        .initialSound("stick_whoosh", 4).hitSound("stick_hit", 2));
    ATTACKS.put("hoodie_lpunch", new Attack()
        .sprite("lpunch").strength(1).animTime(18).frameTime(6).frames(3)
        .hitFrames(2).recoveryTime(50)
        .initialSound("stick_whoosh", 4).hitSound("stick_hit", 2));
    ATTACKS.put("hoodie_special", new Attack()
        .sprite("special").strength(1).animTime(36).frameTime(6).frames(6)
        .hitFrames(2, 5).recoveryTime(50)
        .initialSound("stick_whoosh", 4).hitSound("stick_hit", 2));
    ATTACKS.put("scooterboy_attack1", new Attack()
        .sprite("attack1").strength(2).animTime(72).frameTime(6).frames(12)
        .hitFrames(3).recoveryTime(50).reach(140)
        .initialSound("punch_whoosh", 4).hitSound("chain_hit", 4));
    ATTACKS.put("boss_rpunch", new Attack()
        .sprite("rpunch").strength(2).animTime(27).frameTime(9).frames(3)
        .hitFrames(2).recoveryTime(50)
        .initialSound("boss_punch", 2).hitSound("punch_hit", 4));
    ATTACKS.put("boss_lpunch", new Attack()
        .sprite("lpunch").strength(2).animTime(27).frameTime(9).frames(3)
        .hitFrames(2).recoveryTime(50)
        .initialSound("boss_punch", 2).hitSound("punch_hit", 4));
    ATTACKS.put("boss_kick", new Attack()
        .sprite("kick").strength(2).animTime(18).frameTime(9).frames(2)
        .hitFrames(1).recoveryTime(50)
        .initialSound("kick_whoosh", 4).hitSound("kick_hit", 4));
    ATTACKS.put("boss_grab_player", new Attack()
        .sprite("throw_player").strength(4).animTime(63).frameTime(9).frames(7)
        .hitFrames(6).grab(true).throwAttack(true)
        .initialSound("kick_whoosh", 4).hitSound("kick_hit", 4));
    ATTACKS.put("scooter_hit", new Attack()
        .strength(3).staminaDamageMultiplier(10).hitSound("scooter_hit", 1));
  }

  /** Returns the attack with the given name, like ATTACKS[name] in the original. */
  public static Attack get(String name) {
    return ATTACKS.get(name);
  }

  public String sprite;
  public int strength;
  public int recoveryTime;
  public int animTime;
  public int frameTime = 5;
  public int frames;
  public int[] hitFrames = new int[0];
  public int reach = 80;
  public boolean throwAttack;
  public boolean grab;
  public Map<Integer, String> comboNext;
  public boolean flyingKick;
  public int staminaCost = 10;
  public boolean rearAttack;
  public double staminaDamageMultiplier = 1;
  public double stunTimeMultiplier = 1;
  public String[] initialSound;
  public String[] hitSound;

  public Attack sprite(String sprite) {
    this.sprite = sprite;
    return this;
  }

  public Attack strength(int strength) {
    this.strength = strength;
    return this;
  }

  public Attack recoveryTime(int recoveryTime) {
    this.recoveryTime = recoveryTime;
    return this;
  }

  public Attack animTime(int animTime) {
    this.animTime = animTime;
    return this;
  }

  public Attack frameTime(int frameTime) {
    this.frameTime = frameTime;
    return this;
  }

  public Attack frames(int frames) {
    this.frames = frames;
    return this;
  }

  public Attack hitFrames(int... hitFrames) {
    this.hitFrames = hitFrames;
    return this;
  }

  public Attack reach(int reach) {
    this.reach = reach;
    return this;
  }

  public Attack throwAttack(boolean throwAttack) {
    this.throwAttack = throwAttack;
    return this;
  }

  public Attack grab(boolean grab) {
    this.grab = grab;
    return this;
  }

  public Attack comboNext(int button, String attack) {
    if (this.comboNext == null) {
      this.comboNext = new HashMap<>();
    }
    this.comboNext.put(button, attack);
    return this;
  }

  public Attack flyingKick(boolean flyingKick) {
    this.flyingKick = flyingKick;
    return this;
  }

  public Attack staminaCost(int staminaCost) {
    this.staminaCost = staminaCost;
    return this;
  }

  public Attack rearAttack(boolean rearAttack) {
    this.rearAttack = rearAttack;
    return this;
  }

  public Attack staminaDamageMultiplier(double staminaDamageMultiplier) {
    this.staminaDamageMultiplier = staminaDamageMultiplier;
    return this;
  }

  public Attack stunTimeMultiplier(double stunTimeMultiplier) {
    this.stunTimeMultiplier = stunTimeMultiplier;
    return this;
  }

  public Attack initialSound(String name, int count) {
    this.initialSound = new String[] {name, Integer.toString(count)};
    return this;
  }

  public Attack hitSound(String name, int count) {
    this.hitSound = new String[] {name, Integer.toString(count)};
    return this;
  }
}
