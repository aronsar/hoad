# Cross-play Scores

The purpose of this experiment is to show that all of the imitator agents have
high self-play scores, but low cross-play scores. This is important in a
meta-learning setting, where it is necessary to have many agents with high
quality but different strategies.

To run this experiment, we'll use the same virtual environment as the imitator 
agents.

```
source ../../imitator_agents/venv3/bin/activate
pip install keras, cffi
python cross_play.py --num_games 100 
        --pattern "../../imitator_agents/saved_models/*.save/best.h5"
``` 

The row/column labels are: holmesbot, iggi, outer, piers, rainbow, simplebot, 
van-den-bergh.
