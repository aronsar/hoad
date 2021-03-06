/**
  * HeuristicPlayer.scala
  * Plays according to bunch of simple hardcoded conventions that dictate how cards are signaled
  * as playable or discardable or protectworthy by hints or other actions.
  *
  * However, conventions mostly do not dictate the actions taken. Instead, those are computed by
  * looping over all actions and running a pseudo-depth-2 search by predicting the opponent's likely
  * action or actions and then running an evaluation function, and choosing the action that leads
  * to the highest expected evaluation.
  */

package fireflower

import RichImplicits._

//Below are a bunch of types for different kinds of beliefs or knowledge. Each comes in a pair,
//with an "*Info" that is shared between all cards involved in that belief or piece of knowledge,
//and a second type that contains the former as a field that is tracked per-card.

//All the arrays in these information-related are read-only and should NOT be modified.

//The shared information between all cards connected in a belief
sealed trait BeliefInfo
//The value we track per-card that stays attached to card as it moves in a hand.
sealed trait Belief {
  override def toString(): String = {
    this match {
      case PlaySequence(seqIdx,finesseCard,info) =>
        "PlaySequence(seqIdx=" + seqIdx + ",finesse=" + finesseCard + ",cids=(" + info.cids.mkString(",") + "))"
      case ProtectedSet(seqIdx,info) =>
        "ProtectedSet(seqIdx=" + seqIdx + ",cids=(" + info.cids.mkString(",") + "))"
      case JunkSet(seqIdx,info) =>
        "JunkSet(seqIdx=" + seqIdx + ",cids=(" + info.cids.mkString(",") + "))"
    }
  }
}

//A hint was received - this info is meant to track the purely logical info learned.
case class HintedInfo(sh: SeenHint, hand: Array[CardId])
case class Hinted(hid: HandId, applied: Boolean, info: HintedInfo)

//We think the following cards are playable
//and should be played in this order. Possibly includes cards in other player's hands.
case class PlaySequenceInfo(cids: Array[CardId]) extends BeliefInfo
case class PlaySequence(seqIdx: Int, finesseCard: Option[Card], info: PlaySequenceInfo) extends Belief

//We think that these cards are protected and should be held onto and not discarded
case class ProtectedSetInfo(cids: Array[CardId]) extends BeliefInfo
case class ProtectedSet(seqIdx: Int, info: ProtectedSetInfo) extends Belief

//We think these cards can be thrown away
case class JunkSetInfo(cids: Array[CardId]) extends BeliefInfo
case class JunkSet(seqIdx: Int, info: JunkSetInfo) extends Belief

//One is created every time someone plays or discards, recording the state of the game when they did so.
case class DPSnapshot(
  pid: PlayerId,
  turnNumber: Int,
  postHands: Array[Hand],
  postHints: Int,
  nextPlayable: Array[Number],
  preDangers: Array[Card],
  nextPidExpectedPlaysNow: List[HandId],
  nextPidMostLikelyDiscard: HandId,
  isFromPlay: Boolean
)

//Basic constructors and other static functions for the player
object HeuristicPlayer extends PlayerGen {

  val ENABLE_FINESSE = true

  //Construct a HeuristicPlayer for the given rule set
  def apply(rules: Rules, myPid: Int): HeuristicPlayer = {
    val numCardsInitial = Array.fill(Card.maxArrayIdx)(0)
    rules.cards().foreach { card =>
      numCardsInitial(card.arrayIdx) += 1
    }
    new HeuristicPlayer(
      myPid = myPid,
      rules = rules,
      possibleHintTypes = rules.possibleHintTypes(),
      maxHints = rules.maxHints,
      distinctCards = rules.cards().distinct.toList,
      numCardsInitial = numCardsInitial,
      colors = rules.colors(),
      seenMap = SeenMap.empty(rules),
      seenMapCK = SeenMap.empty(rules),
      hintedMap = CardPropertyMap(rules),
      beliefMap = CardPropertyMap(rules),
      dpSnapshots = List()
    )
  }

  //PlayerGen interface - Generate a set of players for a game.
  def genPlayers(rules: Rules, seed: Long): Array[Player] = {
    (0 until rules.numPlayers).map { myPid =>
      this(rules,myPid)
    }.toArray
  }
}

case class SavedState(
  val seenMap: SeenMap,
  val seenMapCK: SeenMap,
  val hintedMap: CardPropertyMap[Hinted],
  val beliefMap: CardPropertyMap[Belief],
  val dpSnapshots: List[DPSnapshot]
)

class HeuristicPlayer private (
  //IMMUTABLE-------------------------------------------
  val myPid: Int,
  var rules: Rules,

  //Various utility values we compute once and cache
  val maxHints: Int,
  val possibleHintTypes: Array[GiveHintType],
  val distinctCards: List[Card],   //contains each distinct card type once
  val numCardsInitial: Array[Int], //indexed by card.arrayIdx, counts the quantity of that card in the whole deck
  val colors: Array[Color],        //an array of the colors in this game

  //STATE-----------------------------------------------

  //Tracks what cards are visible by us
  val seenMap: SeenMap,
  //Tracks what cards are visible as common knowledge
  val seenMapCK: SeenMap,

  //Logical information we've received via hints, tracked by card
  val hintedMap: CardPropertyMap[Hinted],
  //Beliefs we have about cards based on conventions.
  //In general, it's important that this map doesn't contain things that can be inferred only based on
  //private information because it's used to predict other players' actions.
  val beliefMap: CardPropertyMap[Belief],
  //Snapshots of past states of the game when people discarded.
  var dpSnapshots: List[DPSnapshot]
) extends Player {

  def saveState(): SavedState = {
    SavedState(
      seenMap = SeenMap(seenMap),
      seenMapCK = SeenMap(seenMapCK),
      hintedMap = CardPropertyMap(hintedMap),
      beliefMap = CardPropertyMap(beliefMap),
      dpSnapshots = dpSnapshots
    )
  }
  def restoreState(saved: SavedState): Unit = {
    saved.seenMap.copyTo(seenMap)
    saved.seenMapCK.copyTo(seenMapCK)
    saved.hintedMap.copyTo(hintedMap)
    saved.beliefMap.copyTo(beliefMap)
    dpSnapshots = saved.dpSnapshots
  }

  //Checks whether the current game state is one where we should be printing debug messages.
  def debugging(game: Game): Boolean = {
    game.debugPath match {
      case None => false
      case Some(_) => true
    }
  }

  //Update the seen maps based on a new incoming game state
  def updateSeenMap(game: Game): Unit = {
    game.seenMap.copyTo(seenMap)
    game.seenMap.copyTo(seenMapCK)
    (0 until rules.numPlayers).foreach { pid =>
      game.hands(pid).foreach { cid => seenMapCK(cid) = Card.NULL }
    }

    //Apply one pass where we treat provable cards as themselves seen so that
    //we can deduce further cards.
    (0 until rules.numPlayers).foreach { pid =>
      game.hands(pid).foreach { cid =>
        val ckCard = uniquePossible(cid,ck=true)
        if(ckCard != Card.NULL) seenMapCK(cid) = ckCard
        val card = uniquePossible(cid,ck=false)
        if(card != Card.NULL) seenMap(cid) = card
      }
    }
  }

  //Add a belief via its shared info, computing the per-card values to store
  def addBelief(info: BeliefInfo): Unit = {
    info match {
      case (info:PlaySequenceInfo) =>
        for(i <- 0 until info.cids.length) {
          //Preserve finesse cards
          val cid = info.cids(i)
          val finesseCard = getFinesseCard(cid)
          beliefMap.add(cid,PlaySequence(seqIdx=i,finesseCard=finesseCard,info=info))
        }
      case (info:ProtectedSetInfo) =>
        for(i <- 0 until info.cids.length)
          beliefMap.add(info.cids(i),ProtectedSet(seqIdx=i,info=info))
      case (info:JunkSetInfo) =>
        for(i <- 0 until info.cids.length)
          beliefMap.add(info.cids(i),JunkSet(seqIdx=i,info=info))
    }
  }

  //Add a finesse belief for a given card
  def addFinesse(targetCid: CardId, baseCid: CardId, finesseCard: Card): Unit = {
    primeBelief(baseCid) match {
      case Some(b: PlaySequence) =>
        val seqIdx = b.seqIdx
        val cids: Array[CardId] = b.info.cids.take(seqIdx) ++ Array(targetCid) ++ b.info.cids.drop(seqIdx)
        val info = PlaySequenceInfo(cids)
        addBelief(info)
        //Replace the top info of the target itself to have a finesse card
        beliefMap.pop(targetCid)
        beliefMap.add(targetCid,PlaySequence(seqIdx=seqIdx,finesseCard=Some(finesseCard),info=info))
      case Some(_) | None =>
        val cids = Array(targetCid)
        val info = PlaySequenceInfo(cids)
        beliefMap.add(targetCid,PlaySequence(seqIdx=0,finesseCard=Some(finesseCard),info=info))
    }
  }

  //Remove all beliefs that have this card as a finesse playable now, exposing any underneath
  def removePlayableFinesseBeliefs(game: Game, cid: CardId): Unit = {
    primeBelief(cid) match {
      case Some(b: PlaySequence) =>
        b.finesseCard match {
          case None => ()
          case Some(card) =>
            if(game.isPlayable(card)) {
              //Remove belief from this card
              beliefMap.pop(cid)
              //Also adjust from the sequence for other cards
              addBelief(PlaySequenceInfo(cids = b.info.cids.filter { c => c != cid }))
              //And repeat until there are no more
              removePlayableFinesseBeliefs(game,cid)
            }
        }
      case Some(_) | None => ()
    }
  }

  //Is every hint we've received consistent with cid being card?
  def allHintsConsistent(cid: CardId, card: Card): Boolean = {
    hintedMap(cid).forall { hinted => rules.isConsistent(hinted.info.sh.hint, hinted.applied, card) }
  }

  //TODO this function is called frequently!
  //Maybe we can memoize it - might be a decent speedup.

  //What cards could [cid] be as a strictly logical possiblity?
  //If ck is false, uses all information known.
  //If ck is true, uses only common knowledge information.
  def possibleCards(cid: CardId, ck: Boolean): List[Card] = {
    var sm = seenMap
    if(ck) sm = seenMapCK

    val seenCard = sm(cid)
    if(seenCard != Card.NULL) List(seenCard)
    else sm.filterDistinctUnseen { card => allHintsConsistent(cid,card) }
  }

  //If there is a unique possible value for this card, return it, else Card.NULL
  def uniquePossible(cid: CardId, ck: Boolean): Card = {
    var sm = seenMap
    if(ck) sm = seenMapCK

    val seenCard = sm(cid)
    if(seenCard != Card.NULL) seenCard
    else sm.filterUniqueDistinctUnseen { card => allHintsConsistent(cid,card) }
  }

  //If there is a unique possible useful value for this card, return it, else Card.NULL
  def uniquePossibleUseful(cid: CardId, game: Game, ck: Boolean): Card = {
    var sm = seenMap
    if(ck) sm = seenMapCK

    val seenCard = sm(cid)
    if(seenCard != Card.NULL) { if(game.isUseful(seenCard)) seenCard else Card.NULL }
    else sm.filterUniqueDistinctUnseen { card => allHintsConsistent(cid,card) && game.isUseful(card)}
  }

  //Check if there is any possible value for this card. ALSO verifies consistency of cards we've seen.
  def hasPossible(cid: CardId): Boolean = {
    val seenCard = seenMap(cid)
    if(seenCard != Card.NULL) allHintsConsistent(cid,seenCard)
    else seenMap.existsUnseen { card => allHintsConsistent(cid,card) }
  }

  //Check if there is a unique possible color for this card conditioned on it being useful. If not, returns NullColor
  def uniquePossibleUsefulColor(cid: CardId, game: Game, ck: Boolean): Color = {
    var sm = seenMap
    if(ck) sm = seenMapCK

    val seenCard = sm(cid)
    if(seenCard != Card.NULL) {
      if(game.isUseful(seenCard)) seenCard.color
      else NullColor
    }
    else {
      val possibles = sm.filterDistinctUnseen { card => allHintsConsistent(cid,card) && game.isUseful(card) }
      possibles match {
        case Nil => NullColor
        case head :: tail =>
          if(tail.forall { card => card.color == head.color })
            head.color
          else
            NullColor
      }
    }
  }

  def provablyPlayable(possibles: List[Card], game: Game): Boolean = {
    possibles.forall { card => game.isPlayable(card) }
  }
  def provablyPlayableIfUseful(possibles: List[Card], game: Game): Boolean = {
    possibles.forall { card => game.isPlayable(card) || game.isJunk(card) }
  }
  def provablyNotPlayable(possibles: List[Card], game: Game): Boolean = {
    possibles.forall { card => !game.isPlayable(card) }
  }
  def provablyUseful(possibles: List[Card], game: Game): Boolean = {
    possibles.forall { card => game.isUseful(card) }
  }
  def provablyNotDangerous(possibles: List[Card], game: Game): Boolean = {
    possibles.forall { card => !game.isDangerous(card) }
  }
  def provablyDangerous(possibles: List[Card], game: Game): Boolean = {
    possibles.forall { card => game.isDangerous(card) }
  }
  def provablyJunk(possibles: List[Card], game: Game): Boolean = {
    possibles.forall { card => game.isJunk(card) }
  }

  //The most recent belief formed about this card, if any.
  def primeBelief(cid: CardId): Option[Belief] = {
    beliefMap(cid) match {
      case Nil => None
      case belief :: _ => Some(belief)
    }
  }

  def isBelievedProtected(cid: CardId): Boolean = {
    primeBelief(cid) match {
      case None => false
      case Some(_: ProtectedSet) => true
      case Some(_: PlaySequence) => false
      case Some(_: JunkSet) => false
    }
  }
  def isBelievedPlayable(cid: CardId, now: Boolean): Boolean = {
    primeBelief(cid) match {
      case None => false
      case Some(_: ProtectedSet) => false
      case Some(b: PlaySequence) => if (now) b.seqIdx == 0 else true
      case Some(_: JunkSet) => false
    }
  }
  def isBelievedUseful(cid: CardId): Boolean = {
    primeBelief(cid) match {
      case None => false
      case Some(_: ProtectedSet) => true
      case Some(_: PlaySequence) => true
      case Some(_: JunkSet) => false
    }
  }
  def isBelievedJunk(cid: CardId): Boolean = {
    primeBelief(cid) match {
      case None => false
      case Some(_: ProtectedSet) => false
      case Some(_: PlaySequence) => false
      case Some(_: JunkSet) => true
    }
  }

  def getFinesseCard(cid: CardId): Option[Card] = {
    primeBelief(cid) match {
      case Some(b:PlaySequence) => b.finesseCard
      case Some(_:ProtectedSet) => None
      case Some(_: JunkSet) => None
      case None => None
    }
  }

  //TODO can we use this? It didn't seem to help when using it in probablyCorrectlyBelievedPlayableSoon
  //If knowledge proves or if beliefs and conventions strongly suggest that this card should be a specific card, return
  //that card, otherwise return Card.NULL.
  def believedCard(cid: CardId, game: Game, ck: Boolean): Card = {
    var sm = seenMap
    if(ck) sm = seenMapCK
    val known = uniquePossible(cid, ck)
    if(known != Card.NULL) known
    else {
      primeBelief(cid) match {
        case None => Card.NULL
        case Some(_: ProtectedSet) => Card.NULL
        case Some(_: JunkSet) => Card.NULL
        case Some(b: PlaySequence) =>
          //Believed playable now
          if(b.seqIdx <= 0)
            sm.filterUniqueDistinctUnseen { card => allHintsConsistent(cid,card) && game.isPlayable(card) }
          //Believed playable later
          else {
            //TODO why is this worse for 3p and 4p?
            if(rules.numPlayers > 2)
              Card.NULL
            else {
              val possibles = sm.filterDistinctUnseen { card => allHintsConsistent(cid,card) && game.isUseful(card) }
              possibles match {
                case Nil => Card.NULL
                case head :: tail =>
                  //If this card must be a certain color...
                  if(tail.forall { card => card.color == head.color }) {
                    val color = head.color
                    //Check all earlier cards to count and see which this could be
                    var simulatedNextPlayable = game.nextPlayable(color.id)
                    def loop(seqIdx:Int): Card = {
                      if(seqIdx >= b.seqIdx)
                        possibles.find { card => card.number == simulatedNextPlayable }.getOrElse(Card.NULL)
                      else {
                        val card = sm.filterUniqueDistinctUnseen { card => allHintsConsistent(cid,card) && card.color == color && card.number == simulatedNextPlayable }
                        if(card == Card.NULL)
                          loop(seqIdx + 1)
                        else {
                          simulatedNextPlayable += 1
                          loop(seqIdx + 1)
                        }
                      }
                    }
                    loop(0)
                  }
                  //If it doesn't have to be a certain color, we have no idea
                  else Card.NULL
              }
            }
          }
      }
    }
  }

  //Given a list of turns where each turn has a list of cards that could be played, approximate
  //the longest sequence of cards that can be played with one pass.
  def numPlayableInOrder(cardsByTurn: List[List[Card]], game: Game): Int = {
    //Loop and see if the card becomes playable as we play in sequence
    val simulatedNextPlayable = game.nextPlayable.clone()
    def loop(cardsByTurn: List[List[Card]], acc: Int): Int = {
      cardsByTurn match {
        case Nil => acc
        case Nil :: cardsByTurn =>
          loop(cardsByTurn,acc)
        case cards :: cardsByTurn =>
          val playables = cards.filter { card => simulatedNextPlayable(card.color.id) == card.number }
          val count = if(playables.length >= 1) 1 else 0
          //Pretend we could play all of them, for the purpose of determining if future cards are playable.
          playables.foreach { card => simulatedNextPlayable(card.color.id) = card.number + 1 }
          loop(cardsByTurn,acc+count)
      }
    }
    loop(cardsByTurn,0)
  }

  //Check if to the best of our knowledge, based on what's actually visible and what we suspect, a given card will
  //be playable once it gets reached in play sequence. NOT COMMON KNOWLEDGE!
  //Also, skips over bad cards in the sequence, assuming we can hint to fix them.
  def probablyCorrectlyBelievedPlayableSoon(cid: CardId, game: Game): Boolean = {
    primeBelief(cid) match {
      case None => false
      case Some(_: ProtectedSet) => false
      case Some(_: JunkSet) => false
      case Some(b: PlaySequence) =>
        if(b.seqIdx <= 0) {
          val card = believedCard(cid, game, ck=false)
          if(card == Card.NULL) {
            //TODO for some reason this helps on 2p and 3p but hurts on 4p. Why?
            if(rules.numPlayers <= 3) provablyPlayable(possibleCards(cid,ck=false),game)
            else false
          }
          else
            game.isPlayable(card)
        }
        else {
          //Loop and see if the card becomes playable as we play in sequence
          val simulatedNextPlayable = game.nextPlayable.clone()
          def loopOk(seqIdx:Int, okIfStopHere:Boolean): Boolean = {
            if(seqIdx > b.seqIdx)
              okIfStopHere
            else {
              val cid = b.info.cids(seqIdx)
              val card = believedCard(cid, game, ck=false)

              //Don't have a guess as to what the card is - can't say that it's playable soon
              if(card == Card.NULL)
                false
              //It's playable next in sequence!
              else if(simulatedNextPlayable(card.color.id) == card.number) {
                simulatedNextPlayable(card.color.id) += 1
                loopOk(seqIdx+1,true) //Loop again and if we stop here, it was playable, so good.
              }
              //It's provably junk if the earlier cards play as expected
              else if(simulatedNextPlayable(card.color.id) > card.number) {
                loopOk(seqIdx+1,false) //Loop again and if we stop here, it wasn't playable, so not good.
              }
              //TODO for some reason this helps on 2p and 3p but hurts on 4p. Why?
              //For <= 3 players, skipping bad cards is okay - count the later ones even if needing fixing
              else {
                if(rules.numPlayers <= 3) loopOk(seqIdx+1,false)
                else false
              }
            }
          }
          loopOk(0,false)
        }
    }
  }

  //Check if there was no moment where this card was in a hand when someone discarded resulting
  //in at least minPostHints hints left.
  //Pid should be the player who holds the card.
  def cardIsNew(pid: PlayerId, cid: CardId, minPostHints: Int) = {
    //Find the most recent instance where someone other than the player who holds the card
    //discarded with many hints left
    val ds = dpSnapshots.find { ds => ds.postHints >= 3 && ds.pid != pid && !ds.isFromPlay}
    ds.forall { ds => !ds.postHands(pid).contains(cid) }
  }

  type DiscardGoodness = Int
  val DISCARD_PROVABLE_JUNK: DiscardGoodness = 6
  val DISCARD_JUNK: DiscardGoodness = 5
  val DISCARD_REGULAR: DiscardGoodness = 4
  val DISCARD_USEFUL: DiscardGoodness = 3
  val DISCARD_PLAYABLE: DiscardGoodness = 2
  val DISCARD_MAYBE_GAMEOVER: DiscardGoodness = 1
  val DISCARD_GAMEOVER: DiscardGoodness = 0

  //Goodness and discard are by common knowledge if and only if ck is true
  def mostLikelyDiscard(pid: PlayerId, game: Game, ck: Boolean): (HandId,DiscardGoodness) = {
    val revHand: Array[CardId] = game.hands(pid).cardArray().reverse
    val numCards = revHand.length
    val possibles: Array[List[Card]] = revHand.map { cid => possibleCards(cid,ck) }

    val (pos,dg): (Int,DiscardGoodness) = {
      val provableJunkDiscard = (0 until numCards).find { pos => provablyJunk(possibles(pos),game) }
      provableJunkDiscard match {
        case Some(pos) => (pos,DISCARD_PROVABLE_JUNK)
        case None =>
          val junkDiscard = (0 until numCards).find { pos => isBelievedJunk(revHand(pos)) && !provablyUseful(possibles(pos),game) }
          junkDiscard match {
            case Some(pos) => (pos,DISCARD_JUNK)
            case None =>
              val regularDiscard = (0 until numCards).find { pos => !isBelievedUseful(revHand(pos)) }
              regularDiscard match {
                case Some(pos) => (pos,DISCARD_REGULAR)
                case None =>
                  val usefulDiscard = (0 until numCards).find { pos =>
                    !isBelievedPlayable(revHand(pos),now=false) &&
                    !isBelievedProtected(revHand(pos)) &&
                    !provablyDangerous(possibles(pos),game) &&
                    !provablyPlayable(possibles(pos),game)
                  }
                  usefulDiscard match {
                    case Some(pos) => (pos,DISCARD_USEFUL)
                    case None =>
                      val maybeGameOverDiscard = (0 until numCards).find { pos =>
                        !provablyDangerous(possibles(pos),game)
                      }
                      maybeGameOverDiscard match {
                        case Some(pos) => (pos,DISCARD_MAYBE_GAMEOVER)
                        case None => (0,DISCARD_GAMEOVER)
                      }
                  }
              }
          }
      }
    }
    (numCards-1-pos,dg)
  }

  //Find all the hand positions we think the given player can play, by convention and belief and knowledge.
  //Now determines if the cards must be playable now rather than eventually.
  //ck determines if we only check using common knowledge or all observed info
  def expectedPlays(pid: PlayerId, game: Game, now: Boolean, ck: Boolean): List[HandId] = {
    val hand = game.hands(pid)
    (0 until hand.numCards).filter { hid =>
      val cid = hand(hid)
      val possibles = possibleCards(cid,ck)
      if(provablyNotPlayable(possibles,game))
        false
      else {
        //Provably playable
        provablyPlayable(possibles,game) ||
        //Or believed...
        {
          primeBelief(cid) match {
            case None => false
            case Some(_: JunkSet) => false
            //Protected and playable conditional on being useful
            case Some(_: ProtectedSet) =>
              provablyPlayableIfUseful(possibles,game)
            //Playable and right now
            case Some(b: PlaySequence) =>
              if(now) b.seqIdx == 0 else true
          }
        }

      }
    }.toList
  }

  //All cards believed playable or known exactly and playable.
  def allBelievedAndUniqueProvablePlays(game: Game): List[CardId] = {
    game.hands.flatMap { hand =>
      (0 until hand.numCards).flatMap { hid =>
        val cid = hand(hid)
        val card = uniquePossible(cid,ck=true)
        if(card != Card.NULL && game.isPlayable(card))
          Some(cid)
        else {
          primeBelief(cid) match {
            case None => None
            case Some(_: ProtectedSet) => None
            case Some(_: JunkSet) => None
            case Some(_: PlaySequence) => Some(cid)
          }
        }
      }
    }.toList
  }

  def firstPossiblyPlayableHid(game: Game, pid: PlayerId, ck: Boolean): Option[HandId] = {
    game.hands(pid).findIdx { cid => !provablyNotPlayable(possibleCards(cid,ck),game) }
  }

  //When a hint affects multiple cards to imply a finesse, which of the hinted cards should the finesse
  //be based off of?
  def finesseBase(game: Game, hintCids: Array[CardId]): Option[CardId] = {
    //If there is an ck-exactly-known card that is 1 step from playable, then that one.
    hintCids.find { cid => uniquePossible(cid,ck=true) != Card.NULL && game.isOneFromPlayable(seenMap(cid)) } match {
      case Some(cid) => Some(cid)
      case None =>
        //Otherwise the first card that could be one step from playable, if it actually is.
        hintCids.find { cid => possibleCards(cid,ck=true).exists { card => game.isOneFromPlayable(card) } } match {
          case Some(cid) => if(game.isOneFromPlayable(seenMap(cid))) Some(cid) else None
          case None => None
        }
    }
  }

  def isGoodForFinesse(game: Game, card: Card, baseCard: Card): Boolean = {
    card.color == baseCard.color && card.number == baseCard.number - 1 && game.isUseful(card)
  }

  //Given that the given Card was hinted from someone else's hand, assuming it's a finesse targeting pid,
  //what's the card their hand they should play according to common knowledge?
  def finesseTarget(game: Game, pid: PlayerId, baseCard: Card): Option[CardId] = {

    //For simplicity, there are no finesse targets if the player has a card that they think could be it but
    //is already part of a play sequence.
    val matchesPlay = {
      game.hands(pid).exists { cid => primeBelief(cid) match {
        case Some(b:PlaySequence) => possibleCards(cid,ck=true).exists { card => (b.seqIdx > 0 || game.isPlayable(card)) && isGoodForFinesse(game,card,baseCard) }
        case Some(_:ProtectedSet) | Some(_:JunkSet) | None => false
      }}
    }
    if(matchesPlay)
      None
    else {
      //Otherwise, first prefer all cards that have been protected. Then any non-junk cards.
      val protectedMatch = {
        game.hands(pid).find { cid => primeBelief(cid) match {
          case Some(_:ProtectedSet) => possibleCards(cid,ck=true).exists { card => isGoodForFinesse(game,card,baseCard) }
          case Some(_:PlaySequence) | Some(_:JunkSet) | None => false
        }}
      }
      protectedMatch match {
        case Some(cid) => Some(cid)
        case None =>
          game.hands(pid).find { cid => primeBelief(cid) match {
            case None => possibleCards(cid,ck=true).exists { card => isGoodForFinesse(game,card,baseCard) }
            case Some(_) => false
          }}
      }
    }
  }

  //Handle a discard that we've seen, updating info and beliefmaps.
  //Assumes seenMap is already updated, but nothing else.
  def handleSeenDiscard(sd: SeenDiscard, preGame: Game, postGame: Game): Unit = {
    val cid = sd.cid
    val discardPid = preGame.curPlayer
    val preExpectedPlaysNow: List[HandId] = expectedPlays(discardPid,preGame,now=true,ck=true)
    val prePossibles = possibleCards(cid,ck=true)
    val preDangers = seenMapCK.filterDistinctUnseen { card => preGame.isDangerous(card) }.toArray
    val preMLD = mostLikelyDiscard(preGame.curPlayer,preGame,ck=true)._1

    updateSeenMap(postGame)

    val card = seenMap(cid)

    //TODO if there are sufficiently many hints left and a player discards, they must not believe they have playable cards,
    //so update those beliefs.

    //If a card discarded is part of a play sequence and had it been played it would have proven subsequent
    //cards in the sequence to be unplayable, go ahead and mark them as protected or junk as appropriate.
    //This is possible in the rare cases where a play gets discarded. In the case where
    //it actually does get played, we don't need a special case because simplifyBeliefs will do this updating
    //of the remaining cards in sequence.
    primeBelief(cid) match {
      case None => ()
      case Some(_: ProtectedSet) => ()
      case Some(_: JunkSet) => ()
      case Some(b: PlaySequence) =>
        val remainingCids = b.info.cids.zipWithIndex.flatMap { case (laterCid,i) =>
          if(i <= b.seqIdx) Some(laterCid)
          else {
            val possiblesWithoutThis = possibleCards(laterCid,ck=true).filter { laterCard => laterCard != card }
            if(provablyJunk(possiblesWithoutThis,postGame)) {
              addBelief(JunkSetInfo(cids = Array(laterCid)))
              None
            }
            else if(provablyNotPlayable(possiblesWithoutThis,postGame)) {
              addBelief(ProtectedSetInfo(cids = Array(laterCid)))
              None
            }
            else
              Some(laterCid)
          }
        }
        if(remainingCids.length != b.info.cids.length) {
          addBelief(PlaySequenceInfo(cids = remainingCids))
        }
    }

    //Discard convention based on if the discard if the discard was of an expected play
    if(card != Card.NULL && preExpectedPlaysNow.contains(sd.hid)) {
      //Disallow discard convention from applying to throwing away finessed cards
      val wasFromFinesse = getFinesseCard(cid).nonEmpty

      //TODO maybe also check that it was NOT the most likely discard?
      //TODO this massively hurts playing strength on 4 player. Why?
      if(rules.numPlayers <= 3 && !wasFromFinesse) {
        //It's a hint about a playable duplicate of what that player believed that card could be.
        val prePossiblesPlayable = prePossibles.filter { card => postGame.isPlayable(card) }
        if(prePossiblesPlayable.nonEmpty) {

          //Find all players that have a copy of that card other than the discarder
          val hasCardAndNotDiscarder = (0 until rules.numPlayers).map { pid =>
            if(pid == discardPid) false
            else postGame.hands(pid).exists { cid => prePossiblesPlayable.contains(seenMap(cid)) }
          }.toArray

          val hasCardCount = hasCardAndNotDiscarder.count(x => x == true)

          //Which players does the hint signal
          val targetedPids: List[PlayerId] = {
            //If nobody has that card and it wasn't us giving the hint, it's us.
            if(hasCardCount == 0 && myPid != discardPid) List(myPid)
            //If nobody has that card and it was us giving that hint, it signals everyone else.
            else if(hasCardCount == 0 && myPid == discardPid) {
              (0 until rules.numPlayers).filter { pid => pid != myPid }.toList
            }
            //If exactly one other player than the discarder has the card, it signals them.
            else if(hasCardCount == 1) (0 until rules.numPlayers).filter { pid => hasCardAndNotDiscarder(pid) }.toList
            //Else signals nobody
            else List()
          }

          def markPlayable(targetCid: CardId): Unit = {
            //If the targeted card is already playable now, then do nothing
            val alreadyBelievedPlayableNow = {
              primeBelief(targetCid) match {
                case Some(PlaySequence(0,_,_)) => true
                case _ => false
              }
            }
            if(!alreadyBelievedPlayableNow) {
              primeBelief(cid) match {
                //If old card was part of a sequence, then the new card is part of that sequence.
                case Some(PlaySequence(seqIdx,_finesseCard,info)) =>
                  val cids = info.cids.clone
                  cids(seqIdx) = targetCid
                  addBelief(PlaySequenceInfo(cids))
                //Otherwise the new card is simply thought playable
                case _ =>
                  addBelief(PlaySequenceInfo(cids = Array(cid)))
              }
            }
          }

          targetedPids.foreach { pid =>
            //If there is a positively hinted card that by CK could be the card, mark the first such card as playable if it
            //is not already marked. Else mark the first card that could by CK be it if not already marked.
            val hand = postGame.hands(pid)

            hand.find { cid =>
              hintedMap(cid).exists { hinted => hinted.applied } &&
              possibleCards(cid,ck=true).exists { c => prePossiblesPlayable.contains(c) }
            } match {
              case Some(cid) => markPlayable(cid)
              case None =>
                hand.find { cid =>
                  possibleCards(cid,ck=true).exists { c => prePossiblesPlayable.contains(c) }
                } match {
                  case Some(cid) => markPlayable(cid)
                  case None => ()
                }
            }
          }
        }
      }
    }
    //Changing mind abound junk sets
    else if(card != Card.NULL && !postGame.isJunk(card)) {
      primeBelief(cid) match {
        case None => ()
        case Some(_: ProtectedSet) | Some(_: PlaySequence) => ()
        case Some(b: JunkSet) =>
          //If the card was not actually junk, then immediately change everything in the believed junk set
          //to protected so we don't keep discarding them.
          addBelief(ProtectedSetInfo(cids = b.info.cids))
      }
    }

    val nextPidMostLikelyDiscard = mostLikelyDiscard(postGame.curPlayer,postGame,ck=true)._1

    //If discarding MLD when there was an expected play and there are no hints,
    //then protect the next player's MLD
    if(postGame.numHints <= 1 && preExpectedPlaysNow.nonEmpty && preMLD == sd.hid) {
      val nextMLDcid = postGame.hands(postGame.curPlayer)(nextPidMostLikelyDiscard)
      if(!isBelievedProtected(nextMLDcid))
      addBelief(ProtectedSetInfo(cids = Array(nextMLDcid)))
    }

    //Make a new snapshot
    val newDPSnapshot = {
      DPSnapshot(
        pid = discardPid,
        turnNumber = preGame.turnNumber,
        postHands = postGame.hands.map { hand => Hand(hand) },
        postHints = postGame.numHints,
        nextPlayable = postGame.nextPlayable.clone(),
        preDangers = preDangers,
        //TODO these make things quite a bit slower, any way to speed up?
        nextPidExpectedPlaysNow = expectedPlays(postGame.curPlayer,postGame,now=true,ck=true),
        nextPidMostLikelyDiscard,
        isFromPlay = false
      )
    }
    dpSnapshots = newDPSnapshot :: dpSnapshots
  }

  //Handle a play that we've seen, updating info and beliefmaps.
  //Assumes seenMap is already updated, but nothing else.
  def handleSeenPlay(sp: SeenPlay, preGame: Game, postGame: Game): Unit = {
    val playPid = preGame.curPlayer
    val preDangers = seenMapCK.filterDistinctUnseen { card => preGame.isDangerous(card) }.toArray

    val cid = sp.cid
    updateSeenMap(postGame)

    //Successful play
    primeBelief(cid) match {
      //No prior belief
      case None => ()
      //Card was protected - presumably the player somehow inferred it as playable
      case Some(_: ProtectedSet) => ()
      //Card was believed junk - presumably the player somehow inferred it as playable
      case Some(_: JunkSet) => ()
      //Card was a believed play. Remove the card from the play sequence it was a part of
      case Some(b: PlaySequence) =>
        addBelief(PlaySequenceInfo(cids = b.info.cids.filter { c => c != cid }))
    }

    //Make a new snapshot
    val nextPidMostLikelyDiscard = mostLikelyDiscard(postGame.curPlayer,postGame,ck=true)._1
    val newDPSnapshot = {
      DPSnapshot(
        pid = playPid,
        turnNumber = preGame.turnNumber,
        postHands = postGame.hands.map { hand => Hand(hand) },
        postHints = postGame.numHints,
        nextPlayable = postGame.nextPlayable.clone(),
        preDangers = preDangers,
        //TODO these make things quite a bit slower, any way to speed up?
        nextPidExpectedPlaysNow = expectedPlays(postGame.curPlayer,postGame,now=true,ck=true),
        nextPidMostLikelyDiscard,
        isFromPlay = true
      )
    }
    dpSnapshots = newDPSnapshot :: dpSnapshots

  }

  def handleSeenBomb(sb: SeenBomb, preGame: Game, postGame: Game): Unit = {
    val cid = sb.cid
    val bombPid = preGame.curPlayer
    updateSeenMap(postGame)

    primeBelief(cid) match {
      //No prior belief
      case None => ()
      //Card was protected - presumably the player somehow inferred it as playable but bombed??
      case Some(_: ProtectedSet) => ()
      //Card was believed junk - presumably the player somehow inferred it as playable or chose to play it anyways??
      case Some(_: JunkSet) => ()
      //Card was a believed play, but turned out to bomb.
      case Some(b: PlaySequence) =>
        //Immediately change everything in the sequence to protected if the card was useful
        //If it was junk, assume it was just an unfortunate collision
        val card = seenMap(cid)
        if(card != Card.NULL && postGame.isUseful(card)) {
          addBelief(ProtectedSetInfo(cids = b.info.cids))
        }
        //If the card was only ever hinted playable as the first in its sequence,
        //protect everything older than the oldest in the most recent play sequence.
        val beliefs = beliefMap(cid)
        if(beliefs.forall { belief =>
          belief match {
            case (b2: PlaySequence) => b2.seqIdx == 0
            case (_: ProtectedSet) => true
            case (_: JunkSet) => true
          }
        }) {
          val preHand = preGame.hands(bombPid)
          val lastHid = b.info.cids.foldLeft(0) { case (acc,cid) =>
            preHand.findIdx { c => c == cid } match {
              //A different player had the card in sequence! Ignore it.
              case None => acc
              case Some(hid) => Math.max(acc,hid)
            }
          }
          val protectedCids = ((lastHid+1) until preHand.length).map { hid => preHand(hid) }.toArray
          addBelief(ProtectedSetInfo(cids = protectedCids))
        }
    }
  }


  //Finesses!
  def applyFinesses(pid: PlayerId, postGame: Game, hintCids: Array[CardId]): Unit = {
    if(HeuristicPlayer.ENABLE_FINESSE && pid != postGame.curPlayer && pid != myPid) {
      finesseBase(postGame,hintCids) match {
        case None => ()
        case Some(baseCid) =>
          //Finesses can only apply if the baseCid is also first in its play sequence.
          val firstInSequence = {
            primeBelief(baseCid) match {
              case None => true
              case Some(_: JunkSet) => false
              case Some(b: ProtectedSet) => true
              case Some(b: PlaySequence) => b.seqIdx == 0
            }
          }

          if(firstInSequence) {
            val baseCard = seenMap(baseCid)
            assert(baseCard != Card.NULL && baseCard.number > 0)
            val finesseCard = Card(color=baseCard.color,number=baseCard.number-1)

            //Walk through all the players strictly in between the hinting player and the hinted player
            //and get all the cards that might be targeted
            val targetsInBetween = {
              var cids: List[CardId] = List()
              var pidInBetween = postGame.curPlayer
              while(pidInBetween != pid) {
                finesseTarget(postGame,pidInBetween,baseCard) match {
                  case None => ()
                  case Some(cid) => cids = cids :+ cid
                }
                pidInBetween = (pidInBetween + 1) % rules.numPlayers
              }
              cids
            }

            //Must have at most 1 target be good - finesse does nothing if multiple good targets.
            val numGood = targetsInBetween.count { cid => seenMap(cid) != Card.NULL && isGoodForFinesse(postGame,seenMap(cid),baseCard) }
            if(numGood <= 1) {
              val goodTarget = targetsInBetween.find { cid => seenMap(cid) != Card.NULL && isGoodForFinesse(postGame,seenMap(cid),baseCard) }
              goodTarget match {
                case Some(targetCid) =>
                  addFinesse(targetCid,baseCid,finesseCard)
                case None =>
                  //If there's a card in there we can't see (i.e. it targets us) then do that one.
                  val unknownTarget = targetsInBetween.find { cid => seenMap(cid) == Card.NULL }
                  unknownTarget match {
                    case Some(targetCid) =>
                      addFinesse(targetCid,baseCid,finesseCard)
                    case None =>
                      //No real targets at all - then everyone will assume it hits everyone, so reflect that in beliefs.
                      targetsInBetween.foreach { targetCid => addFinesse(targetCid,baseCid,finesseCard) }
                  }
              }
            }
          }
      }
    }
  }

  //Handle a hint that we've seen, updating info and beliefmaps.
  //Assumes seenMap is already updated, but nothing else.
  def handleSeenHint(sh: SeenHint, postGame: Game): Unit = {
    updateSeenMap(postGame)

    val pid = sh.pid //Player who was hinted
    val hand = postGame.hands(pid) //Hand of player who was hinted
    val hintCids = (0 until hand.numCards).flatMap { hid =>
      if(sh.appliedTo(hid)) Some(hand(hid))
      else None
    }.toArray

    //Prior to updating the hintedMap of what we and everyone knows about cards, figure out common
    //knowledge about what cards could have been what prior to this action.
    //handPrePossiblesCKByCid: For all cids in a hand, the possibles for those cids
    val handPrePossiblesCKByCid: Array[List[Card]] = Array.fill(rules.deckSize)(List())
    val prePossiblesCKByHand: Array[Array[List[Card]]] = postGame.hands.map { hand =>
      hand.cardArray().map { cid =>
        val possibles = possibleCards(cid,ck=true)
        handPrePossiblesCKByCid(cid) = possibles
        possibles
      }
    }

    //See what cards would have been be possible for the player to play by common knowledge
    val preExpectedPlaysNow: List[HandId] = expectedPlays(pid,postGame,now=true,ck=true)
    val preAllBelievedAndUniqueProvablePlays: List[CardId] = allBelievedAndUniqueProvablePlays(postGame)

    //Now update hintedMap with the logical information of the hint
    val hintedInfo = HintedInfo(sh, hand.cardArray())
    for (hid <- 0 until hand.numCards) {
      val hinted = Hinted(hid,sh.appliedTo(hid),hintedInfo)
      hintedMap.add(hand(hid),hinted)
    }

    //Scan through all cids provided.
    //If some cids are provably of the same colors as other cards believed playable already, then
    //chain those cids on to the appropriate play sequence for those other cards, and filter them out of the array.
    def chainAndFilterFuturePlays(cids: Array[CardId]): Array[CardId] = {
      cids.filter { cid =>
        val color = uniquePossibleUsefulColor(cid, postGame, ck=true)
        var keep = true
        if(color != NullColor) {
          val earlierPlayCid = preAllBelievedAndUniqueProvablePlays.find {
            playCid => playCid != cid && color == uniquePossibleUsefulColor(playCid, postGame, ck=true)
          }
          earlierPlayCid match {
            case None => ()
            case Some(earlierPlayCid) =>
              primeBelief(earlierPlayCid) match {
                case Some(b: PlaySequence) =>
                  val info = b.info
                  val cidsOfSameColor = info.cids.filter { c => c == earlierPlayCid || color == uniquePossibleUsefulColor(c, postGame, ck=true) }
                  addBelief(PlaySequenceInfo(cids = cidsOfSameColor :+ cid))
                  keep = false
                case _ =>
                  addBelief(PlaySequenceInfo(cids = Array(earlierPlayCid,cid)))
                  keep = false
              }
          }
        }
        keep
      }
    }

    //See what card that player would have been likely to discard
    val (preMLD,preMLDGoodness): (HandId, DiscardGoodness) = mostLikelyDiscard(pid,postGame,ck=true)

    //Affects the most likely discard, and the most likely discard could possibly be dangerous
    val suggestsMLDPossibleDanger = {
      sh.appliedTo(preMLD) &&
      !provablyNotDangerous(possibleCards(hand(preMLD),ck=true),postGame)
    }
    //Proves that an expected/believed play is actually dangerous
    val provesPlayNowAsDanger = {
      preExpectedPlaysNow.exists { hid =>
        sh.appliedTo(hid)
        provablyDangerous(possibleCards(hand(hid),ck=true),postGame)
      }
    }

    //MLD was hinted and it had no prior belief, but an earlier discard or play snapshot indicates it's safe
    val mldSuggestedButSafeDueToDiscardOrPlay = {
      suggestsMLDPossibleDanger &&
      primeBelief(hand(preMLD)).isEmpty && {
        //Player immediately before the hinted player failed to take the relevant action
        val beforePid = (pid + rules.numPlayers - 1) % rules.numPlayers
        def dpSnapshotOkay(ds: DPSnapshot): Boolean = {
          //There was no card we were expected to play, and the same card was our MLD at the time as well.
          ds.nextPidExpectedPlaysNow.isEmpty &&
          ds.postHands(pid)(ds.nextPidMostLikelyDiscard) == hand(preMLD) &&
          {
            //There is no possible card for mld that became dangerous in the meantime
            val dangerNow = seenMapCK.filterDistinctUnseen { card => postGame.isDangerous(card) }.toArray
            !(possibleCards(hand(preMLD),ck=true).exists { card =>
              dangerNow.contains(card) && !ds.preDangers.contains(card)
            })
          }
        }

        val ds = dpSnapshots.find { ds => ds.postHints >= 3 && ds.postHints < rules.maxHints && ds.pid == beforePid }
        ds match {
          case None => false
          case Some(ds) => dpSnapshotOkay(ds)
        }
      }
    }

    //Check if it's a hint where the manner of the hint strongly indicates that it's a play hint
    //even if it would otherwise touch the most likely discard or a believed play that was actually danger
    val isPlayEvenIfAffectingMldOrDangerPlay: Boolean = {
      {
        //Hint affects at least one card that was not a play before and that could be playable now.
        hintCids.exists { cid =>
          val possibles = possibleCards(cid,ck=true)
          !provablyNotPlayable(possibles,postGame) //could be playable now
          !preExpectedPlaysNow.exists { hid => cid == hand(hid) } //not possible play before
        }
      } && {
        //Suggests the MLD but a discard snapshot makes it safe, and there's no other reason we should
        //interpret this hint as protection
        (mldSuggestedButSafeDueToDiscardOrPlay && !provesPlayNowAsDanger) || {
          //Number-and-color-specific rules
          sh.hint match {
            case HintNumber(num) =>
              //All cards in hint are either provably junk, possibly playable, or completely known
              hintCids.forall { cid =>
                val possibles = possibleCards(cid,ck=true)
                !provablyNotPlayable(possibles,postGame) ||
                provablyJunk(possibles,postGame) ||
                possibles.length == 1
              } && {
                //TODO tweak these conditions
                //The number of cards possibly playable is > the number of cards of this number that are useful.
                //OR all color piles are >= that number
                //OR the first card is new and the number of cards possibly playable is >= the number useful and not playable
                val numPossiblyPlayable = hintCids.count { cid =>
                  val possibles = possibleCards(cid,ck=true)
                  !provablyNotPlayable(possibles,postGame)
                }
                numPossiblyPlayable > colors.count { color => postGame.nextPlayable(color.id) <= num } ||
                colors.forall { color => postGame.nextPlayable(color.id) >= num } ||
                (cardIsNew(pid,hintCids.head,minPostHints=3) &&
                  numPossiblyPlayable >= colors.count { color => postGame.nextPlayable(color.id) < num })
              }
            case HintColor(color) =>
              //Affects the first card and cards other than the first are already protected.
              //OR newest card is new and there are no dangers yet in that color
              sh.appliedTo(0) && (1 until hand.length).forall { hid => isBelievedProtected(hand(hid)) } ||
              {
                val firstHid = sh.appliedTo.indexWhere(b => b)
                val firstCid = hand(firstHid)
                //Test if "new" - find the first snapshot with at least 3 hints after, and if it fails to contain the card,
                //then the card "never clearly had a chance to be hinted yet".
                cardIsNew(pid,firstCid,minPostHints=3) &&
                //No dangers yet - all possiblities for all cards in hint either have numCardsInitial = 1 or are not dangerous
                //or are completely known or are playable.
                hintCids.forall { cid =>
                  val possibles = possibleCards(cid,ck=true)
                  possibles.length == 1 ||
                  possibles.forall { card => numCardsInitial(card.arrayIdx) <= 1 || !postGame.isDangerous(card) || postGame.isPlayable(card) }
                }
              }
            case _ => false
          }
        }
      }
    }

    //If this hint is an unknown hint, it does nothing
    if(sh.hint == UnknownHint)
    {}
    //If (the hint possibly protects the most likely discard OR proves a believed play now is danger)
    //AND (there are no cards that the player would have played OR the hint touches a card we would have played)
    //AND (we don't trigger one of the exceptions for isPlayEvenIfAffectingMldOrDangerPlay)
    //then it's a protection hint.
    else if(
      (suggestsMLDPossibleDanger || provesPlayNowAsDanger) &&
        (preExpectedPlaysNow.isEmpty || preExpectedPlaysNow.exists { hid => sh.appliedTo(hid) }) &&
        !isPlayEvenIfAffectingMldOrDangerPlay
    ) {
      addBelief(ProtectedSetInfo(cids = hintCids))
    }
    //Otherwise if at least one card hinted could be playable after the hint, then it's a play hint
    else if(hintCids.exists { cid => !provablyNotPlayable(possibleCards(cid,ck=true),postGame) }) {
      //Cards that are provably playable come first in the ordering
      val (hintCidsProvable, hintCidsNotProvable): (Array[CardId],Array[CardId]) =
        hintCids.partition { cid => provablyPlayable(possibleCards(cid,ck=true),postGame) }

      //Split out any cards that should belong to other play sequences
      val hintCidsNotProvable2 = chainAndFilterFuturePlays(hintCidsNotProvable)
      addBelief(PlaySequenceInfo(cids = hintCidsProvable ++ hintCidsNotProvable2))
      //Finesses!
      applyFinesses(pid,postGame,hintCids)
    }
    //Otherwise if all cards in the hint are provably unplayable and not provably junk
    else if(hintCids.forall { cid =>
      val possibles = possibleCards(cid,ck=true)
      provablyNotPlayable(possibles,postGame) && !provablyJunk(possibles,postGame)
    }) {
      //Finesses! Apply them first so that other things can get chained on to them.
      applyFinesses(pid,postGame,hintCids)
      //Split out any cards that should belong to other play sequences
      val leftoverCids = chainAndFilterFuturePlays(hintCids)
      //Anything remaining treat as protected
      addBelief(ProtectedSetInfo(cids = leftoverCids))
    }
    //Otherwise if all cards in the hint are provably junk, then it's a protection hint
    //to all older cards that are not provably junk older than the oldest in the hint
    else if(hintCids.forall { cid => provablyJunk(possibleCards(cid,ck=true),postGame) }) {
      var oldestHintHid = 0
      for(hid <- 0 until sh.appliedTo.length) {
        if(sh.appliedTo(hid))
          oldestHintHid = hid
      }
      val protectedCids = ((oldestHintHid+1) until sh.appliedTo.length).map { hid => postGame.hands(pid)(hid) }
      addBelief(ProtectedSetInfo(cids = protectedCids.toArray))
    }

  }

  //Simplify and prune beliefs based on actual common-knowledge observations that may contradict them.
  def simplifyBeliefs(preGame: Game, postGame: Game) = {
    //Array to avoid visiting each cid more than once
    val visited = Array.fill(rules.deckSize)(false)
    postGame.hands.foreach { hand =>
      hand.foreach { cid =>
        if(!visited(cid)) {
          primeBelief(cid) match {
            case None => ()
            //Filter protected sets down to only cards that could be dangerous
            case Some(b: ProtectedSet) =>
              b.info.cids.foreach { cid => visited(cid) = true }
              val (remainingCids,filteredCids) = b.info.cids.partition { cid => !provablyJunk(possibleCards(cid,ck=true),postGame) }
              val (newCids,playCids) = remainingCids.partition { cid => !provablyPlayableIfUseful(possibleCards(cid,ck=true),postGame) }
              if(filteredCids.length > 0) addBelief(JunkSetInfo(cids = filteredCids))
              if(playCids.length > 0) addBelief(PlaySequenceInfo(cids = playCids))
              if(newCids.length < b.info.cids.length) addBelief(ProtectedSetInfo(cids = newCids))

            //Filter junk sets down to only cards that could be safe
            case Some(b: JunkSet) =>
              b.info.cids.foreach { cid => visited(cid) = true }
              val (newCids,filteredCids) = b.info.cids.partition { cid => !provablyDangerous(possibleCards(cid,ck=true),postGame) }
              if(filteredCids.length > 0) {
                addBelief(JunkSetInfo(cids = newCids))
                addBelief(ProtectedSetInfo(cids = filteredCids))
              }

            //Filter play sequences down to only card ids that could be playable in that sequence given the cards before
            //Also remove cards from the play sequence that were superseeded by another belief
            case Some(b: PlaySequence) =>
              b.info.cids.foreach { cid => visited(cid) = true }
              var count = 0
              var expectedPlaysUpToNow: List[Card] = List()
              def possiblyPlayable(card: Card): Boolean = {
                postGame.isPlayable(card) ||
                expectedPlaysUpToNow.exists { c => c.color == card.color && c.number == card.number-1 }
              }
              def partOfThisSequence(cid: CardId): Boolean = {
                primeBelief(cid) match {
                  case Some(other: PlaySequence) => other.info eq b.info
                  case _ => false
                }
              }
              val remainingCids = b.info.cids.filter { cid => partOfThisSequence(cid) }
              val (newCids,filteredCids) = remainingCids.partition { cid =>
                count += 1
                if(count == b.info.cids.length)
                  possibleCards(cid,ck=true).exists { card => possiblyPlayable(card) }
                else {
                  val possiblePlays = possibleCards(cid,ck=true).filter { card => possiblyPlayable(card) }
                  if(possiblePlays.isEmpty)
                    false
                  else {
                    expectedPlaysUpToNow = possiblePlays ++ expectedPlaysUpToNow
                    true
                  }
                }
              }
              //TODO this is weird. We update the PlaySequence belief and filter out cids that are no longer part of the sequence
              //only at the times where we have protected or junk card to separate out.
              //It makes things worse on all of 2p,3p,4p to:
              // * Always filter out cids that are no longer part of the sequence
              // * Never filter out cids that are no longer part of the sequence.
              //Why??
              if(filteredCids.length > 0) {
                val (protectCids,junkCids) = filteredCids.partition { cid => !provablyJunk(possibleCards(cid,ck=true),postGame) }
                addBelief(PlaySequenceInfo(cids = newCids))
                addBelief(ProtectedSetInfo(cids = protectCids))
                addBelief(JunkSetInfo(cids = junkCids))
              }
          }
        }
      }
    }

    //Also remove any finesses by the player who just moved if their finesse-implied card was playable now.
    preGame.hands(preGame.curPlayer).foreach { cid =>
      removePlayableFinesseBeliefs(preGame,cid)
    }
  }

  //Check if the current state appears to be consistent.
  //Not exhaustive, but should catch most inconsistencies that might occur in practice.
  //(i.e. discarding things assuming them to be X and then finding out later that X must
  //still be in your hand due to more complex inferences that you didn't work out then)
  def checkIsConsistent(postGame: Game): Boolean = {
    postGame.hands.forall { hand =>
      hand.forall { cid => hasPossible(cid) }
    }
  }

  def softPlus(x: Double, width: Double) = {
    if(x/width >= 40.0) //To avoid floating point overflow
      40.0
    else
      Math.log(1.0 + Math.exp(x/width)) * width
  }

  //Maps from expected score space ("raw eval") -> goodness space ("eval")
  //This is a bit of a hack, because otherwise the horizon effect makes the bot highly reluctant to discard
  //due to fears of discarding the exact same card as partner is about to discard. By exping the values, we make
  //the averaging of that scenario have less effect.
  def transformEval(rawEval: Double) = {
    Math.exp(rawEval / 2.5)
  }
  def untransformEval(eval: Double) = {
    Math.log(eval) * 2.5
  }
  def evalToString(eval: Double) = {
    "%.1f (%.3f)".format(eval,untransformEval(eval))
  }

  //If we're not stopping on early losses, drop the raw eval by this many points for each point of score
  //we provably will miss a win by.
  val scoreDropPerLostPoint = 3.0

  def staticEvalGame(game: Game): Double = {
    if(game.isDone()) {
      transformEval(game.numPlayed.toDouble - scoreDropPerLostPoint * (rules.maxScore - game.numPlayed))
    }
    else {
      //PRELIMARIES-----------------------------------------------------------------------------------------
      //Compute some basic bounds and values used in the eval

      //Number of cards successfully played already
      val numPlayed = game.numPlayed.toDouble

      //Maximum number of turns that there could potentially be this game that play a card.
      val turnsWithPossiblePlayLeft = {
        //On the last round
        if(game.finalTurnsLeft >= 0) {
          //Count remaining players who have a turn
          (0 until game.finalTurnsLeft).count { pidOffset =>
            val pid = (game.curPlayer + pidOffset) % rules.numPlayers
            //Whose hand has at least one possibly useful card.
            game.hands(pid).exists { cid => !provablyJunk(possibleCards(cid,ck=false),game) }
          }
        }
        else {
          //Simply the number of possible cards left in the deck, plus 1 for every player with
          //any useful card
          game.deck.length + game.hands.count { hand =>
            hand.exists { cid => !provablyJunk(possibleCards(cid,ck=false),game) }
          }
        }
      }

      //TODO this logic doesn't make a lot of sense (why not also use turnsWithPossiblePlayLeft in the
      //case where we stop early loss)?
      //But attempts to change it make the bot worse.
      //Also, the bot still misplays in the endgame by not understanding that everyone must have a playable
      //if you get to max discards, but adding that understanding also makes things worse...!?

      //Maximum number of possible plays left to make in the game, taking into account turnsWithPossiblePlayLeft
      val maxPlaysLeft = {
        //Count up cards that are still useful taking into account dead piles.
        var usefulCardCount = 0
        colors.foreach { color =>
          var number = game.nextPlayable(color.id)
          while(game.numCardRemaining(Card.arrayIdx(color,number)) > 0 && number <= rules.maxNumber) {
            usefulCardCount += 1
            number += 1
          }
        }
        Math.min(usefulCardCount,turnsWithPossiblePlayLeft)
      }.toDouble

      //The amount by which we will provably miss the max score by.
      val provableLossBy = rules.maxScore - numPlayed - maxPlaysLeft

      //NET HINTS-----------------------------------------------------------------------------------------
      //The most important term in the eval function - having more hints left in the game
      //(including in the future) is better.

      val numHints = game.numHints
      val numDiscardsLeft = rules.maxDiscards - game.numDiscarded
      val numUnknownHintsGiven = game.numUnknownHintsGiven
      val numPotentialHints = {
        numDiscardsLeft +
        numHints +
        //Assume that unknown hints gain some value, even if we don't know what would be hinted
        numUnknownHintsGiven * {
          //They're more valuable if we have a new card that we just drew.
          //TODO this isn't exactly right for 3 and 4 player
          if(cardIsNew(myPid,game.hands(myPid)(1),minPostHints=3))
            0.45
          else if(cardIsNew(myPid,game.hands(myPid)(0),minPostHints=3))
            0.30
          else
            0.10
        } +
        //Also count future hints from playing 5s. But the last one isn't useful, so subtract 1.
        {
          if(rules.extraHintFromPlayingMax)
            Math.max(0, -1 + colors.count { color => game.nextPlayable(color.id) <= rules.maxNumber })
          else
            0
        }
      }

      //TODO fix up all of the coefficients below and tune them
      //Adjustment - penalize for "bad" beliefs that need more hints to fix
      val fixupHintsRequired =
        game.hands.foldLeft(0.0) { case (acc,hand) =>
          hand.foldLeft(acc) { case (acc,cid) =>
            val card = game.seenMap(cid)
            val value = {
              if(card == Card.NULL)
                0.0
              else {
                val possibles = possibleCards(cid,ck=true)
                if(!provablyNotPlayable(possibles,game) &&
                  isBelievedPlayable(cid,now=true) &&
                  !game.isPlayable(card) &&
                  !game.isDangerous(card) //This because danger we often have to hint anyways, so no cost to have to fixup
                )
                  0.30 / 0.85
                else
                  0.0
              }
            }
            acc + value
          }
        }

      //Collects what cards are known and could be played soon, player by player over the next round
      //in the order that they come up.
      var distinctKnownPlayCardsByTurn: List[List[Card]] = List()
      //Adjustment - bonus for "good" knowledge we already know that saves hints
      val (knownPlays,goodKnowledge) = {
        var kp = 0.0
        var gk = 0.0
        val nextRoundLen = {
          if(game.finalTurnsLeft >= 0) game.finalTurnsLeft
          else rules.numPlayers
        }
        (0 until nextRoundLen).foreach { pidOffset =>
          val pid = (game.curPlayer+pidOffset) % rules.numPlayers
          var distinctKnownPlayCardsThisTurn: List[Card] = List()
          val handLen = game.hands(pid).length
          (0 until handLen).foreach { hid =>
            val cid = game.hands(pid)(hid)
            val card = game.seenMap(cid)
            val possibles = possibleCards(cid,ck=true)
            if(probablyCorrectlyBelievedPlayableSoon(cid,game)) {
              val inferredCard = uniquePossibleUseful(cid,game,ck=false)
              //If we can't exactly determine a card, then count it as a good play, but otherwise
              //filter out cases where multiple players each think they have the playable of a card
              //to avoid over-counting them.
              if(inferredCard == Card.NULL || {
                !distinctKnownPlayCardsByTurn.exists(_.contains(inferredCard)) &&
                !distinctKnownPlayCardsThisTurn.contains(inferredCard)
              }) {
                if(inferredCard != Card.NULL)
                  distinctKnownPlayCardsThisTurn = inferredCard :: distinctKnownPlayCardsThisTurn
                gk += 0.45 / 0.85
                kp += 1.00
              }
            }
            else if(isBelievedProtected(cid) && (card != Card.NULL && game.isDangerous(card))) {
              //Protection at end of hand more efficient than other protection which could be deferred
              //TODO is this really bad for 3 players or is it just overfitting to the test games?
              if(hid == handLen-1 && rules.numPlayers != 3) gk += 0.35 / 0.85
              else gk += 0.20 / 0.85
            }
            else if(isBelievedProtected(cid) && (card != Card.NULL && game.isPlayable(card)))
              gk += 0.10 / 0.85

            //Add a bonus for knowing the card exactly
            val numPossibles = possibles.length
            if(numPossibles <= 1)
              gk += 0.02 / 0.85
            else
              gk += 0.01 / numPossibles
          }
          distinctKnownPlayCardsByTurn = distinctKnownPlayCardsThisTurn.reverse :: distinctKnownPlayCardsByTurn
        }
        distinctKnownPlayCardsByTurn = distinctKnownPlayCardsByTurn.reverse
        (kp,gk)
      }

      //TODO not sure why we can't count a known play more.
      //Increasing this makes things worse!
      val knownPlayValue = if(rules.numPlayers <= 2) 0.20 else  0.1625
      val numHintedOrPlayed = numPlayed + knownPlays * knownPlayValue
      val numRemainingToHint = maxPlaysLeft - knownPlays * knownPlayValue
      val netFreeHints = (numPotentialHints - fixupHintsRequired + goodKnowledge) * 0.85  - numRemainingToHint - 3.0

      //How many plays we have or expect to be able to hint in the future.
      val expectedNumPlaysDueToHints = {
        //Dummy value if there's nothing left to hint
        if(numRemainingToHint <= 0)
          numHintedOrPlayed
        else {
          val gapDueToLowHints = softPlus(-netFreeHints,2.5)
          //Add 3 in numerator and denominator to scale to be closer to 1
          var hintScoreFactorRaw = (maxPlaysLeft - gapDueToLowHints + 3.0) / (maxPlaysLeft + 3.0)
          //Avoid it going negative, smoothly
          hintScoreFactorRaw = softPlus(hintScoreFactorRaw,0.1)
          //Avoid it going above 1
          hintScoreFactorRaw = Math.min(hintScoreFactorRaw,1.0)
          //Apply factor for final result
          numHintedOrPlayed + numRemainingToHint * hintScoreFactorRaw
        }
      }

      //Hack - for adjusting the evaluation in the last round given the cards that are probably correctly believed playable.
      //Ideally we would also count known plays as worth more (see above TODO), but that seems to make things worse!
      val singleRoundExpectedNumPlays = {
        numPlayed + numPlayableInOrder(distinctKnownPlayCardsByTurn,game)
      }

      val finalExpectedNumPlaysDueToHints = {
        if(expectedNumPlaysDueToHints < singleRoundExpectedNumPlays)
          expectedNumPlaysDueToHints + 0.65 * (singleRoundExpectedNumPlays - expectedNumPlaysDueToHints)
        else
          expectedNumPlaysDueToHints
      }

      //Re-adjust to be a factor in terms of numPlayed and maxPlaysLeft.
      val hintScoreFactor = {
        if(maxPlaysLeft == 0) 1.0
        else (Math.min(finalExpectedNumPlaysDueToHints, numPlayed + maxPlaysLeft) - numPlayed) / maxPlaysLeft
      }

      //LIMITED TIME/TURNS -----------------------------------------------------------------------------------------
      //Compute eval factors relating to having a limited amount of time or discards in the game.

      //How much of the remaining score are we not getting due to lack of turns
      val turnsLeftFactor = {
        if(maxPlaysLeft == 0) 1.0
        else Math.min(
          maxPlaysLeft,
          //0.8 * turnsWithPossiblePlayLeft because we want a slight excess in amount
          //of turns left to feel comfortable
          0.8 * turnsWithPossiblePlayLeft
        ) / maxPlaysLeft
      }

      //DANGER AND CLOGGING -----------------------------------------------------------------------------------------
      //Compute eval factors relating to having clogged hands or having discarded useful cards

      //TODO consider making this more principled - score based on the distribution of the remaining deck
      //and not merely dangerousness?

      //How much of the remaining score are we not getting due to danger stuff
      val dangerCount = distinctCards.foldLeft(0.0) { case (acc,card) =>
        val gap: Double = (rules.maxNumber - card.number).toDouble
        if(card.number >= game.nextPlayable(card.color.id) &&
          game.isDangerous(card) &&
          seenMap.numUnseenByCard(card.arrayIdx) == 1)
          acc + (gap + 0.1 * gap * gap)
        else if(card.number >= game.nextPlayable(card.color.id) &&
          numCardsInitial(card.arrayIdx) > 2 &&
          game.numCardRemaining(card.arrayIdx) == 2 &&
          seenMap.numUnseenByCard(card.arrayIdx) == 2)
          acc + (gap + 0.1 * gap * gap) * 0.6
        else
          acc
      }
      val dangerFactor = Math.max(0.0, 1.0 - (dangerCount / 200.0))

      //For decreasing the clog value a little for things near playable or if nothing in front is dangerous.
      //Equals distance from playable + number of dangers in front
      def distanceFromPlayable(card: Card): Int = {
        var distance = card.number - game.nextPlayable(card.color.id)
        for(num <- game.nextPlayable(card.color.id) until card.number) {
          if(game.isDangerous(Card(card.color,num)))
            distance += 1
        }
        distance
      }
      def clogFactorOfDistance(distance: Int): Double = {
        if(distance <= 1) 0.80
        else if(distance <= 2) 0.88
        else if(distance <= 3) 0.94
        else if(distance <= 4) 0.97
        else if(distance <= 5) 0.99
        else 1.00
      }

      //TODO clogginess depend on distance from playable in more cases, such as for danger cards
      val handClogFactor = game.hands.foldLeft(1.0) { case (acc,hand) =>
        val numClogs = hand.foldLeft(0.0) { case (acc,cid) =>
          val card = seenMap(cid)
          //We can't see the card - either in our hand or we're a simulation for that player
          //This means it's safe to use ck=false, since we know no more than that player does, so whatever we
          //prove can be proven by them too.
          val clogAmount = {
            if(card == Card.NULL) {
              val possibles = possibleCards(cid,ck=false)
              val base = {
                if(provablyPlayable(possibles,game))
                  0.0
                else if(isBelievedPlayable(cid,now=false))
                  0.0
                else if(provablyJunk(possibles,game))
                  0.0
                else if(provablyDangerous(possibles,game))
                  1.0
                else if(isBelievedUseful(cid) && !isBelievedPlayable(cid,now=false))
                  1.0
                else
                  0.0
              }
              if(base <= 0.0) base
              else {
                val distance = possibles.foldLeft(0) { case (acc,card) => math.max(acc,distanceFromPlayable(card)) }
                base * clogFactorOfDistance(distance)
              }
            }
            //We can actually see the card
            else {
              val base = {
                //TODO playable cards can sometimes clog a hand if they're hard to hint out and/or the
                //player's current belief about them is wrong. Maybe experiment with this.
                if(game.isPlayable(card))
                  0.0
                else if(probablyCorrectlyBelievedPlayableSoon(cid,game))
                  0.0
                else if(game.isDangerous(card))
                  1.0
                //TODO should we count believed-playable junk cards as clogging?
                else if(isBelievedUseful(cid))
                  1.0
                else
                  0.0
              }
              if(base <= 0.0) base
              else {
                val distance = distanceFromPlayable(card)
                base * clogFactorOfDistance(distance)
              }
            }
          }
          acc + clogAmount
        }
        val freeSpace = rules.handSize.toDouble - numClogs
        val knots = Array(0.66, 0.88, 0.96, 0.99, 1.00)
        val value = {
          val idx = math.floor(freeSpace).toInt
          if(idx >= knots.length-1) knots(knots.length-1)
          else knots(idx) + (knots(idx+1)-knots(idx)) * (freeSpace - idx.toDouble)
        }
        acc * value
      }

      //BOMBS -----------------------------------------------------------------------------------------

      val bombsLeft = rules.maxBombs - game.numBombs + 1
      val bombsFactor = {
        if(bombsLeft >= 3) 1.0
        else if(bombsLeft == 2) 0.98
        else if(bombsLeft == 1) 0.93
        else 0.0
      }

      //NEXT TURN -------------------------------------------------------------------------------------
      //Penalize if the player next to move is possibly about to just lose the game.
      val nextTurnLossFactor = {
        val pid = game.curPlayer
        //TODO restricting all these to < 1 hint right now because if we do more, then the bot wrongly evals many actions
        //because likelyActionsSimple isn't great and often allows us to end up in this kind of situation when in reality
        //that next player would prevent this. Restricting to low hints limits to cases where this is less likely to miseval.
        val dueToDiscardFactor = {
          if(pid == myPid || game.numHints >= rules.maxHints || game.numDiscarded >= rules.maxDiscards || game.numHints > 1)
            1.0
          else {
            val (hid,dg) = mostLikelyDiscard(pid, game, ck=true)
            val cid = game.hands(pid)(hid)
            val aboutToLose = {
              !isBelievedProtected(cid) &&
              provablyDangerous(possibleCards(cid,ck=false),game) &&
              dg >= DISCARD_REGULAR &&
              //TODO try ck=false here
              expectedPlays(pid, game, now=true, ck=false).isEmpty
            }
            if(!aboutToLose) 1.00
            else {
              if(game.numHints <= 0) 0.80
              else 0.90
            }
          }
        }
        val dueToBombFactor = {
          if(pid == myPid || game.numHints > 1)
            1.0
          else {
            val plays = expectedPlays(pid, game, now=true, ck=true)
            val aboutToLose =
              plays.exists { hid =>
                val cid = game.hands(pid)(hid)
                val possibles = possibleCards(cid,ck=false)
                provablyNotPlayable(possibles,game) && (bombsLeft <= 1 || provablyDangerous(possibles,game))
              }
            if(!aboutToLose) 1.0
            else {
              val factorLoss = if(game.numHints <= 0) 0.20 else 0.10
              1.0 - factorLoss / plays.length
            }
          }
        }

        dueToDiscardFactor * dueToBombFactor
      }

      val fewHintsFactor = {
        val nextPid = (game.curPlayer + 1) % rules.numPlayers
        //Penalize if the current player has 0 hints and the next player's MLD is scary and not known to be scary.
        val cantProtectDangerFactor = {
          val cantProtectDanger = {
            (game.numHints == 0 && game.numDiscarded < rules.maxDiscards && nextPid != myPid) && {
              val (hid,_) = mostLikelyDiscard(nextPid, game, ck=true)
              val cid = game.hands(nextPid)(hid)
              !isBelievedProtected(cid) &&
              provablyDangerous(possibleCards(cid,ck=false),game) &&
              //Ideally should be now=true, but the problem is that we want to not penalize the case
              //where the next player has no playables but the current player has a playable that makes
              //the next player have a playable!
              expectedPlays(nextPid, game, now=false, ck=true).isEmpty
            }
          }
          if(cantProtectDanger) 0.85
          else 1.0
        }
        //Penalize if the current player has 0 hints and the next player is about to bomb and lose the game
        val cantAvoidBombLossFactor = {
          if(game.numHints > 0 || nextPid == myPid) 1.0
          else {
            val plays = expectedPlays(nextPid, game, now=true, ck=true)
            val cantAvoidBombLoss = {
              plays.exists { hid =>
                val cid = game.hands(nextPid)(hid)
                val possibles = possibleCards(cid,ck=false)
                provablyNotPlayable(possibles,game) && (bombsLeft <= 1 || provablyDangerous(possibles,game))
              }
            }
            if(cantAvoidBombLoss) 1.0 - (0.15 / plays.length)
            else 1.0
          }
        }

        // if(cantProtectDangerFactor < 1.0) cantProtectDangerFactor
        if(cantProtectDangerFactor < 1.0 || cantAvoidBombLossFactor < 1.0) cantProtectDangerFactor * cantAvoidBombLossFactor
        //TODO why does this hurt 3 player?
        else if(game.numHints == 0 && rules.numPlayers != 3) 0.98
        else 1.00
      }

      //PUT IT ALL TOGETHER -----------------------------------------------------------------------------------------

      val totalFactor = {
        dangerFactor *
        turnsLeftFactor *
        hintScoreFactor *
        bombsFactor *
        handClogFactor *
        nextTurnLossFactor *
        fewHintsFactor
      }
      val raw = {
        numPlayed + maxPlaysLeft * totalFactor - scoreDropPerLostPoint * provableLossBy
      }

      val eval = transformEval(raw)

      if(debugging(game)) {
        println("EVAL----------------")
        maybePrintAllBeliefs(game)
        println("NumPlayed: %.0f, MaxPlaysLeft: %.0f, ProvableLossBy %.0f".format(
          numPlayed, maxPlaysLeft, provableLossBy))
        println("Hints: %.2f, Knol: %.2f, Fixup: %.2f, NetHnt: %.2f".format(
          numPotentialHints,goodKnowledge,fixupHintsRequired,netFreeHints))
        println("HintedOrPlayed: %.2f, Remaining: %.2f, SingleRoundPlays: %.2f, Expected: %.2f, HSF: %.3f".format(
          numHintedOrPlayed,numRemainingToHint,singleRoundExpectedNumPlays,finalExpectedNumPlaysDueToHints,hintScoreFactor))
        println("TurnsWPossPlayLeft: %d, TWPPLF: %.3f".format(
          turnsWithPossiblePlayLeft, turnsLeftFactor))
        println("DangerCount: %.3f, DF: %.3f".format(
          dangerCount, dangerFactor))
        println("BombsLeft: %d, BF: %.3f".format(
          bombsLeft, bombsFactor))
        println("HandClogF: %.3f".format(
          handClogFactor))
        println("NextTurnLossF: %.3f".format(
          nextTurnLossFactor))
        println("FewHintsF: %.3f".format(
          fewHintsFactor))
        println("TotalFactor: %.3f".format(
          totalFactor))
        println("Eval: %s".format(
          evalToString(eval)))
      }

      eval
    }
  }

  //Called at the start of the game once
  def doHandleGameStart(game: Game): Unit = {
    updateSeenMap(game)
  }


  //Update player for a given action. Return true if game still appears consistent, false otherwise.
  def doHandleSeenAction(sa: SeenAction, preGame: Game, postGame: Game): Boolean = {
    sa match {
      case (sd: SeenDiscard) =>
        handleSeenDiscard(sd,preGame,postGame)
      case (sp: SeenPlay) =>
        handleSeenPlay(sp,preGame,postGame)
      case (sb: SeenBomb) =>
        handleSeenBomb(sb,preGame,postGame)
      case (sh: SeenHint) =>
        handleSeenHint(sh,postGame)
    }

    val consistent = checkIsConsistent(postGame)
    if(consistent)
      simplifyBeliefs(preGame,postGame)
    consistent
  }

  //Perform the given action assuming the given CardIds are the given Cards, and recursively search and evaluate the result.
  //Assumes other players act "simply", according to evalLikelyActionSimple
  //At the end, restore to the saved state.
  def tryAction(game: Game, ga: GiveAction, assumingCards: List[(CardId,Card)], weight: Double, cDepth: Int, rDepth: Int, saved: SavedState): Double = {
    val gameCopy = Game(game)
    assumingCards.foreach { case (cid,card) => gameCopy.seenMap(cid) = card }

    val sa = gameCopy.seenAction(ga)
    gameCopy.doAction(ga)

    //We need to check consistency in case the act of doing the action makes a higher-order deduction clear that we hadn't deduced
    //before that the position is actually impossible, since our logical inferencing isn't 100% complete.
    //doHandleSeenAction returns whether it finds things to be consistent or not.
    val consistent = doHandleSeenAction(sa, game, gameCopy)

    if(!consistent) {
      restoreState(saved)
      Double.NaN
    }
    else {
      val newCDepth = cDepth+1
      val newRDepth = rDepth-1
      val eval = {
        if(newRDepth <= 0)
          staticEvalGame(gameCopy)
        else {
          if(gameCopy.curPlayer == myPid) {
            val (_,eval) = doGetAction(gameCopy,newCDepth,newRDepth)
            eval
          }
          else
            evalLikelyActionSimple(gameCopy,newCDepth,newRDepth)
        }
      }
      restoreState(saved)
      if(debugging(gameCopy)) {
        println("Tried %-10s Assuming %s Weight %.2f Eval: %s".format(
          game.giveActionToString(ga),
          assumingCards.map({case (_,card) => card.toString(useAnsiColors=true)}).mkString(""),
          weight,
          evalToString(eval)
        ))
      }
      eval
    }
  }

  def average[T](list: List[T])(f: (T,Double) => Double): Double = {
    var sum = 0.0
    var weightSum = 0.0

    list.foreach { elt =>
      val weight = 1.0
      val eval = f(elt,weight)
      if(!eval.isNaN()) {
        sum += eval * weight
        weightSum += weight
      }
    }
    sum / weightSum
  }

  def weightedAverage[T](list: List[(T,Double)])(f: (T,Double) => Double): Double = {
    var sum = 0.0
    var weightSum = 0.0

    list.foreach { case (elt,weight) =>
      val eval = f(elt,weight)
      if(!eval.isNaN()) {
        sum += eval * weight
        weightSum += weight
      }
    }
    sum / weightSum
  }

  //Recursively evaluate averaging over a prediction of what the next player might do.
  def evalLikelyActionSimple(game: Game, cDepth: Int, rDepth: Int): Double = {
    val saved = saveState()
    val nextActions = likelyActionsSimple(game.curPlayer,game,saved)
    weightedAverage(nextActions) { (nextAction,prob) =>
      val eval = tryAction(game,nextAction,List(),prob,cDepth,rDepth,saved)
      if(debugging(game)) {
        maybePrintAllBeliefs(game)
        println("Likely next: %-12s Weight: %.2f Eval: %s".format(
          game.giveActionToString(nextAction),
          prob,
          evalToString(eval)
        ))
      }
      eval
    }
  }

  //TODO make this better

  //Returns a probability distribution on possible actions the next player might do. Modifies the state to
  //reflect what that player sees, restoring to the given state afterwards.
  def likelyActionsSimple(pid: PlayerId, game: Game, saved: SavedState): List[(GiveAction,Double)] = {
    updateSeenMap(game.hiddenFor(pid))
    val actions = {
      val playsNow: List[HandId] = expectedPlays(pid, game, now=true, ck=false)
      //Play if possible, randomly among all of them
      //TODO the next player will prefer actually to play what makes future cards playable.
      if(playsNow.nonEmpty) {
        if(game.numDiscarded == rules.maxDiscards && game.numHints > 0 && playsNow.length <= 1 &&
          game.deck.length > 0 &&
          game.deck.forall { cid => provablyJunk(possibleCards(cid,ck=true),game) })
          List((GiveHint((pid+1) % game.rules.numPlayers, UnknownHint),1.0))
        else
          playsNow.map { hid => (GivePlay(hid),1.0) }
      }
      //Give a hint if at max hints
      else if(game.numHints >= rules.maxHints)
        List((GiveHint((pid+1) % game.rules.numPlayers, UnknownHint),1.0))
      //No hints, must discard
      else if(game.numHints <= 0) {
        val (mld,dg) = mostLikelyDiscard(pid,game,ck=false)
        //But a discard kills us - so play the first possibly playable card
        if(game.numDiscarded >= rules.maxDiscards || dg <= DISCARD_USEFUL) {
          val hid = firstPossiblyPlayableHid(game,pid,ck=true).getOrElse(0)
          List((GivePlay(hid),1.0))
        }
        //Discard doesn't kill us
        else {
          List((GiveDiscard(mld),1.0))
        }
      }
      //Neither max nor no hints
      else {
        //Discard kills us - then give a hint //TODO improve this for the last round
        val (mld,dg) = mostLikelyDiscard(pid,game,ck=false)
        if(game.numDiscarded >= rules.maxDiscards || dg <= DISCARD_USEFUL) {
          List((GiveHint((pid+1) % game.rules.numPlayers, UnknownHint),1.0))
        }
        else {
          //TODO pretty inaccurate, make this smarter. Note though that the evaluation
          //underestimates how good UnknownHint is because it doesn't do anything!
          //TODO why is this only possible at such a low value?
          //Assign a 20% probability to giving a hint
          //TODO assign more probability in 3 and 4 player if you can see the next person needs a hint.
          //TODO make this understand last-round mechanics and when delay hints are needed?
          List(
            (GiveDiscard(mld),0.80),
            (GiveHint((pid+1) % game.rules.numPlayers, UnknownHint),0.20)
          )
        }
      }
    }
    restoreState(saved)
    actions
  }

  //Get an action for this player in the current game state via a short search.
  //rDepth is the remaining number of turns until we evaluate.
  //Returns the action and its evaluation.
  def doGetAction(game: Game, cDepth: Int, rDepth: Int): (GiveAction,Double) = {
    assert(myPid == game.curPlayer)
    val nextPid = (myPid+1) % rules.numPlayers

    //We loop over all "reasonable" actions, computing their values, using these variables
    //to store the best one, which we return at the end.
    var bestAction: GiveAction = GivePlay(0) //always legal
    var bestActionValue: Double = -10000.0
    val saved = saveState()

    def recordAction(ga: GiveAction, value: Double) = {
      if(!value.isNaN() && value > bestActionValue) {
        bestActionValue = value
        bestAction = ga
      }
      if(debugging(game)) {
        println("Action %-10s Eval: %s".format(
          game.giveActionToString(ga),
          evalToString(value)
        ))
      }
    }

    //Try all play actions
    val ckPlaysNow: List[HandId] = expectedPlays(myPid, game, now=true, ck=true)
    val playsNow: List[HandId] = expectedPlays(myPid, game, now=true, ck=false)
    playsNow.foreach { hid =>
      //Right now, we only play cards we think are probably playable, so get all the possibilities
      //and filter down conditioning on the card being playable, and average over the results
      val cid = game.hands(myPid)(hid)
      //If the card was from a finesse, weight that possibility very highly
      val finesseCard = getFinesseCard(cid)

      val possiblesAndWeights = possibleCards(cid,ck=false).flatMap { card =>
        if(!game.isPlayable(card)) None
        else if(finesseCard == Some(card)) Some((card,100.0))
        else Some((card,1.0))
      }
      val ga = GivePlay(hid)

      val value = weightedAverage(possiblesAndWeights) { (card,weight) =>
        tryAction(game, ga, assumingCards=List((cid,card)), weight, cDepth, rDepth, saved)
      }
      recordAction(ga,value)
    }

    //Try playing our first possibly-playable-card
    if(playsNow.isEmpty) {
      firstPossiblyPlayableHid(game,myPid,ck=false) match {
        case None => ()
        case Some(hid) =>
          val cid = game.hands(myPid)(hid)
          val isProtected = isBelievedProtected(cid)
          val possibles = possibleCards(cid,ck=false).map { card =>
            //Weight nonplayable cards ultra-heavily, so that we'll only do this as a last resort.
            //TODO can we decrease the weight?
            if(isProtected && !game.isPlayable(card) && game.isDangerous(card)) (card,200.0)
            else if(!game.isPlayable(card) && game.isDangerous(card)) (card,150.0)
            else if(!game.isPlayable(card)) (card,100.0)
            else (card,1.0)
          }
          val ga = GivePlay(hid)
          val value = weightedAverage(possibles) { (card,weight) =>
             tryAction(game, ga, assumingCards=List((cid,card)), weight, cDepth, rDepth, saved)
          }
          recordAction(ga,value)
      }
    }

    //Try our most likely discard action
    if(game.numHints < rules.maxHints) {
      val (mld,dg) = mostLikelyDiscard(myPid,game,ck=false)
      val cid = game.hands(myPid)(mld)

      //TODO technically this and many other places that use probability distributions or weighted
      //card distributions don't handle multiplicity of cards properly in the prior - weights are not
      //affected by whether there are 1, 2, or 3 of a card left

      //Based on what kind of discard it is, reweight the various cards that it could logically be
      //to reflect that for better discards, it's rather unlikely for us to throw away something bad.
      val possiblesAndWeights = dg match {
        case (DISCARD_PROVABLE_JUNK | DISCARD_JUNK) =>
          possibleCards(cid,ck=false).map { card =>
            if(game.isJunk(card)) (card,1.0)
            else if(!game.isDangerous(card)) (card,0.1)
            else (card,0.01)
            }
        case (DISCARD_REGULAR) =>
          possibleCards(cid,ck=false).map { card =>
            if(game.isJunk(card)) (card,1.0)
            else if(!game.isDangerous(card)) (card,0.7)
            //Opponent is not expecting us to discard if we have a play available
            else if(ckPlaysNow.nonEmpty) (card,0.5)
            else (card,0.02) //TODO this should depend on hand position
          }
        case (DISCARD_USEFUL | DISCARD_PLAYABLE) =>
          possibleCards(cid,ck=false).map { card => (card,1.0) }

        case (DISCARD_MAYBE_GAMEOVER | DISCARD_GAMEOVER) =>
          possibleCards(cid,ck=false).map { card =>
            if(!game.isDangerous(card)) (card,1.0)
            else (card,2.0)
          }
      }

      //Compute the average eval weighted by the weight of each card it could be.
      val ga = GiveDiscard(mld)
      val value = weightedAverage(possiblesAndWeights) { (card,weight) =>
        tryAction(game, ga, assumingCards=List((cid,card)), weight, cDepth, rDepth, saved)
      }
      recordAction(ga,value)

      //Try discarding each of our playables
      playsNow.foreach { hid =>
        if(hid != mld) {
          val cid = game.hands(myPid)(hid)
          val possiblesAndWeights = possibleCards(cid,ck=false).flatMap { card => if(game.isPlayable(card)) Some((card,1.0)) else None }
          val ga = GiveDiscard(hid)
          val value = weightedAverage(possiblesAndWeights) { (card,weight) =>
            tryAction(game, ga, assumingCards=List((cid,card)), weight, cDepth, rDepth, saved)
          }
          recordAction(ga,value)
        }
      }
    }

    //Try all hint actions
    if(game.numHints > 0) {
      (0 until rules.numPlayers-1).foreach { pidOffset =>
        possibleHintTypes.foreach { hint =>
          val ga = GiveHint((nextPid+pidOffset) % rules.numPlayers,hint)
          if(game.isLegal(ga)) {
            val value = tryAction(game, ga, assumingCards=List(), weight=1.0, cDepth, rDepth, saved)
            recordAction(ga,value)
          }
        }
      }
    }

    //And return the best action
    (bestAction,bestActionValue)
  }

  def maybePrintAllBeliefs(game: Game): Unit = {
    if(debugging(game)) {
      (0 until rules.numPlayers).foreach { pid =>
        game.hands(pid).foreach { cid =>
          val card = seenMap(cid)
          println("P%d Card %s (#%d) Belief %s".format(
            pid,
            card.toString(useAnsiColors=true),
            cid,
            primeBelief(cid).toString()
          ))
        }
      }
    }
  }

  //INTERFACE --------------------------------------------------------------------

  override def handleGameStart(game: Game): Unit = {
    doHandleGameStart(game)
  }

  override def handleSeenAction(sa: SeenAction, preGame: Game, postGame: Game): Unit = {
    val consistent = doHandleSeenAction(sa,preGame,postGame)
    assert(consistent)
  }

  override def getAction(game: Game): GiveAction = {
    //Hack
    //If the game would not be done under rules that stop early on provable loss, then play as if that were the case.
    var maybeModifiedGame = game
    val origRules = rules
    if(!maybeModifiedGame.rules.stopEarlyLoss) {
      maybeModifiedGame.rules match {
        case (gameRules: Rules.Standard) =>
          maybeModifiedGame = Game(game)
          maybeModifiedGame.rules = gameRules.copy(stopEarlyLoss = true)
          rules = maybeModifiedGame.rules
          if(maybeModifiedGame.isDone()) {
            maybeModifiedGame = game
            rules = origRules
          }
      }
    }

    maybePrintAllBeliefs(game)
    val (action,_eval) = doGetAction(maybeModifiedGame,cDepth=0,rDepth=2)
    rules = origRules
    action
  }

}
