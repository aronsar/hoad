extern crate csv;
use game::*;
// use strategy::*;
use std::io;

//Header of csv
// cur player idx, turn idx, p1 cards, p2 cards, ..., pN cards, discard pile,
// fireworks, # of remaining life tokens, # of remaining info tokens, and size of remaining deck
pub fn encoding_game(game : &GameState, player : u32, choice: &TurnChoice){

    // let mut writer = csv::Writer::from_path("rust_agent.csv");
    let mut writer = csv::Writer::from_writer(io::stdout());
    let borrowedgameview = game.get_view(player);
    let other_players_hands = borrowedgameview.other_hands; // FnvHashMap<Player, &'a Cards>
    let cur_player = borrowedgameview.player;
    let board = borrowedgameview.board;
    let num_player = board.num_players;
    let turn = board.turn;
    let discard_pile = board.discard.cards;
    let frwks = board.fireworks; //pub fireworks: FnvHashMap<Color, Firework>,
    

    // Header
    // writer.write_record(&["cur_player_id","turn","p1_cards","p2_cards","p3_cards","p4_cards","p5_cards"]);
    let mut temp = vec![];
    temp.push(player.to_string());       //cur_player_id
    temp.push(turn.to_string());       //turn number

    for player in 0..num_player {
        // println!("{}",player);
        if player == cur_player {
            temp.push("".to_string());
        }
        else{
            let mut temp_cards = vec![];
            for &card in other_players_hands.get(&player){
                for c in card {
                    temp_cards.push(c.color.to_string()+ &c.value.to_string());
                }
            }
            let joined = temp_cards.join("-");
            temp.push(joined);

    }

    let mut action = vec![];
    let mut c : std::string::String;
    let mut v : std::string::String;
    let mut i : std::string::String;
    match choice {
        TurnChoice::Hint(ref hint) => {
            action.push("hint");
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


    writer.write_record(&temp);
}
