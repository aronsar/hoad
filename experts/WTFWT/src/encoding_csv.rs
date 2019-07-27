extern crate csv;
use game::*;
use strategy::*;
use std::io;
pub fn encoding_game(game : &GameState, player : u32){
    // let mut writer = csv::Writer::from_path("rust_agent.csv");
    let mut writer = csv::Writer::from_writer(io::stdout());
    let borrowedgameview = game.get_view(player);
    let other_players_hands = borrowedgameview.other_hands; // FnvHashMap<Player, &'a Cards>
    let cur_player = borrowedgameview.player;
    let board = borrowedgameview.board;
    let num_player = board.num_players;

    // Header
    // writer.write_record(&["cur_player_id","p1_cards","p2_cards","p3_cards","p4_cards","p5_cards"]);
    let mut temp = vec![];
    temp.push(player.to_string());       //cur_player_id

    for player in 0..num_player {
        // println!("{}",player);
        if player == cur_player {
            temp.push("".to_string());
        }
        else{
            let mut temp_cards = vec![];
            for &card in other_players_hands.get(&player){
                for c in card {
                    temp_cards.push(c.color.to_string());
                    temp_cards.push(c.value.to_string());
                }
            }
            let joined = temp_cards.join("-");
            temp.push(joined);
        }

    }
    writer.write_record(&temp);
}
