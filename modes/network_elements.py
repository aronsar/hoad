import tensorflow as tf
from keras.layers import Dense

def gen_agent_fc(input_tensor, weights, biases, act=tf.nn.relu):
    """Take an input tensor, weights, and biases, and do fully connected layer.

    Inputs:
    input tensor: [batch_size, hidden prev len]
    weights: [hidden next len, hidden prev len]
    biases: [, hidden next len]

    Outputs:
    activated: [batch_size, hidden next len]
    """

    weights = tf.cast(tf.stack([weights]*NUM_AGENT_OBS), tf.float32)
    biases = tf.cast(tf.stack([biases]*NUM_AGENT_OBS), tf.float32)
    input_tensor = tf.expand_dims(input_tensor, 2)
    matmul = tf.matmul(weights, input_tensor)[:,:,0]
    added = tf.add(matmul, biases)
    activated = act(added)
    return activated

def LSTM_strategy_embedder(adhoc_games, cfg)
    # LSTM part of generator
    LSTM_cell = tf.nn.rnn_cell.LSTMCell(num_units=64)
    last_states = [None]*cfg['num_adhoc_games']

    for game_num in range(cfg['num_adhoc_games']):
        input_game = adhoc_games[game_num, :, :]
        # FIXME: need to increase first dim on input game
        _, last_states[game_num] = tf.nn.dynamic_rnn(
              cell=LSTM_cell, # might need a "get_var" to ensure the same LSTM cell is used              dtype=tf.float32,
              sequence_length=[game_lengths[game_num]],
              inputs=tf.expand_dims(input_game, 0))
        # accessing hidden state of LSTMstatecell
        last_states[game_num] = last_states[game_num][0]

    strat_vec = tf.concat(last_states, axis=1)
    return strat_vec

def generated_agent_action_taker(input_obs, strat_vec, cfg)
    hidden1 = Dense(cfg['hidden1_dim'], activation=tf.nn.relu)(strat_vec)
    top_head1 = Dense(cfg['top_head1_dim'], activation=tf.nn.relu)(hidden1)
    bot_head1 = Dense(cfg['bot_head1_dim'], activation=tf.nn.relu)(hidden1)

    agent_hidden3_params = Dense( 
            (1 + cfg['agent_hidden2_dim']) * cfg['agent_hidden3_dim'], 
            activation=tf.nn.relu)(top_head1)
    agent_logit_params = Dense(
            (1 + cfg['agent_hidden3_dim']) * cfg['act_vec_len'], 
            activation=tf.nn.relu)(bot_head1)

    # reshaping the agent's generated parameters including batched dimension
    reshaped = {}
    reshaped['hidden3'] = tf.reshape(
        agent_hidden3_params,
        shape=(cfg['agent_hidden3_dim'], 1 + cfg['agent_hidden2_dim']))
    reshaped['logit'] = tf.reshape(
        agent_logit_params,
        shape=(cfg['act_vec_len'], 1 + cfg['agent_hidden3_dim']))

    # composing agent's generated weights and biases (including batched dimension)
    agent_weights = {}
    agent_biases = {}
    for layer in ['hidden3', 'logit']:
      agent_weights[layer] = reshaped[layer][:, :-1]
      agent_biases[layer] = reshaped[layer][:, -1]

    # agent forward pass (including generated last two layers)
    agent_hidden1 = Dense(cfg['agent_hidden1_dim'], activation=tf.nn.relu)(input_obs)
    agent_hidden2 = Dense(cfg['agent_hidden2_dim'], 
            activation=tf.nn.relu)(agent_hidden1)
    agent_hidden3 = gen_agent_fc(agent_hidden2, agent_weights['hidden3'], 
            agent_biases['hidden3'], act=tf.nn.relu)
    agent_logit = gen_agent_fc(agent_hidden3, agent_weights['logit'], 
            agent_biases['logit'], act=tf.identity)
    onehot_action = tf.one_hot(tf.argmax(agent_logit), tf.shape(agent_logit))
    return onehot_action

def discriminator(agent_act, input_act, cfg)
    top_hidden1 = Dense(cfg['top_hidden1_dim'], activation=tf.nn.relu)(top_input)
    bot_hidden1 = Dense(cfg['bot_hidden1_dim'], activation=tf.nn.relu)(bot_input)
    concat_hidden1 = tf.concat([top_hidden1, bot_hidden1], axis=1)
    final_hidden = Dense(cfg['final_hidden_dim'], 
            activation=tf.nn.relu)(concat_hidden1)
    logits = Dense(2, activation=None)(final_hidden)
    onehot_same_agent = tf.one_hot(tf.argmax(logits), tf.shape(logits))
    return onehot_same_agent




