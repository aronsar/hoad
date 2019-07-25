extern crate csv;
use game::*;
// use strategy::*;
use std::io;

//Header of csv
// [cur player idx,
// turn idx
// p1 cards
// ...
// pN cards
// discard pile
// action (hint(color,value), discard(index), play(index))
// Fireworks
// # of remaining life tokens
// # of remaining info tokens,
// size of remaining deck
// remaining deck of cards
pub fn encoding_game(game : &GameState, player : u32, choice: &TurnChoice){

    // let mut writer = csv::Writer::from_path("rust_agent.csv");
    let mut writer = csv::Writer::from_writer(io::stdout());
    let borrowedgameview = game.get_view(player);
    let players_hands = &game.hands; // FnvHashMap<Player, &'a Cards>
    let cur_player = borrowedgameview.player;
    let board = borrowedgameview.board;
    let num_player = board.num_players;
    let turn = board.turn;
    let discard_pile = &board.discard.cards;
    let frwks = &board.fireworks; //pub fireworks: FnvHashMap<Color, Firework>,
    let liv_rmn = board.lives_remaining;
    let info_rmn = board.hints_remaining;
    let dk = &game.deck;
    let dk_sz = board.deck_size;

    // Header
    let mut temp = vec![];
    temp.push(player.to_string());       //cur_player_id
    temp.push(turn.to_string());       //turn number

    // players cards
    for plyr in 0..num_player{
        let mut temp_cards = vec![];
        let mut cards = players_hands.get(&plyr).unwrap();
        for c in cards{
            temp_cards.push(c.color.to_string()+ &c.value.to_string());
        }
        let joined = temp_cards.join("-");
        temp.push(joined);
    }


    // for player in 0..num_player {
    //     // println!("{}",player);
    //     if player == cur_player {
    //         temp.push("".to_string());
    //     }
    //     else{
    //         let mut temp_cards = vec![];
    //         for &card in other_players_hands.get(&player){
    //             for c in card {
    //                 temp_cards.push(c.color.to_string()+ &c.value.to_string());
    //             }
    //         }
    //         let joined = temp_cards.join("-");
    //         temp.push(joined);
    //     }
    // }

    let mut temp_cards = vec![];
    for c in discard_pile{
        temp_cards.push(c.color.to_string()+ &c.value.to_string());
    }
    let joined = temp_cards.join("-");
    temp.push(joined);

    let mut action = vec![];
    let mut c : std::string::String;
    let mut v : std::string::String;
    let mut i : std::string::String;
    match choice {
        TurnChoice::Hint(ref hint) => {
            action.push("hint");
            let hint_player = hint.player.to_string();
            action.push(&hint_player);

            match hint.hinted {
                Hinted::Color(color) => {
                    c = color.to_string();
                    action.push(&c);
                }
                Hinted::Value(value) => {
                    v = value.to_string();
                    // action.iter().map(|s| s = v ).collect::<Vec<_>>();
                    action.push(&v);
                }
            };
            let joined_action = action.join("-");
            temp.push(joined_action);
        }
        TurnChoice::Discard(index) => {
            i = index.to_string();
            action.push("discard");
            action.push(&i);
            let joined_action = action.join("-");
            temp.push(joined_action);
        }
        TurnChoice::Play(index) => {
            i = index.to_string();
            action.push("play");
            action.push(&i);
            let joined_action = action.join("-");
            temp.push(joined_action);
        }
    }

    temp_cards = vec![];
    for (cl, fr_wk) in frwks {
        temp_cards.push(fr_wk.color.to_string()+ &fr_wk.top.to_string());
    }
    let joined = temp_cards.join("-");
    temp.push(joined);

    temp.push(liv_rmn.to_string());
    temp.push(info_rmn.to_string());
    temp.push(dk_sz.to_string());
    temp_cards = vec![];
    for card in dk {
        temp_cards.push(card.color.to_string()+ &card.value.to_string());
    }
    let joined = temp_cards.join("-");
    temp.push(joined);

    writer.write_record(&temp);
}
