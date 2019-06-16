# Copy code from earlier version.

from utils import parse_args
import importlib
import create_data

@gin_configurable
class Trainer:
    def __init__(self, data_loader, args):


def main(data_loader, args):
    trainer = Trainer(data_loader, args)

    #FIXME: combine into one line once stuff works
    model_builder = importlib.import_module(args.mode)                          
    model = model_builder.Model(data_loader, args)

    model.compile(optimizer = trainer.optimizer,
                  loss = trainer.optimizer,
                  metrics = trainer.metrics)

    model.fit(data, 
              validation_data,
              labels, 
              batch_size = trainer.batch_size, 
              epochs = trainer.epochs)
              

    return model


  graph = tf.Graph()
  with graph.as_default():
    # FIXME: batching not supported (add None to shape and such)
    adhoc_games = tf.placeholder(dtype=tf.float32, shape=[NUM_ADHOC_GAMES, 
        MAX_GAME_LEN, OBS_ACT_VEC_LEN])
    game_lengths = tf.placeholder(dtype=tf.int32, shape=[NUM_ADHOC_GAMES])
  
    # LSTM part of generator
    LSTM_cell = tf.nn.rnn_cell.LSTMCell(num_units=64)
    last_states = [None]*NUM_ADHOC_GAMES
    
    for game_num in range(NUM_ADHOC_GAMES):
      input_game = adhoc_games[game_num, :, :]
      # FIXME: need to increase first dim on input game
      _, last_states[game_num] = tf.nn.dynamic_rnn(
          cell=LSTM_cell, # possible need a "get_var" to ensure the same LSTM cell is used
          dtype=tf.float32,
          sequence_length=[game_lengths[game_num]],
          inputs=tf.expand_dims(input_game, 0))
      # accessing hidden state of LSTMstatecell 
      last_states[game_num] = last_states[game_num][0] 
    
    # fully connected part of generator
    concat_embed_vec = tf.concat(last_states, axis=1)
    hidden1 = tf.contrib.layers.fully_connected(concat_embed_vec, 512, activation_fn=tf.nn.relu)
    top_head1 = tf.contrib.layers.fully_connected(hidden1, 256, activation_fn=tf.nn.relu)
    bot_head1 = tf.contrib.layers.fully_connected(hidden1, 256, activation_fn=tf.nn.relu)
    
    # [obs vec size + 1 for bias, gen agent hidden layer size]
    # computing the generated parameters of agent
    # NOTE: relu activation might cause issues with 0 weights?
    agent_hidden3_params = tf.contrib.layers.fully_connected(top_head1, 
        (hidden2_len+1)*hidden3_len, activation_fn=tf.nn.relu)
    
    agent_logit_params = tf.contrib.layers.fully_connected(bot_head1,
        (hidden3_len+1)*ACT_VEC_LEN, activation_fn=tf.nn.relu)
    
    # NOTE: worried that the generator might be unstable with too high learning rate?
    
    # reshaping the agent's generated parameters including batched dimension
    reshaped = {}
    reshaped['hidden3'] = tf.reshape(
        agent_hidden3_params, 
        shape=(hidden3_len, hidden2_len+1)) 
    reshaped['logit'] = tf.reshape(
        agent_logit_params, 
        shape=(ACT_VEC_LEN, hidden3_len+1))
    
    # composing agent's generated weights and biases (including batched dimension)
    agent_weights = {}
    agent_biases = {}
    for layer in ['hidden3', 'logit']:
      agent_weights[layer] = reshaped[layer][:, :-1]
      agent_biases[layer] = reshaped[layer][:, -1]

    # agent forward pass (including generated last two layers)
    agent_obs_ph = tf.placeholder(tf.float32, shape=[None, OBS_VEC_LEN])
    agent_label_ph = tf.placeholder(tf.int32, shape=[None, ACT_VEC_LEN])
    agent_hidden1 = tf.contrib.layers.fully_connected(agent_obs_ph, hidden1_len, activation_fn=tf.nn.relu)
    agent_hidden2 = tf.contrib.layers.fully_connected(agent_hidden1, hidden2_len, activation_fn=tf.nn.relu)
    agent_hidden3 = gen_agent_fc(agent_hidden2, agent_weights['hidden3'], agent_biases['hidden3'], act=tf.nn.relu)
    agent_logit = gen_agent_fc(agent_hidden3, agent_weights['logit'], agent_biases['logit'], act=tf.identity)
   
    # outputs
    loss = tf.losses.softmax_cross_entropy(logits=agent_logit, onehot_labels=agent_label_ph)
    optimizer = tf.train.AdamOptimizer(learning_rate=LEARNING_RATE).minimize(loss)
    # FIXME: not actual accuracy, should have division by NUM_OBS_AGENT or something
    accuracy = tf.reduce_sum(tf.cast(tf.equal(tf.argmax(agent_label_ph,1), tf.argmax(agent_logit,1)), tf.int32))

  with tf.Session(graph=graph) as sess:
    data_reader = DataReader(data_path=DATA_PATH, batch_size=1)
   
    tf.global_variables_initializer().run()
    tf.local_variables_initializer().run()
    
    for step in range(NUM_STEPS):
      tr_adhoc_games, tr_game_lengths, tr_obs, tr_act = data_reader.next_batch(batch_type='train')
      loss_val, train_acc_val, _ = sess.run([loss, accuracy, optimizer], feed_dict={
          adhoc_games: tr_adhoc_games,
          game_lengths: tr_game_lengths,
          agent_obs_ph: tr_obs,
          agent_label_ph: tr_act,
          })
    
      if step % DISPLAY_EVERY == 0:
        va_adhoc_games, va_game_lengths, va_obs, va_act = data_reader.next_batch(batch_type='validation')
        validation_acc_val = sess.run(accuracy, feed_dict={
            adhoc_games: va_adhoc_games,
            game_lengths: va_game_lengths,
            agent_obs_ph: va_obs,
            agent_label_ph: va_act,
            })
        
        te_adhoc_games, te_game_lengths, te_obs, te_act = data_reader.next_batch(batch_type='test')
        test_acc_val = sess.run(accuracy, feed_dict={
            adhoc_games: te_adhoc_games,
            game_lengths: te_game_lengths,
            agent_obs_ph: te_obs,
            agent_label_ph: te_act,
            })
        
        print('step {:4d}:    loss={:7.3f}    tr_acc={:.3f}    vl_acc = {:.3f}    ts_acc={:.3f}'.format(
              step, loss_val, train_acc_val/NUM_AGENT_OBS, 
              validation_acc_val/NUM_AGENT_OBS,
              test_acc_val/NUM_AGENT_OBS))

    return model

if __name__ == "__main__":
    args = parse_args.parse()
    data_reader = create_data.main(args)
    main(data_reader, args)
