### Rules of the Game
The base game considered here consists of the following materials:

- 50 cards; 10 each of blue, yellow, red, white, and green; of each color there are three 1 valued cards, two 2’s, two 3’s, two 4’s, and only one 5 valued card
- 8 hint tokens, also referred to as information tokens; these have two faces to indicate whether they’ve been used already
- 3 lives, also referred to as mulligans or danger tokens; these too have two faces to designate their availability

The goal of the game is for the players to collectively form 5 piles of cards, one pile of each color. Each pile must be built in ascending order, starting with the 1 valued card on the bottom, and ending with the 5 valued card on top. At the start of the game, each player is dealt a hand of 5 cards, and the 8 hint tokens and 3 life tokens are all facing up. During play, a player may look at the cards of their teammates, but never their own. Players take legal actions in turns, going around the table in a clockwise fashion. The game ends when any of the below conditions occur:

- If the third life token is flipped face down, the game ends with a score of 0 (this is the most important factor in why it is difficult to play with unfamiliar agents)
- If the 5 piles are completed before the deck runs out, the game ends with a score of 25
- If the deck runs out before the 5 piles are completed, each player gets one last turn, including the one who drew the last card. The final score is the sum of the cards in the 5 piles.

According to the official ruleset, no communication may occur during the game other than through the use of hint tokens (explained below). However, the rules also recommend that players interpret this rule in a way that suits them best. Although there is some controversy in the research community over what exactly should be considered acceptable communication (Eger 2019), in this work we have restricted the agents to only be able to pass information to their teammates through their choice of legal action on their own turn—the same as proposed in (Bard 2019). The possible actions in a game are:

- Giving a hint: a player can either give a color hint or a value hint. To give a color hint, pick another player, pick a color, and then point out all instances of that card in their hand. To give a value hint, do the same, but for a card value instead of a color. Giving a hint is only a legal action if there are remaining face up hint tokens. No empty hints may be given, i.e. one may not tell a player what is not in their hand.
- Playing a card: a player may choose a card in their hand and play it. To play a card, attempt to place it on one of the five piles. If successful, draw another card from the deck and pass the turn. If the card can’t currently be placed on any of the piles, discard the card, flip a life token face down, and draw another card from the deck. Note: if a 5 is successfully played, flip a hint token face up if possible.
- Discarding a card: a player can choose a card in their hand and place it in the discard pile, flipping a hint token face up, and then drawing a card from the deck (note: players may look through the discard pile at any time). This is only a legal action if there are 7 or fewer hint tokens face up.

### References
Markus Eger and Daniel Gruss. 2019. Wait a Second: Playing Hanabi withoutGiving Hints. (2019). https://doi.org/10.1145/3337722

Nolan Bard, Jakob N. Foerster, Sarath Chandar, Neil Burch, Marc Lanctot, H. Fran-cis Song, Emilio Parisotto, Vincent Dumoulin, Subhodeep Moitra, Edward Hughes,Iain Dunning, Shibl Mourad, Hugo Larochelle, Marc G. Bellemare, and MichaelBowling. 2019. *The Hanabi Challenge: A New Frontier for AI Research*. TechnicalReport. arXiv:1902.00506 http://arxiv.org/abs/1902.00506
