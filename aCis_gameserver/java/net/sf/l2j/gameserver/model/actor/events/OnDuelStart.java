/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.l2j.gameserver.model.actor.events;

import lombok.Data;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 *
 * @author finfan
 */
@Data
public class OnDuelStart {

	private final Player opponent;
}