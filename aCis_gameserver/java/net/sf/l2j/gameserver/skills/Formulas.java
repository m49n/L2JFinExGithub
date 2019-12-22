package net.sf.l2j.gameserver.skills;

import net.sf.finex.enums.ESkillAlignmentType;
import net.sf.finex.model.creature.attack.DamageInfo;
import net.sf.finex.model.talents.handlers.ProfessionalAnger;
import net.sf.l2j.Config;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.SevenSigns.CabalType;
import net.sf.l2j.gameserver.instancemanager.SevenSignsFestival;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.events.OnSuccessParry;
import net.sf.l2j.gameserver.model.actor.instance.Cubic;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.template.Race;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.Siege.SiegeSide;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.model.zone.type.L2MotherTreeZone;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;
import net.sf.l2j.gameserver.templates.skills.ESkillType;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Formulas {

	protected static final Logger _log = LoggerFactory.getLogger(Formulas.class.getName());

	private static final int HP_REGENERATE_PERIOD = 3000; // 3 secs

	public static final byte SHIELD_DEFENSE_FAILED = 0; // no shield defense
	public static final byte SHIELD_DEFENSE_SUCCEED = 1; // normal shield defense
	public static final byte SHIELD_DEFENSE_PERFECT_BLOCK = 2; // perfect block

	public static final byte SKILL_REFLECT_FAILED = 0; // no reflect
	public static final byte SKILL_REFLECT_SUCCEED = 1; // normal reflect, some damage reflected some other not
	public static final byte SKILL_REFLECT_VENGEANCE = 2; // 100% of the damage affect both

	private static final byte MELEE_ATTACK_RANGE = 40;

	public static final int MAX_STAT_VALUE = 100;

	private static final double[] STR_COMPUTE = new double[]{
		1.036,
		34.845
	};
	private static final double[] INT_COMPUTE = new double[]{
		1.020,
		31.375
	};
	private static final double[] DEX_COMPUTE = new double[]{
		1.0167,
		19.360
	};
	private static final double[] WIT_COMPUTE = new double[]{
		1.050,
		20.000
	};
	private static final double[] CON_COMPUTE = new double[]{
		1.030,
		27.632
	};
	private static final double[] MEN_COMPUTE = new double[]{
		1.016,
		-0.060
	};

	public static final double[] WIT_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] MEN_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] INT_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] STR_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] DEX_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] CON_BONUS = new double[MAX_STAT_VALUE];

	public static final double[] BASE_EVASION_ACCURACY = new double[MAX_STAT_VALUE];

	protected static final double[] SQRT_MEN_BONUS = new double[MAX_STAT_VALUE];
	protected static final double[] SQRT_CON_BONUS = new double[MAX_STAT_VALUE];

	static {
		for (int i = 0; i < STR_BONUS.length; i++) {
			STR_BONUS[i] = Math.floor(Math.pow(STR_COMPUTE[0], i - STR_COMPUTE[1]) * 100 + .5d) / 100;
		}
		for (int i = 0; i < INT_BONUS.length; i++) {
			INT_BONUS[i] = Math.floor(Math.pow(INT_COMPUTE[0], i - INT_COMPUTE[1]) * 100 + .5d) / 100;
		}
		for (int i = 0; i < DEX_BONUS.length; i++) {
			DEX_BONUS[i] = Math.floor(Math.pow(DEX_COMPUTE[0], i - DEX_COMPUTE[1]) * 100 + .5d) / 100;
		}
		for (int i = 0; i < WIT_BONUS.length; i++) {
			WIT_BONUS[i] = Math.floor(Math.pow(WIT_COMPUTE[0], i - WIT_COMPUTE[1]) * 100 + .5d) / 100;
		}
		for (int i = 0; i < CON_BONUS.length; i++) {
			CON_BONUS[i] = Math.floor(Math.pow(CON_COMPUTE[0], i - CON_COMPUTE[1]) * 100 + .5d) / 100;
		}
		for (int i = 0; i < MEN_BONUS.length; i++) {
			MEN_BONUS[i] = Math.floor(Math.pow(MEN_COMPUTE[0], i - MEN_COMPUTE[1]) * 100 + .5d) / 100;
		}

		for (int i = 0; i < BASE_EVASION_ACCURACY.length; i++) {
			BASE_EVASION_ACCURACY[i] = Math.sqrt(i) * 6;
		}

		// Precompute square root values
		for (int i = 0; i < SQRT_CON_BONUS.length; i++) {
			SQRT_CON_BONUS[i] = Math.sqrt(CON_BONUS[i]);
		}
		for (int i = 0; i < SQRT_MEN_BONUS.length; i++) {
			SQRT_MEN_BONUS[i] = Math.sqrt(MEN_BONUS[i]);
		}
	}

	private static final double[] karmaMods
			= {
				0,
				0.772184315,
				2.069377971,
				2.315085083,
				2.467800843,
				2.514219611,
				2.510075822,
				2.532083418,
				2.473028945,
				2.377178509,
				2.285526643,
				2.654635163,
				2.963434737,
				3.266100461,
				3.561112664,
				3.847320291,
				4.123878064,
				4.390194136,
				4.645886341,
				4.890745518,
				5.124704707,
				6.97914069,
				7.270620642,
				7.548951721,
				7.81438302,
				8.067235867,
				8.307889422,
				8.536768399,
				8.754332624,
				8.961068152,
				9.157479758,
				11.41901707,
				11.64989746,
				11.87007991,
				12.08015809,
				12.28072687,
				12.47237891,
				12.65570177,
				12.83127553,
				12.99967093,
				13.16144786,
				15.6563607,
				15.84513182,
				16.02782135,
				16.20501182,
				16.37727218,
				16.54515749,
				16.70920885,
				16.86995336,
				17.02790439,
				17.18356182,
				19.85792061,
				20.04235517,
				20.22556446,
				20.40806338,
				20.59035551,
				20.77293378,
				20.95628115,
				21.1408714,
				21.3271699,
				21.51563446,
				24.3895427,
				24.61486587,
				24.84389213,
				25.07711247,
				25.31501442,
				25.55808296,
				25.80680152,
				26.06165297,
				26.32312062,
				26.59168923,
				26.86784604,
				27.15208178,
				27.44489172,
				27.74677676,
				28.05824444,
				28.37981005,
				28.71199773,
				29.05534154,
				29.41038662,
				29.77769028
			};

	/**
	 * @param cha The character to make checks on.
	 * @return the HP regen rate (base + modifiers).
	 */
	public static final double calcHpRegen(Creature cha) {
		double init = cha.getTemplate().getBaseHpReg();
		double hpRegenMultiplier = cha.isRaid() ? Config.RAID_HP_REGEN_MULTIPLIER : Config.HP_REGEN_MULTIPLIER;
		double hpRegenBonus = 0;

		if (cha.isChampion()) {
			hpRegenMultiplier *= Config.CHAMPION_HP_REGEN;
		}

		if (cha instanceof Player) {
			Player player = (Player) cha;

			// Calculate correct baseHpReg value for certain level of PC
			init += (player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5;

			// SevenSigns Festival modifier
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant()) {
				hpRegenMultiplier *= calcFestivalRegenModifier(player);
			} else if (calcSiegeRegenModifer(player)) {
				hpRegenMultiplier *= 1.5;
			}

			if (player.isInsideZone(ZoneId.CLAN_HALL) && player.getClan() != null) {
				int clanHallIndex = player.getClan().getHideoutId();
				if (clanHallIndex > 0) {
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null) {
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null) {
							hpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLvl() / 100;
						}
					}
				}
			}

			// Mother Tree effect is calculated at last
			if (player.isInsideZone(ZoneId.MOTHER_TREE)) {
				L2MotherTreeZone zone = ZoneManager.getInstance().getZone(player, L2MotherTreeZone.class);
				int hpBonus = zone == null ? 0 : zone.getHpRegenBonus();
				hpRegenBonus += hpBonus;
			}

			// Calculate Movement bonus
			if (player.isSitting()) {
				hpRegenMultiplier *= 1.5; // Sitting
			} else if (!player.isMoving()) {
				hpRegenMultiplier *= 1.1; // Staying
			} else if (player.isRunning()) {
				hpRegenMultiplier *= 0.7; // Running
			}
		}
		// Add CON bonus
		init *= cha.getLevelMod() * CON_BONUS[cha.getCON()];

		if (init < 1) {
			init = 1;
		}

		return cha.calcStat(Stats.RegenHP, init, null, null) * hpRegenMultiplier + hpRegenBonus;
	}

	/**
	 * @param cha The character to make checks on.
	 * @return the MP regen rate (base + modifiers).
	 */
	public static final double calcMpRegen(Creature cha) {
		double init = cha.getTemplate().getBaseMpReg();
		double mpRegenMultiplier = cha.isRaid() ? Config.RAID_MP_REGEN_MULTIPLIER : Config.MP_REGEN_MULTIPLIER;
		double mpRegenBonus = 0;

		if (cha instanceof Player) {
			Player player = (Player) cha;

			// Calculate correct baseMpReg value for certain level of PC
			init += 0.3 * ((player.getLevel() - 1) / 10.0);

			// SevenSigns Festival modifier
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant()) {
				mpRegenMultiplier *= calcFestivalRegenModifier(player);
			}

			// Mother Tree effect is calculated at last
			if (player.isInsideZone(ZoneId.MOTHER_TREE)) {
				L2MotherTreeZone zone = ZoneManager.getInstance().getZone(player, L2MotherTreeZone.class);
				int mpBonus = zone == null ? 0 : zone.getMpRegenBonus();
				mpRegenBonus += mpBonus;
			}

			if (player.isInsideZone(ZoneId.CLAN_HALL) && player.getClan() != null) {
				int clanHallIndex = player.getClan().getHideoutId();
				if (clanHallIndex > 0) {
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null) {
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null) {
							mpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLvl() / 100;
						}
					}
				}
			}

			// Calculate Movement bonus
			if (player.isSitting()) {
				mpRegenMultiplier *= 1.5; // Sitting
			} else if (!player.isMoving()) {
				mpRegenMultiplier *= 1.1; // Staying
			} else if (player.isRunning()) {
				mpRegenMultiplier *= 0.7; // Running
			}
		}
		// Add MEN bonus
		init *= cha.getLevelMod() * MEN_BONUS[cha.getMEN()];

		if (init < 1) {
			init = 1;
		}

		return cha.calcStat(Stats.RegenMP, init, null, null) * mpRegenMultiplier + mpRegenBonus;
	}

	/**
	 * @param player The player to make checks on.
	 * @return the CP regen rate (base + modifiers).
	 */
	public static final double calcCpRegen(Player player) {
		// Calculate correct baseHpReg value for certain level of PC
		double init = player.getTemplate().getBaseHpReg() + ((player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5);
		double cpRegenMultiplier = Config.CP_REGEN_MULTIPLIER;

		// Calculate Movement bonus
		if (player.isSitting()) {
			cpRegenMultiplier *= 1.5; // Sitting
		} else if (!player.isMoving()) {
			cpRegenMultiplier *= 1.1; // Staying
		} else if (player.isRunning()) {
			cpRegenMultiplier *= 0.7; // Running
		}
		// Apply CON bonus
		init *= player.getLevelMod() * CON_BONUS[player.getCON()];

		if (init < 1) {
			init = 1;
		}

		return player.calcStat(Stats.RegenCP, init, null, null) * cpRegenMultiplier;
	}

	public static final double calcFestivalRegenModifier(Player activeChar) {
		final int[] festivalInfo = SevenSignsFestival.getInstance().getFestivalForPlayer(activeChar);
		final CabalType oracle = CabalType.VALUES[festivalInfo[0]];
		final int festivalId = festivalInfo[1];
		int[] festivalCenter;

		// If the player isn't found in the festival, leave the regen rate as it is.
		if (festivalId < 0) {
			return 0;
		}

		// Retrieve the X and Y coords for the center of the festival arena the player is in.
		if (oracle == CabalType.DAWN) {
			festivalCenter = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
		} else {
			festivalCenter = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];
		}

		// Check the distance between the player and the player spawn point, in the center of the arena.
		double distToCenter = activeChar.getPlanDistanceSq(festivalCenter[0], festivalCenter[1]);

		if (Config.DEVELOPER) {
			_log.info("Distance: " + distToCenter + ", RegenMulti: " + (distToCenter * 2.5) / 50);
		}

		return 1.0 - (distToCenter * 0.0005); // Maximum Decreased Regen of ~ -65%;
	}

	/**
	 * @param activeChar the player to test on.
	 * @return true if the player is near one of his clan HQ (+50% regen boost).
	 */
	public static final boolean calcSiegeRegenModifer(Player activeChar) {
		if (activeChar == null || activeChar.getClan() == null) {
			return false;
		}

		final Siege siege = CastleManager.getInstance().getSiege(activeChar);
		if (siege == null || !siege.checkSide(activeChar.getClan(), SiegeSide.ATTACKER)) {
			return false;
		}

		return MathUtil.checkIfInRange(200, activeChar, activeChar.getClan().getFlag(), true);
	}

	/**
	 * @param attacker The attacker, from where the blow comes from.
	 * @param target The victim of the blow.
	 * @param skill The skill used.
	 * @param shld True if victim was wearign a shield.
	 * @param ss True if ss were activated.
	 * @return blow damage based on cAtk
	 */
	public static double calcBlowDamage(Creature attacker, Creature target, L2Skill skill, byte shld, boolean parry, boolean ss) {
		double defence = target.getPDef(attacker);
		switch (shld) {
			case SHIELD_DEFENSE_SUCCEED:
				defence += target.getShldDef();
				break;

			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}

		if (parry) {
			defence += target.getActiveWeaponItem().getStats().get(Stats.PAtk);
		}

		final boolean isPvP = attacker.isPlayable() && target.isPlayable();

		double power = skill.getPower();
		double damage = 0;
		damage += calcValakasAttribute(attacker, target, skill);

		if (ss) {
			damage *= 2.;

			if (skill.getSSBoost() > 0) {
				power *= skill.getSSBoost();
			}
		}

		damage += attacker.calcStat(Stats.CriticalDamage, (damage + power), target, skill);
		damage += attacker.calcStat(Stats.CriticalDamageAdd, 0, target, skill) * 6.5;
		damage *= target.calcStat(Stats.CriticalVuln, 1, target, skill);

		// get the vulnerability for the instance due to skills (buffs, passives, toggles, etc)
		damage = target.calcStat(Stats.DaggerWpnVuln, damage, target, null);
		damage *= 70. / defence;

		// Random weapon damage
		damage *= attacker.getRandomDamageMultiplier();

		// Dmg bonusses in PvP fight
		if (isPvP) {
			damage *= attacker.calcStat(Stats.PvpSkillDmg, 1, null, null);
		}

		return damage < 1 ? 1. : damage;
	}

	/**
	 * Calculated damage caused by ATTACK of attacker on target, called
	 * separatly for each weapon, if dual-weapon is used.
	 *
	 * @param attacker player or NPC that makes ATTACK
	 * @param target player or NPC, target of ATTACK
	 * @param skill skill used.
	 * @param shld target was using a shield or not.
	 * @param crit if the ATTACK have critical success
	 * @param parry if attack was parryed the damage reduce fro activeWeaponAtk
	 * @param ss if weapon item was charged by soulshot
	 * @return damage points
	 */
	public static final double calcPhysDam(Creature attacker, Creature target, L2Skill skill, DamageInfo info, boolean ss) {
		if (attacker.isPlayer()) {
			Player pcInst = (Player) attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage()) {
				return 0;
			}
		}

		double defence = target.getPDef(attacker);
		switch (info.shieldResult) {
			case SHIELD_DEFENSE_SUCCEED:
				if (!Config.ALT_GAME_SHIELD_BLOCKS) {
					defence += target.getShldDef();
				}
				break;

			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1.;
		}

		if (info.isParry) {
			defence += target.getActiveWeaponItem().getStats().get(Stats.PAtk);
		}

		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		double damage = attacker.getPAtk(target);

		damage += calcValakasAttribute(attacker, target, skill);

		if (ss) {
			damage *= 2;
		}

		if (skill != null) {
			double skillpower = skill.getPower(attacker);

			final float ssBoost = skill.getSSBoost();
			if (ssBoost > 0 && ss) {
				skillpower *= ssBoost;
			}

			damage += skillpower;

			if (skill.isAoE()) {
				damage = calcAoEDamage(attacker, target, skill, damage);
			}
		}

		// defence modifier depending of the attacker weapon
		Weapon weapon = attacker.getActiveWeaponItem();
		Stats stat = null;
		if (weapon != null) {
			stat = weapon.getItemType().getVuln();
		}

		if (info.isCrit) {
			// Finally retail like formula
			damage = 2 * attacker.calcStat(Stats.CriticalDamage, 1, target, skill) * target.calcStat(Stats.CriticalVuln, 1, target, null) * (70 * damage / defence);
			// Crit dmg add is almost useless in normal hits...
			damage += (attacker.calcStat(Stats.CriticalDamageAdd, 0, target, skill) * 70 / defence);
		} else {
			damage = 70 * damage / defence;
		}

		if (stat != null) {
			damage = target.calcStat(stat, damage, target, null);
		}

		// Weapon random damage ; invalid for CHARGEDAM skills.
		if (skill == null || skill.getEffectType() != ESkillType.CHARGEDAM) {
			damage *= attacker.getRandomDamageMultiplier();
		}

		if (info.shieldResult > 0 && Config.ALT_GAME_SHIELD_BLOCKS) {
			damage -= target.getShldDef();
			if (damage < 0) {
				damage = 0;
			}
		}

		// Attack against defence of monsters
		if (target.isNpc()) {
			final Race race = ((Npc) target).getTemplate().getRace();
			if (race.getPAtkStat() != null && race.getPDefStat() != null) {
				damage *= attacker.calcStat(race.getPAtkStat(), 0, target, null) / 100. + 1;
				damage *= 1 - target.calcStat(race.getPDefStat(), 0, target, null) / 100;
			}
		}

		if (damage > 0 && damage < 1) {
			damage = 1;
		} else if (damage < 0) {
			damage = 0;
		}

		// Dmg bonuses in PvP fight
		if (isPvP) {
			if (skill == null) {
				damage *= attacker.calcStat(Stats.PvpPhysDmg, 1, null, null);
			} else {
				damage *= attacker.calcStat(Stats.PvpSkillDmg, 1, null, null);
			}
		}

		// Weapon elemental damages
		damage += calcElemental(attacker, target, null);

		// Additional damage dealing
		damage = attacker.calcStat(Stats.PhysicalDamage, damage, target, skill);

		return damage;
	}

	public static final double calcMagicDam(Creature attacker, Creature target, L2Skill skill, int shld, boolean parry, boolean ss, boolean bss, boolean mcrit) {
		if (attacker.isPlayer()) {
			Player pcInst = (Player) attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage()) {
				return 0;
			}
		}

		double mDef = target.getMDef(attacker, skill);
		switch (shld) {
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef();
				break;

			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1.;
		}

		if (parry) {
			mDef += target.getActiveWeaponItem().getStats().get(Stats.MAtk);
		}

		double mAtk = attacker.getMAtk(target, skill);

		if (bss) {
			mAtk *= 4;
		} else if (ss) {
			mAtk *= 2;
		}

		double damage = 91 * Math.sqrt(mAtk) / mDef * skill.getPower(attacker);

		// Failure calculation
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill)) {
			if (attacker instanceof Player) {
				if (calcMagicSuccess(attacker, target, skill) && (target.getLevel() - attacker.getLevel()) <= 9) {
					if (skill.getSkillType() == ESkillType.DRAIN) {
						attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DRAIN_HALF_SUCCESFUL));
					} else {
						attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
					}

					damage /= 2;
				} else {
					attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					damage = 1;
				}
			}

			if (target instanceof Player) {
				if (skill.getSkillType() == ESkillType.DRAIN) {
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_DRAIN).addCharName(attacker));
				} else {
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_MAGIC).addCharName(attacker));
				}
			}
		} else if (mcrit) {
			damage *= 4;
		}

		// Pvp bonuses for dmg
		if (attacker instanceof Playable && target instanceof Playable) {
			if (skill.isMagic()) {
				damage *= attacker.calcStat(Stats.PvpMagicalDmg, 1, null, null);
			} else {
				damage *= attacker.calcStat(Stats.PvpSkillDmg, 1, null, null);
			}
		}

		damage *= calcElemental(attacker, target, skill);

		damage = attacker.calcStat(Stats.MagicalDamage, damage, target, skill);

		if (attacker.isPlayer()) {
			attacker.getParams().set("calcmdam_ss", ss);
			attacker.getParams().set("calcmdam_bss", bss);
			attacker.getParams().set("calcmdam_mcrit", mcrit);
			attacker.getParams().set("calcmdam_damage", damage);
		}

		return damage;
	}

	public static final double calcMagicDam(Cubic attacker, Creature target, L2Skill skill, boolean mcrit, byte shld) {
		double mDef = target.getMDef(attacker.getOwner(), skill);
		switch (shld) {
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef();
				break;

			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}

		double damage = 91 / mDef * skill.getPower();
		Player owner = attacker.getOwner();

		// Failure calculation
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(owner, target, skill)) {
			if (calcMagicSuccess(owner, target, skill) && (target.getLevel() - skill.getMagicLevel()) <= 9) {
				if (skill.getSkillType() == ESkillType.DRAIN) {
					owner.sendPacket(SystemMessageId.DRAIN_HALF_SUCCESFUL);
				} else {
					owner.sendPacket(SystemMessageId.ATTACK_FAILED);
				}

				damage /= 2;
			} else {
				owner.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
				damage = 1;
			}

			if (target instanceof Player) {
				if (skill.getSkillType() == ESkillType.DRAIN) {
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_DRAIN).addCharName(owner));
				} else {
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_MAGIC).addCharName(owner));
				}
			}
		} else if (mcrit) {
			damage *= 4;
		}

		damage *= calcElemental(owner, target, skill);

		damage = owner.calcStat(Stats.MagicalDamage, damage, target, skill);

		return damage;
	}

	/**
	 * @param rate The value to make check on.
	 * @return true in case of critical hit
	 */
	public static final boolean calcCrit(double rate) {
		return rate > Rnd.get(1000);
	}

	/**
	 * Calcul value of blow success
	 *
	 * @param activeChar The character delaing the blow.
	 * @param target The victim.
	 * @param chance The base chance of landing a blow.
	 * @return true if successful, false otherwise
	 */
	public static final boolean calcBlow(Creature activeChar, Creature target, int chance) {
		return activeChar.calcStat(Stats.BlowRate, chance * (1.0 + (activeChar.getDEX() - 20) / 100), target, null) > Rnd.get(100);
	}

	/**
	 * Calcul value of lethal chance
	 *
	 * @param activeChar The character delaing the blow.
	 * @param target The victim.
	 * @param baseLethal The base lethal chance of the skill.
	 * @param magiclvl
	 * @return
	 */
	public static final double calcLethal(Creature activeChar, Creature target, int baseLethal, int magiclvl) {
		double chance = 0;
		if (magiclvl > 0) {
			int delta = ((magiclvl + activeChar.getLevel()) / 2) - 1 - target.getLevel();

			if (delta >= -3) {
				chance = (baseLethal * ((double) activeChar.getLevel() / target.getLevel()));
			} else if (delta < -3 && delta >= -9) {
				chance = (-3) * (baseLethal / (delta));
			} else {
				chance = baseLethal / 15;
			}
		} else {
			chance = (baseLethal * ((double) activeChar.getLevel() / target.getLevel()));
		}

		chance = 10 * activeChar.calcStat(Stats.LethalRate, chance, target, null);

		if (Config.DEVELOPER) {
			_log.info("Current calcLethal: " + chance + " / 1000");
		}

		return chance;
	}

	public static final void calcLethalHit(Creature activeChar, Creature target, L2Skill skill) {
		if (target.isRaid() || target instanceof Door) {
			return;
		}

		// If one of following IDs is found, return false (Tyrannosaurus x 3, Headquarters)
		if (target instanceof Npc) {
			switch (((Npc) target).getNpcId()) {
				case 22215:
				case 22216:
				case 22217:
				case 35062:
					return;
			}
		}

		// Second lethal effect (hp to 1 for npc, cp/hp to 1 for player).
		if (skill.getLethalChance2() > 0 && Rnd.get(1000) < calcLethal(activeChar, target, skill.getLethalChance2(), skill.getMagicLevel())) {
			if (target instanceof Npc) {
				target.reduceCurrentHp(target.getCurrentHp() - 1, activeChar, skill);
			} else if (target instanceof Player) // If is a active player set his HP and CP to 1
			{
				Player player = (Player) target;
				if (!player.isInvul()) {
					if (!(activeChar instanceof Player && (((Player) activeChar).isGM() && !((Player) activeChar).getAccessLevel().canGiveDamage()))) {
						player.setCurrentHp(1);
						player.setCurrentCp(1);
						player.sendPacket(SystemMessageId.LETHAL_STRIKE);
					}
				}
			}
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL));
		} // First lethal effect (hp/2 for npc, cp to 1 for player).
		else if (skill.getLethalChance1() > 0 && Rnd.get(1000) < calcLethal(activeChar, target, skill.getLethalChance1(), skill.getMagicLevel())) {
			if (target instanceof Npc) {
				target.reduceCurrentHp(target.getCurrentHp() / 2, activeChar, skill);
			} else if (target instanceof Player) {
				Player player = (Player) target;
				if (!player.isInvul()) {
					if (!(activeChar instanceof Player && (((Player) activeChar).isGM() && !((Player) activeChar).getAccessLevel().canGiveDamage()))) {
						player.setCurrentCp(1);
						player.sendPacket(SystemMessageId.LETHAL_STRIKE);
					}
				}
			}
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL));
		}
	}

	public static final boolean calcMCrit(int mRate) {
		if (Config.DEVELOPER) {
			_log.info("Current mCritRate: " + mRate + "/1000");
		}

		return mRate > Rnd.get(1000);
	}

	/**
	 * Check if casting process is canceled due to hit.
	 *
	 * @param target The target to make checks on.
	 * @param dmg The amount of dealt damages.
	 */
	public static final void calcCastBreak(Creature target, double dmg) {
		// Don't go further for invul characters or raid bosses.
		if (target.isRaid() || target.isInvul()) {
			return;
		}

		// Break automatically the skill cast if under attack.
		if (target instanceof Player && ((Player) target).getFusionSkill() != null) {
			target.breakCast();
			return;
		}

		// Initialization to 15% for magical skills ; don't go further for ppl casting a physical skill
		if (!target.isCastingNow() && (target.getLastSkillCast() != null && !target.getLastSkillCast().isMagic())) {
			return;
		}

		// Calculate all modifiers for ATTACK_CANCEL ; chance to break is higher with higher dmg, and is affected by target MEN.
		double rate = target.calcStat(Stats.AttackCancel, 15 + Math.sqrt(13 * dmg) - (MEN_BONUS[target.getMEN()] * 100 - 100), null, null);

		// Adjust the rate to be between 1 and 99
		if (rate > 99) {
			rate = 99;
		} else if (rate < 1) {
			rate = 1;
		}

		if (Config.DEVELOPER) {
			_log.info("calcCastBreak rate: " + (int) rate + "%");
		}

		if (Rnd.get(100) < rate) {
			target.breakCast();
		}
	}

	/**
	 * Calculate delay (in milliseconds) before next ATTACK.
	 *
	 * @param attacker
	 * @param target
	 * @param rate
	 * @return delay in ms.
	 */
	public static final int calcPAtkSpd(Creature attacker, Creature target, double rate) {
		if (rate < 2) {
			return 2700;
		}

		return (int) (470000 / rate);
	}

	/**
	 * Calculate delay (in milliseconds) for skills cast.
	 *
	 * @param attacker
	 * @param skill used to know if skill is magic or no.
	 * @param skillTime
	 * @return delay in ms.
	 */
	public static final int calcAtkSpd(Creature attacker, L2Skill skill, double skillTime) {
		if (skill.isMagic()) {
			return (int) (skillTime * 333 / attacker.getMAtkSpd());
		}

		return (int) (skillTime * 333 / attacker.getPAtkSpd());
	}

	/**
	 * Calculate the hit/miss chance.
	 *
	 * @param attacker : The attacker to make checks on.
	 * @param target : The target to make checks on.
	 * @return true if hit is missed, false if it evaded.
	 */
	public static boolean calcHitMiss(Creature attacker, Creature target) {
		int chance = (80 + (2 * (attacker.getAccuracy() - target.getEvasionRate(attacker)))) * 10;

		double modifier = 100;

		// Get high or low Z bonus.
		if (attacker.getZ() - target.getZ() > 50) {
			modifier += 3;
		} else if (attacker.getZ() - target.getZ() < -50) {
			modifier -= 3;
		}

		// Get weather bonus. TODO: rain support (-3%).
		if (GameTimeTaskManager.getInstance().isNight()) {
			modifier -= 10;
		}

		// Get position bonus.
		if (attacker.isBehindTarget()) {
			modifier += 10;
		} else if (!attacker.isInFrontOfTarget()) {
			modifier += 5;
		}

		chance *= modifier / 100;

		if (Config.DEVELOPER) {
			_log.info("calcHitMiss rate: " + chance / 10 + "%, modifier : x" + modifier / 100);
		}

		return Math.max(Math.min(chance, 980), 200) < Rnd.get(1000);
	}

	/**
	 * Test the shield use.
	 *
	 * @param attacker The attacker.
	 * @param target The victim ; make check about his shield.
	 * @param skill The skill the attacker has used.
	 * @return 0 = shield defense doesn't succeed<br>
	 * 1 = shield defense succeed<br>
	 * 2 = perfect block
	 */
	public static byte calcShldUse(Creature attacker, Creature target, L2Skill skill) {
		// Ignore shield skills types bypass the shield use.
		if (skill != null && skill.ignoreShield()) {
			return 0;
		}

		Item item = target.getSecondaryWeaponItem();
		if (item == null || !(item instanceof Armor)) {
			return 0;
		}

		double shldRate = target.calcStat(Stats.BlockRate, 0, attacker, null) * DEX_BONUS[target.getDEX()];
		if (shldRate == 0.0) {
			return 0;
		}

		int degreeside = (int) target.calcStat(Stats.BlockAngle, 120, null, null);
		if (degreeside < 360 && (!target.isFacing(attacker, degreeside))) {
			return 0;
		}

		byte shldSuccess = SHIELD_DEFENSE_FAILED;

		// if attacker use bow and target wear shield, shield block rate is multiplied by 1.3 (30%)
		if (attacker.getAttackType() == WeaponType.BOW) {
			shldRate *= 1.3;
		}

		if (shldRate > 0 && 100 - Config.ALT_PERFECT_SHLD_BLOCK < Rnd.get(100)) {
			shldSuccess = SHIELD_DEFENSE_PERFECT_BLOCK;
		} else if (shldRate > Rnd.get(100)) {
			shldSuccess = SHIELD_DEFENSE_SUCCEED;
		}

		if (target instanceof Player) {
			switch (shldSuccess) {
				case SHIELD_DEFENSE_SUCCEED:
					((Player) target).sendPacket(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL);
					break;

				case SHIELD_DEFENSE_PERFECT_BLOCK:
					((Player) target).sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
					break;
			}
		}

		return shldSuccess;
	}

	public static boolean calcMagicAffected(Creature actor, Creature target, L2Skill skill) {
		ESkillType type = skill.getSkillType();
		if (target.isRaid() && !calcRaidAffected(type)) {
			return false;
		}

		double defence = 0;

		if (skill.isActive() && skill.isOffensive()) {
			defence = target.getMDef(actor, skill);
		}

		double attack = 2 * actor.getMAtk(target, skill) * calcSkillVulnerability(actor, target, skill, type);
		double d = (attack - defence) / (attack + defence);

		d += 0.5 * Rnd.nextGaussian();
		return d > 0;
	}

	public static double calcSkillVulnerability(Creature attacker, Creature target, L2Skill skill, ESkillType type) {
		double multiplier = 1;

		// Get the elemental damages.
		if (skill.getElement() > 0) {
			multiplier *= Math.sqrt(calcElemental(attacker, target, skill));
		}

		// Get the skillType to calculate its effect in function of base stats of the target.
		switch (type) {
			case BLEED:
				multiplier = target.calcStat(Stats.BleedVuln, multiplier, target, null);
				break;

			case POISON:
				multiplier = target.calcStat(Stats.PoisonVuln, multiplier, target, null);
				break;

			case STUN:
				multiplier = target.calcStat(Stats.StunVuln, multiplier, target, null);
				break;

			case PARALYZE:
				multiplier = target.calcStat(Stats.ParalyzeVuln, multiplier, target, null);
				break;

			case ROOT:
				multiplier = target.calcStat(Stats.RootVuln, multiplier, target, null);
				break;

			case SLEEP:
				multiplier = target.calcStat(Stats.SleepVuln, multiplier, target, null);
				break;

			case MUTE:
				multiplier = target.calcStat(Stats.MuteVuln, multiplier, target, null);
				break;

			case FEAR:
				multiplier = target.calcStat(Stats.FearVuln, multiplier, target, null);
				break;

			case BETRAY:
			case AGGDEBUFF:
			case AGGREDUCE_CHAR:
			case ERASE:
			case CONFUSION:
				multiplier = target.calcStat(Stats.DerangementVuln, multiplier, target, null);
				break;

			case DEBUFF:
			case WEAKNESS:
				multiplier = target.calcStat(Stats.DebuffVuln, multiplier, target, null);
				break;

			case CANCEL:
				multiplier = target.calcStat(Stats.CancelVuln, multiplier, target, null);
				break;
		}

		// Return a multiplier (exemple with resist shock : 1 + (-0,4 stun vuln) = 0,6%
		return multiplier;
	}

	private static double calcSkillStatModifier(ESkillType type, Creature target) {
		double multiplier = 1;

		switch (type) {
			case STUN:
			case BLEED:
			case POISON:
				multiplier = 2 - SQRT_CON_BONUS[target.getStat().getCON()];
				break;

			case SLEEP:
			case DEBUFF:
			case WEAKNESS:
			case ERASE:
			case ROOT:
			case MUTE:
			case FEAR:
			case BETRAY:
			case CONFUSION:
			case AGGREDUCE_CHAR:
			case PARALYZE:
				multiplier = 2 - SQRT_MEN_BONUS[target.getStat().getMEN()];
				break;
		}

		return Math.max(0, multiplier);
	}

	private static double getLevelModifier(Creature attacker, Creature target, L2Skill skill) {
		if (skill.getLevelDepend() == 0) {
			return 1;
		}

		final int maxSkillLevel = SkillTable.getInstance().getMaxLevel(skill.getId());
		final double skillLevelMod = skill.getLevel() / maxSkillLevel * attacker.getLevel() + skill.getLevelDepend() - target.getLevel();
		return 1 + ((skillLevelMod < 0 ? 0.01 : 0.005) * skillLevelMod);
	}

	private static double getMatkModifier(Creature attacker, Creature target, L2Skill skill, boolean bss) {
		double mAtkModifier = 1;

		if (skill.isMagic()) {
			final double mAtk = attacker.getMAtk(target, skill);
			double val = mAtk;
			if (bss) {
				val = mAtk * 4.0;
			}

			mAtkModifier = (Math.sqrt(val) / target.getMDef(attacker, skill)) * 11.0;
		}
		return mAtkModifier;
	}

	public static boolean calcEffectSuccess(Creature attacker, Creature target, EffectTemplate effect, L2Skill skill, int shld, boolean bss) {
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK) // perfect block
		{
			return false;
		}

		final ESkillType type = effect.effectType;
		final double baseChance = effect.effectPower;

		if (type == null) {
			return Rnd.get(100) < baseChance;
		}

		if (type.equals(ESkillType.CANCEL)) // CANCEL type lands always
		{
			return true;
		}

		final double statModifier = calcSkillStatModifier(type, target);
		final double skillModifier = calcSkillVulnerability(attacker, target, skill, type);
		final double mAtkModifier = getMatkModifier(attacker, target, skill, bss);
		final double lvlModifier = getLevelModifier(attacker, target, skill);
		final double calced = attacker.calcStat(Stats.SpellPower, Math.min((baseChance * statModifier * skillModifier * mAtkModifier * lvlModifier), 99), target, skill);
		final double rate = Math.max(1, Math.min(calced, 99));

		if (Config.DEVELOPER) {
			_log.info("calcEffectSuccess(): Name:" + skill.getName() + " eff.type:" + type.toString() + " power:" + baseChance + " statMod:" + String.format("%1.2f", statModifier) + " skillMod:" + String.format("%1.2f", skillModifier) + " mAtkMod:" + String.format("%1.2f", mAtkModifier) + " lvlMod:" + String.format("%1.2f", lvlModifier) + " total:" + String.format("%1.2f", rate) + "%");
		}

		return (Rnd.get(100) < rate);
	}

	public static boolean calcSkillSuccess(Creature attacker, Creature target, L2Skill skill, int shld, boolean bss) {
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK) // perfect block
		{
			return false;
		}

		final ESkillType type = skill.getEffectType();

		if (target.isRaid() && !calcRaidAffected(type)) {
			return false;
		}

		final double baseChance = skill.getEffectPower();
		if (skill.ignoreResists()) {
			return (Rnd.get(100) < baseChance);
		}

		final double statModifier = calcSkillStatModifier(type, target);
		final double skillModifier = calcSkillVulnerability(attacker, target, skill, type);
		final double mAtkModifier = getMatkModifier(attacker, target, skill, bss);
		final double lvlModifier = getLevelModifier(attacker, target, skill);
		final double calced = attacker.calcStat(Stats.SpellPower, Math.min((baseChance * statModifier * skillModifier * mAtkModifier * lvlModifier), 99), target, skill);
		final double rate = Math.max(1, calced);

		if (Config.DEVELOPER) {
			_log.info("calcSkillSuccess(): Name:" + skill.getName() + " type:" + skill.getSkillType().toString() + " power:" + baseChance + " statMod:" + String.format("%1.2f", statModifier) + " skillMod:" + String.format("%1.2f", skillModifier) + " mAtkMod:" + String.format("%1.2f", mAtkModifier) + " lvlMod:" + String.format("%1.2f", lvlModifier) + " total:" + String.format("%1.2f", rate) + "%");
		}

		return (Rnd.get(100) < rate);
	}

	public static boolean calcCubicSkillSuccess(Cubic attacker, Creature target, L2Skill skill, byte shld, boolean bss) {
		// if target reflect this skill then the effect will fail
		if (calcSkillReflect(target, skill) != SKILL_REFLECT_FAILED) {
			return false;
		}

		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK) // perfect block
		{
			return false;
		}

		final ESkillType type = skill.getEffectType();

		if (target.isRaid() && !calcRaidAffected(type)) {
			return false;
		}

		final double baseChance = skill.getEffectPower();

		if (skill.ignoreResists()) {
			return Rnd.get(100) < baseChance;
		}

		double mAtkModifier = 1;

		// Add Matk/Mdef Bonus
		if (skill.isMagic()) {
			final double mAtk = attacker.getMAtk();
			double val = mAtk;
			if (bss) {
				val = mAtk * 4.0;
			}

			mAtkModifier = (Math.sqrt(val) / target.getMDef(null, null)) * 11.0;
		}

		final double statModifier = calcSkillStatModifier(type, target);
		final double skillModifier = calcSkillVulnerability(attacker.getOwner(), target, skill, type);
		final double lvlModifier = getLevelModifier(attacker.getOwner(), target, skill);
		final double rate = Math.max(1, Math.min((baseChance * statModifier * skillModifier * mAtkModifier * lvlModifier), 99));

		if (Config.DEVELOPER) {
			_log.info("calcCubicSkillSuccess(): Name:" + skill.getName() + " type:" + skill.getSkillType().toString() + " power:" + String.valueOf(baseChance) + " statMod:" + String.format("%1.2f", statModifier) + " skillMod:" + String.format("%1.2f", skillModifier) + " mAtkMod:" + String.format("%1.2f", mAtkModifier) + " lvlMod:" + String.format("%1.2f", lvlModifier) + " total:" + String.format("%1.2f", rate) + "%");
		}

		return (Rnd.get(100) < rate);
	}

	public static boolean calcMagicSuccess(Creature attacker, Creature target, L2Skill skill) {
		int lvlDifference = target.getLevel() - ((skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel()) + skill.getLevelDepend());
		double rate = 100;

		if (lvlDifference > 0) {
			rate = (Math.pow(1.3, lvlDifference)) * 100;
		}

		if (attacker instanceof Player && ((Player) attacker).getExpertiseWeaponPenalty()) {
			rate += 6000;
		}

		if (Config.DEVELOPER) {
			_log.info("calcMagicSuccess(): Name:" + skill.getName() + " lvlDiff:" + lvlDifference + " fail:" + String.format("%1.2f", rate / 100) + "%");
		}

		rate = Math.min(rate, 9900);

		return (Rnd.get(10000) > rate);
	}

	public static double calcManaDam(Creature attacker, Creature target, L2Skill skill, boolean ss, boolean bss) {
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		double mp = target.getMaxMp();

		if (bss) {
			mAtk *= 4;
		} else if (ss) {
			mAtk *= 2;
		}

		double damage = (Math.sqrt(mAtk) * skill.getPower(attacker) * (mp / 97)) / mDef;
		damage *= calcSkillVulnerability(attacker, target, skill, skill.getSkillType());
		return damage;
	}

	public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, Creature caster) {
		if (baseRestorePercent == 0 || baseRestorePercent == 100) {
			return baseRestorePercent;
		}

		double restorePercent = baseRestorePercent * WIT_BONUS[caster.getWIT()];
		if (restorePercent - baseRestorePercent > 20.0) {
			restorePercent += 20.0;
		}

		restorePercent = Math.max(restorePercent, baseRestorePercent);
		restorePercent = Math.min(restorePercent, 90.0);

		return restorePercent;
	}

	public static boolean calcPhysicalSkillEvasion(Creature target, L2Skill skill) {
		if (skill.isMagic() || skill.isAoE()) {
			return false;
		}

		return Rnd.get(100) < target.calcStat(Stats.SkillDodge, 0, null, skill);
	}

	public static boolean calcSkillMastery(Creature actor, L2Skill sk) {
		// Pointless check for Creature other than players, as initial value will stay 0.
		if (!(actor instanceof Player)) {
			return false;
		}

		if (sk.getSkillType() == ESkillType.FISHING) {
			return false;
		}

		double val = actor.getStat().calcStat(Stats.SkillMastery, 0, null, null);

		if (((Player) actor).isMageClass()) {
			val *= INT_BONUS[actor.getINT()];
		} else {
			val *= STR_BONUS[actor.getSTR()];
		}

		return Rnd.get(100) < val;
	}

	public static double calcValakasAttribute(Creature attacker, Creature target, L2Skill skill) {
		double calcPower = 0;
		double calcDefen = 0;

		if (skill != null && skill.getAttributeName().contains("valakas")) {
			calcPower = attacker.calcStat(Stats.Valakas, calcPower, target, skill);
			calcDefen = target.calcStat(Stats.ValakasRes, calcDefen, target, skill);
		} else {
			calcPower = attacker.calcStat(Stats.Valakas, calcPower, target, skill);
			if (calcPower > 0) {
				calcPower = attacker.calcStat(Stats.Valakas, calcPower, target, skill);
				calcDefen = target.calcStat(Stats.ValakasRes, calcDefen, target, skill);
			}
		}
		return calcPower - calcDefen;
	}

	/**
	 * Calculate elemental modifier. There are 2 possible cases :
	 * <ul>
	 * <li>the check emanates from a skill : the result will be a multiplier,
	 * including an amount of attacker element, and the target vuln/prof.</li>
	 * <li>the check emanates from a weapon : the result is an addition of all
	 * elements, lowered/enhanced by the target vuln/prof</li>
	 * </ul>
	 *
	 * @param attacker : The attacker used to retrieve elemental attacks.
	 * @param target : The victim used to retrieve elemental protections.
	 * @param skill : If different of null, it will be considered as a skill
	 * resist check.
	 * @return A multiplier or a sum of damages.
	 */
	public static double calcElemental(Creature attacker, Creature target, L2Skill skill) {
		if (skill != null) {
			final byte element = skill.getElement();
			if (element > 0) {
				return 1 + (((attacker.getAttackElementValue(element) / 10.0) / 100.0) - (1 - target.getDefenseElementValue(element)));
			}

			return 1;
		}

		double elemDamage = 0;
		for (byte i = 1; i < 7; i++) {
			final int attackerBonus = attacker.getAttackElementValue(i);
			elemDamage += Math.max(0, (attackerBonus - (attackerBonus * (target.getDefenseElementValue(i) / 100.0))));
		}
		return elemDamage;
	}

	/**
	 * Calculate skill reflection according to these three possibilities:
	 * <ul>
	 * <li>Reflect failed</li>
	 * <li>Normal reflect (just effects).</li>
	 * <li>Vengeance reflect (100% damage reflected but damage is also dealt to
	 * actor).</li>
	 * </ul>
	 *
	 * @param target : The skill's target.
	 * @param skill : The skill to test.
	 * @return SKILL_REFLECTED_FAILED, SKILL_REFLECT_SUCCEED or
	 * SKILL_REFLECT_VENGEANCE
	 */
	public static byte calcSkillReflect(Creature target, L2Skill skill) {
		// Some special skills (like hero debuffs...) or ignoring resistances skills can't be reflected.
		if (skill.ignoreResists() || !skill.canBeReflected()) {
			return SKILL_REFLECT_FAILED;
		}

		// Only magic and melee skills can be reflected.
		if (!skill.isMagic() && (skill.getCastRange() == -1 || skill.getCastRange() > MELEE_ATTACK_RANGE)) {
			return SKILL_REFLECT_FAILED;
		}

		byte reflect = SKILL_REFLECT_FAILED;

		// Check for non-reflected skilltypes, need additional retail check.
		switch (skill.getSkillType()) {
			case BUFF:
			case REFLECT:
			case HEAL_PERCENT:
			case MANAHEAL_PERCENT:
			case HOT:
			case CPHOT:
			case MPHOT:
			case UNDEAD_DEFENSE:
			case AGGDEBUFF:
			case CONT:
			case ABSORB:
				return SKILL_REFLECT_FAILED;

			case PDAM:
			case BLOW:
			case MDAM:
			case DEATHLINK:
			case CHARGEDAM:
				final double venganceChance = target.getStat().calcStat((skill.isMagic()) ? Stats.VengeanceMDam : Stats.VengeancePDam, 0, target, skill);
				if (venganceChance > Rnd.get(100)) {
					reflect |= SKILL_REFLECT_VENGEANCE;
				}
				break;
		}

		final double reflectChance = target.calcStat((skill.isMagic()) ? Stats.ReflectSpellEffect : Stats.ReflectSkillEffect, 0, null, skill);
		if (Rnd.get(100) < reflectChance) {
			reflect |= SKILL_REFLECT_SUCCEED;
		}

		return reflect;
	}

	/**
	 * @param cha : The character affected.
	 * @param fallHeight : The height the NPC fallen.
	 * @return the damage, based on max HPs and falling height.
	 */
	public static double calcFallDam(Creature cha, int fallHeight) {
		if (!Config.ENABLE_FALLING_DAMAGE || fallHeight < 0) {
			return 0;
		}

		return cha.calcStat(Stats.Fall, fallHeight * cha.getMaxHp() / 1000, null, null);
	}

	/**
	 * @param type : The L2SkillType to test.
	 * @return true if the L2SkillType can affect a raid boss, false otherwise.
	 */
	public static boolean calcRaidAffected(ESkillType type) {
		switch (type) {
			case MANADAM:
			case MDOT:
				return true;

			case CONFUSION:
			case ROOT:
			case STUN:
			case MUTE:
			case FEAR:
			case DEBUFF:
			case PARALYZE:
			case SLEEP:
			case AGGDEBUFF:
			case AGGREDUCE_CHAR:
				if (Rnd.get(1000) == 1) {
					return true;
				}
		}
		return false;
	}

	/**
	 * Calculates karma lost upon death.
	 *
	 * @param playerLevel : The level of the PKer.
	 * @param exp : The amount of xp earned.
	 * @return The amount of karma player has lost.
	 */
	public static int calculateKarmaLost(int playerLevel, long exp) {
		return (int) (exp / karmaMods[playerLevel] / 15);
	}

	/**
	 * Calculates karma gain upon player kill.
	 *
	 * @param pkCount : The current number of PK kills.
	 * @param isSummon : Does the victim is a summon or no (lesser karma gain if
	 * true).
	 * @return karma points that will be added to the player.
	 */
	public static int calculateKarmaGain(int pkCount, boolean isSummon) {
		int result = 14400;
		if (pkCount < 100) {
			result = (int) (((((pkCount - 1) * 0.5) + 1) * 60) * 4);
		} else if (pkCount < 180) {
			result = (int) (((((pkCount + 1) * 0.125) + 37.5) * 60) * 4);
		}

		if (isSummon) {
			result = ((pkCount & 3) + result) >> 2;
		}

		return result;
	}

	/**
	 * Increase damage depends from target distance to caster. Closer distance
	 * get more damage, far distance - less.
	 *
	 * @param caster who cast a skill
	 * @param target creature for calculating distance
	 * @param skill casted skill
	 * @param damage base deled damage
	 * @return modified dealable damage
	 */
	public static final double calcAoEDamage(Creature caster, Creature target, L2Skill skill, double damage) {
		final double distance = MathUtil.calculateDistance(caster, target, true);
		final double percent = distance / (skill.getSkillRadius() / 2);
		if (percent > 1) {
			return target.calcStat(Stats.AoEDamage, damage, target, skill);
		}

		if (caster.isPlayer()) {
			_log.info("Additional AoE damage: +" + (1 + (1 - percent)) + "%");
		}

		return target.calcStat(Stats.AoEDamage, 1 + (1 - percent) * damage, target, skill);
	}

	/**
	 * Calculates modified effect apply-time which depends from current target
	 * <b>defense</b> variable.
	 *
	 * @param caster
	 * @param target
	 * @param skill
	 * @param type
	 * @param baseTime
	 * @param isSkillMastery
	 * @return modified effect time
	 */
	public static final int calcEffectTime(Creature caster, Creature target, L2Skill skill, L2EffectType type, int baseTime, boolean isSkillMastery) {
		double modifiedTime = baseTime;
		if (skill.getId() > 2277 && skill.getId() < 2286) {
			if (target.isServitor() || (target.isPlayer() && target.getPlayer().getActiveSummon() != null)) {
				modifiedTime /= 2;
			}
		}

		if (isSkillMastery) {
			modifiedTime *= 2;
		}

		switch (type) {
			case STUN:
			case STUN_SELF:
				modifiedTime = (int) target.calcStat(Stats.StunDef, baseTime, target, null);
				break;

			case FEAR:
				modifiedTime = (int) target.calcStat(Stats.FearDef, baseTime, target, null);
				break;

			case SILENCE:
				modifiedTime = (int) target.calcStat(Stats.MuteDef, baseTime, target, null);
				break;

			case DEBUFF:
				modifiedTime = (int) target.calcStat(Stats.DebuffDef, baseTime, target, null);
				break;

			case PARALYZE:
				modifiedTime = (int) target.calcStat(Stats.ParalyzeDef, baseTime, target, null);
				break;

			case SLEEP:
				modifiedTime = (int) target.calcStat(Stats.SleepDef, baseTime, target, null);
				break;

			case ROOT:
				modifiedTime = (int) target.calcStat(Stats.RootDef, baseTime, target, null);
				break;

			case PETRIFICATION:
				modifiedTime = (int) target.calcStat(Stats.StoneDef, baseTime, target, null);
				break;

			case BETRAY:
			case CONFUSION:
				modifiedTime = (int) target.calcStat(Stats.DerangementDef, baseTime, target, null);
				break;
		}

		modifiedTime = Math.max(modifiedTime, 1);

		// swordsinger songs with weapon type
		if (caster.isPlayer()) {
			final Player pc = caster.getPlayer();
			if (pc.getClassId().equalsOrChildOf(ClassId.Swordsinger)) {
				switch (skill.getId()) {
					case 364:
					case 264:
					case 306:
					case 269:
					case 270:
					case 265:
					case 363:
					case 349:
					case 308:
					case 305:
					case 304:
					case 267:
					case 266:
					case 268: {
						final WeaponType weaponType = caster.getAttackType();
						switch (weaponType) {
							case BIGSWORD:
								modifiedTime = baseTime * 0.5; // -50% time with 2h sword
								break;

							case DUAL:
								modifiedTime = baseTime * 2; // +100% time with dual swords
								break;
						}
						break;
					}

					case 437: // song of silence increase their time with duals by 100%
						if (caster.getAttackType() == WeaponType.DUAL) {
							modifiedTime = baseTime * 2;
						}
						break;
				}
			}
			
			if(ProfessionalAnger.validate(pc, skill)) {
				modifiedTime *= (int) SkillTable.FrequentTalent.PROFESSIONAL_ANGER.getHandler().invoke();
			}
		}
		
		return (int) modifiedTime;
	}

	public static final boolean calcParry(Creature attacker, Creature defender, L2Skill skill) {
		if (!defender.isInFrontOf(attacker)) {
			return false;
		}

		Weapon weapon;
		if (defender.isNpc()) {
			weapon = (Weapon) ItemTable.getInstance().getTemplate(defender.getNpc().getRightHandItem());
		} else {
			weapon = defender.getActiveWeaponItem();
		}

		if (weapon == null) {
			return false;
		}

		double parry = 0;
		if (skill != null) {
			// only projectile skills can be parryed
			if (skill.isProjectile()) {
				parry = defender.calcStat(skill.isMagic() ? Stats.SpellParry : Stats.SkillParry, 0, defender, skill);
			}
		} else {
			parry = defender.calcStat(Stats.AttackParry, 0, defender, null);
			if (parry > 0) {
				parry *= DEX_BONUS[defender.getDEX()];
			}
		}

		if (parry > 0) {
			switch (weapon.getItemType()) {
				case DUAL:
				case DUALFIST:
					parry *= 1.05;
					break;
			}
		}

		boolean result = parry > Rnd.get(100);
		if (result) {
			defender.getEventBus().notify(new OnSuccessParry(defender, attacker, skill));
			SkillTable.getInstance().getInfo(5301, 1).getEffects(defender, defender); // parry effect activate

			// sound effects of parry
			final PlaySound parrySound = new PlaySound("ItemSound.public_sword_shing_6");
			defender.sendPacket(parrySound);
			attacker.sendPacket(parrySound);
		}
		return result;
	}

	/**
	 * Return the skill flyTime in MS.<br>
	 * <font color="ff0000"><b>Works only for {@code isProjectile()}
	 * skills!</b></font>
	 *
	 * @param caster spell owner
	 * @param target for distance calculating
	 * @return calculated time after which skill will be called
	 */
	public static final int calcSkillFlyTime(Creature caster, WorldObject target) {
		return (int) (MathUtil.calculateDistance(caster, target, true) * 0.333333);
	}

	public static final boolean calcAbsorb(Creature attacker, Creature absorber, L2Skill skill) {
		double chance = 0;
		if (skill != null) {
			if (skill.getAlignment() == ESkillAlignmentType.PHYSIC) {
				chance = absorber.calcStat(Stats.SkillAbsorbRate, 0, null, null);
			} else if (skill.getAlignment() == ESkillAlignmentType.MAGIC) {
				chance = absorber.calcStat(Stats.SpellAbsorb, 0, null, null);
			}
		} else {
			chance = absorber.calcStat(Stats.DamageAbsorbRate, 0, null, null);
		}

		if (chance <= 0) {
			return false;
		}

		return chance > Rnd.get(100);
	}
}
